package com.minipay.payment.controller;

import com.minipay.common.req.PaymentReq;
import com.minipay.common.resp.CommonResp;
import com.minipay.payment.dto.CreatePaymentReq;
import com.minipay.payment.dto.RefundReq;
import com.minipay.payment.model.Payment;
import com.minipay.payment.model.PaymentOrder;
import com.minipay.payment.model.RefundOrder;
import com.minipay.payment.service.AlipayService;
import com.minipay.payment.service.PaymentService;
import com.minipay.payment.util.QRCodeUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentController.class);

    @Resource
    private PaymentService paymentService;

    @Resource
    private AlipayService alipayService;

    @PostMapping
    public CommonResp<Map<String, Object>> createPayment(@RequestBody PaymentReq req) {
        LOG.info("创建支付请求, orderId: {}, payType: {}", req.getOrderId(), req.getPayType());
        if (req.getOrderId() == null || req.getOrderId().isEmpty()) {
            return new CommonResp<>(400, "订单ID不能为空", null, false);
        }
        if (req.getAmount() == null || req.getAmount() <= 0) {
            return new CommonResp<>(400, "金额必须大于0", null, false);
        }

        // 将前端传入的分为单位的整数金额，换算成元为单位、保留 2 位小数、四舍五入的标准人民币金额
        BigDecimal amount = new BigDecimal(req.getAmount().toString()).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
        Payment payment = paymentService.createPayment(req.getOrderId(), amount);
        String payType = req.getPayType() != null ? req.getPayType() : "alipay";

        // 如果是支付宝支付
        if ("alipay".equals(payType)) {
            // 调用支付宝服务创建支付订单，获取二维码 URL
            // 支付单号、订单标题、支付金额（元字符串），对接支付宝开放接口，支付宝返回一个 Map
            Map<String, String> alipayResult = alipayService.createPayment(
                payment.getPaymentId(), "MiniPay订单支付", amount.toString()
            );
            //code=1：支付宝下单成功，拿到支付宝付款二维码地址 qr_code
            if ("1".equals(alipayResult.get("code"))) {
                // 获取Url
                String qrCodeUrl = alipayResult.get("qr_code");
                // 使用工具生成300×300 像素的二维码图片，并转为 Base64 字符串
                String qrCodeBase64 = QRCodeUtil.generateQRCodeBase64(qrCodeUrl, 300, 300);
                // 创建哈希表，作为返回结果，包含支付单号、订单号、金额、状态、二维码图片和支付链接,用于前端处理展示
                Map<String, Object> result = new HashMap<>();
                result.put("paymentId", payment.getPaymentId());
                result.put("orderId", payment.getOrderId());
                result.put("amount", amount);
                result.put("status", "PENDING");
                result.put("qrCode", qrCodeBase64);
                result.put("payUrl", qrCodeUrl);
                result.put("payType", "alipay");
                return new CommonResp<>(200, "支付订单创建成功", result, true);
            } else {
                return new CommonResp<>(500, "创建支付失败: " + alipayResult.get("msg"), null, false);
            }
        } else {
            Payment result = paymentService.simulatePayment(payment);
            Map<String, Object> response = new HashMap<>();
            response.put("paymentId", result.getPaymentId());
            response.put("orderId", result.getOrderId());
            response.put("amount", amount);
            response.put("status", result.getStatus());
            return new CommonResp<>(200, "支付创建成功", response, true);
        }
    }

    /**
     * 处理支付宝回调通知
     */
    @PostMapping("/alipay/notify")
    public String alipayNotify(HttpServletRequest request) {
        
        LOG.info("收到支付宝回调通知");
        // 所有请求参数全部存入 Map，用于后续验签与业务处理
        Map<String, String> params = new HashMap<>();
        // request.getParameterNames()获取本次请求里所有参数的 key 名称集合
        Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            params.put(name, request.getParameter(name));
        }
        LOG.info("支付宝回调参数: {}", params);
        // 使用支付宝公钥校验回调里的 sign 签名
        if (!alipayService.verifyNotify(params)) {
            LOG.error("支付宝签名验证失败");
            return "failure";
        }
        // 从回调参数中取出本次支付宝交易的状态（字符串），常见值：TRADE_SUCCESS、TRADE_FINISHED、TRADE_CLOSED 等。
        String tradeStatus = params.get("trade_status");
//        TRADE_SUCCESS：用户付款成功，正常业务处理（改订单、扣库存都在这个状态处理）
//        TRADE_FINISHED：交易彻底完结（超过退款时效），不再处理业务逻辑
        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            // 我方系统生成的支付单号，下单时传给支付宝，用来对应自己库中的支付记录
            String outTradeNo = params.get("out_trade_no");
            // 支付宝系统内部生成的全局唯一交易号（支付宝凭证）
            String tradeNo = params.get("trade_no");
            /*调用业务层方法，执行本地业务更新：
            根据我方支付单号，把 payment 支付记录状态改为【已支付】
            同步调用订单服务，修改订单状态为已支付
            记录支付宝交易号入库，方便对账、退款溯源*/
            paymentService.updatePaymentSuccess(outTradeNo, tradeNo);
            LOG.info("支付宝支付成功, 订单号: {}, 交易号: {}", outTradeNo, tradeNo);
        }
        return "success";
    }

    /**
     * 查询支付状态
     */
    @GetMapping("/{orderId}/status")
    public CommonResp<Map<String, Object>> queryPaymentStatus(@PathVariable("orderId") String orderId) {
        //  根据id查询支付记录
        Payment payment = paymentService.getPaymentByOrderId(orderId);
        if (payment == null) {
            return new CommonResp<>(404, "支付记录不存在", null, false);
        }
        // 如果还是待支付，主动向支付宝查询
        if ("PENDING".equals(payment.getStatus())) {
            LOG.info("订单 {} 待支付，主动查询支付宝", orderId);
            paymentService.queryAlipayStatus(payment);
            // 重新查询更新后的状态
            payment = paymentService.getPaymentByOrderId(orderId);
        }
        // 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("status", payment.getStatus());
        result.put("paymentId", payment.getPaymentId());
        result.put("orderId", payment.getOrderId());
        return new CommonResp<>(200, "查询成功", result, true);
    }

    // 通过订单id查询支付单
    @GetMapping("/{orderId}")
    public CommonResp<Payment> getPaymentByOrderId(@PathVariable("orderId") String orderId) {
        Payment payment = paymentService.getPaymentByOrderId(orderId);
        if (payment == null) {
            return new CommonResp<>(404, "该支付订单不存在", null, false);
        }
        return new CommonResp<>(200, "查询成功", payment, true);
    }

    // 创建支付单
    @PostMapping("/orders")
    public CommonResp<PaymentOrder> createPaymentOrder(@RequestBody CreatePaymentReq req) {
        return new CommonResp<>(200, "支付单创建成功", paymentService.createPaymentOrder(req), true);
    }

    // 通过支付单号查询支付单
    @GetMapping("/payment-orders/{paymentNo}")
    public CommonResp<PaymentOrder> getPaymentOrder(@PathVariable String paymentNo) {
        return new CommonResp<>(200, "支付单查询成功", paymentService.getPaymentOrder(paymentNo), true);
    }

    // 查询支付状态
    @GetMapping("/orders/{orderNo}")
    public CommonResp<PaymentOrder> getPaymentOrderByOrderNo(@PathVariable String orderNo) {
        return new CommonResp<>(200, "订单支付状态查询成功", paymentService.getPaymentOrderByOrderNo(orderNo), true);
    }

    // 模拟支付--TODO
    @PostMapping("/callback/mock")
    public CommonResp<Object> mockCallback(@RequestBody CreatePaymentReq req) {
        return new CommonResp<>(200, "模拟支付成功", paymentService.mockCallback(req), true);
    }

    // 关闭支付单
    @PostMapping("/{paymentNo}/close")
    public CommonResp<PaymentOrder> closePaymentOrder(@PathVariable String paymentNo) {
        return new CommonResp<>(200, "支付单关闭成功", paymentService.closePaymentOrder(paymentNo), true);
    }

    // 创建退款单
    @PostMapping("/{paymentNo}/refund")
    public CommonResp<RefundOrder> refund(@PathVariable String paymentNo, @RequestBody RefundReq req) {
        return new CommonResp<>(200, "退款单创建成功", paymentService.refund(paymentNo, req), true);
    }
}
