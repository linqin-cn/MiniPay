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

        BigDecimal amount = new BigDecimal(req.getAmount().toString()).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
        Payment payment = paymentService.createPayment(req.getOrderId(), amount);
        String payType = req.getPayType() != null ? req.getPayType() : "alipay";

        if ("alipay".equals(payType)) {
            Map<String, String> alipayResult = alipayService.createPayment(
                payment.getPaymentId(), "MiniPay订单支付", amount.toString()
            );
            if ("1".equals(alipayResult.get("code"))) {
                String qrCodeUrl = alipayResult.get("qr_code");
                String qrCodeBase64 = QRCodeUtil.generateQRCodeBase64(qrCodeUrl, 300, 300);
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

    @PostMapping("/alipay/notify")
    public String alipayNotify(HttpServletRequest request) {
        
        LOG.info("收到支付宝回调通知");
        Map<String, String> params = new HashMap<>();
        Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            params.put(name, request.getParameter(name));
        }
        LOG.info("支付宝回调参数: {}", params);
        if (!alipayService.verifyNotify(params)) {
            LOG.error("支付宝签名验证失败");
            return "failure";
        }
        String tradeStatus = params.get("trade_status");
        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            String outTradeNo = params.get("out_trade_no");
            String tradeNo = params.get("trade_no");
            paymentService.updatePaymentSuccess(outTradeNo, tradeNo);
            LOG.info("支付宝支付成功, 订单号: {}, 交易号: {}", outTradeNo, tradeNo);
        }
        return "success";
    }

    @GetMapping("/{orderId}/status")
    public CommonResp<Map<String, Object>> queryPaymentStatus(@PathVariable("orderId") String orderId) {
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
        Map<String, Object> result = new HashMap<>();
        result.put("status", payment.getStatus());
        result.put("paymentId", payment.getPaymentId());
        result.put("orderId", payment.getOrderId());
        return new CommonResp<>(200, "查询成功", result, true);
    }

    @GetMapping("/{orderId}")
    public CommonResp<Payment> getPaymentByOrderId(@PathVariable("orderId") String orderId) {
        Payment payment = paymentService.getPaymentByOrderId(orderId);
        if (payment == null) {
            return new CommonResp<>(404, "该支付订单不存在", null, false);
        }
        return new CommonResp<>(200, "查询成功", payment, true);
    }

    @GetMapping("/health")
    public CommonResp<String> health() {
        return new CommonResp<>(200, "success", "payment-service is running", true);
    }

    @PostMapping("/orders")
    public CommonResp<PaymentOrder> createPaymentOrder(@RequestBody CreatePaymentReq req) {
        return new CommonResp<>(200, "TODO", paymentService.createPaymentOrder(req), true);
    }

    @GetMapping("/payment-orders/{paymentNo}")
    public CommonResp<PaymentOrder> getPaymentOrder(@PathVariable String paymentNo) {
        return new CommonResp<>(200, "TODO", paymentService.getPaymentOrder(paymentNo), true);
    }

    @GetMapping("/orders/{orderNo}")
    public CommonResp<PaymentOrder> getPaymentOrderByOrderNo(@PathVariable String orderNo) {
        return new CommonResp<>(200, "TODO", paymentService.getPaymentOrderByOrderNo(orderNo), true);
    }

    @PostMapping("/callback/mock")
    public CommonResp<Object> mockCallback(@RequestBody CreatePaymentReq req) {
        return new CommonResp<>(200, "TODO", paymentService.mockCallback(req), true);
    }

    @PostMapping("/{paymentNo}/close")
    public CommonResp<PaymentOrder> closePaymentOrder(@PathVariable String paymentNo) {
        return new CommonResp<>(200, "TODO", paymentService.closePaymentOrder(paymentNo), true);
    }

    @PostMapping("/{paymentNo}/refund")
    public CommonResp<RefundOrder> refund(@PathVariable String paymentNo, @RequestBody RefundReq req) {
        return new CommonResp<>(200, "TODO", paymentService.refund(paymentNo, req), true);
    }
}
