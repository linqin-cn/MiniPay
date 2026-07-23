package com.minipay.payment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minipay.payment.config.AlipayConfig;
import com.minipay.payment.dto.CreatePaymentReq;
import com.minipay.payment.dto.RefundReq;
import com.minipay.payment.mapper.PaymentMapper;
import com.minipay.payment.model.Payment;
import com.minipay.payment.model.PaymentOrder;
import com.minipay.payment.model.RefundOrder;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentService.class);

    @Resource
    private PaymentMapper paymentMapper;

    @Resource
    private AlipayConfig alipayConfig;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String ORDER_SERVICE_URL = "http://localhost:8081/api/orders";

    public Payment createPayment(String orderId, BigDecimal amount) {
        LOG.info("创建支付订单, orderId: {}, amount: {}", orderId, amount);

        // 如果已有待支付的记录，直接返回（避免重复创建）
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Payment::getOrderId, orderId);
        wrapper.eq(Payment::getStatus, "PENDING");
        wrapper.orderByDesc(Payment::getCreatedAt);
        List<Payment> existing = paymentMapper.selectList(wrapper);
        if (!existing.isEmpty()) {
            LOG.info("复用已有待支付记录, paymentId: {}", existing.get(0).getPaymentId());
            return existing.get(0);
        }

        Payment payment = new Payment();
        payment.setPaymentId(UUID.randomUUID().toString().replace("-", ""));
        payment.setOrderId(orderId);
        payment.setAmount(amount);
        payment.setStatus("PENDING");
        payment.setCreatedAt(LocalDateTime.now());
        paymentMapper.insert(payment);
        return payment;
    }

    public Payment simulatePayment(Payment payment) {
        int result = new java.util.Random().nextInt(100);
        if (result < 80) {
            payment.setStatus("SUCCESS");
            payment.setPaidAt(LocalDateTime.now());
        } else {
            payment.setStatus("FAILED");
            payment.setPaidAt(LocalDateTime.now());
        }
        paymentMapper.updateById(payment);
        updateOrderStatus(payment.getOrderId(), payment.getStatus());
        return payment;
    }

    public void updatePaymentSuccess(String paymentId, String tradeNo) {
        LOG.info("更新支付成功状态, paymentId: {}, tradeNo: {}", paymentId, tradeNo);
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Payment::getPaymentId, paymentId);
        Payment payment = paymentMapper.selectOne(wrapper);
        if (payment != null && "PENDING".equals(payment.getStatus())) {
            payment.setStatus("SUCCESS");
            payment.setTradeNo(tradeNo);
            payment.setPaidAt(LocalDateTime.now());
            paymentMapper.updateById(payment);
            updateOrderStatus(payment.getOrderId(), "PAID");
        }
    }

    public void queryAlipayStatus(Payment payment) {
        try {
            com.alipay.api.AlipayClient alipayClient = new com.alipay.api.DefaultAlipayClient(
                "https://openapi-sandbox.dl.alipaydev.com/gateway.do",
                alipayConfig.getAppId(),
                alipayConfig.getPrivateKey(),
                "json", "UTF-8",
                alipayConfig.getAlipayPublicKey(),
                "RSA2"
            );
            com.alipay.api.request.AlipayTradeQueryRequest request = new com.alipay.api.request.AlipayTradeQueryRequest();
            request.setBizContent("{\"out_trade_no\":\"" + payment.getPaymentId() + "\"}");
            com.alipay.api.response.AlipayTradeQueryResponse response = alipayClient.execute(request);
            LOG.info("主动查询支付宝: code={}, tradeStatus={}", response.getCode(), response.getTradeStatus());
            if (response.isSuccess() && "TRADE_SUCCESS".equals(response.getTradeStatus())) {
                payment.setStatus("SUCCESS");
                payment.setTradeNo(response.getTradeNo());
                payment.setPaidAt(LocalDateTime.now());
                paymentMapper.updateById(payment);
                updateOrderStatus(payment.getOrderId(), "PAID");
                LOG.info("主动查询发现支付已成功, paymentId: {}", payment.getPaymentId());
            }
        } catch (Exception e) {
            LOG.error("主动查询支付宝状态失败", e);
        }
    }

    private void updateOrderStatus(String orderId, String paymentStatus) {
        try {
            String orderStatus = "SUCCESS".equals(paymentStatus) ? "PAID" : paymentStatus;
            String url = ORDER_SERVICE_URL + "/" + orderId + "/status";
            Map<String, String> request = new HashMap<>();
            request.put("status", orderStatus);
            LOG.info("通知订单服务更新状态, orderId: {}, status: {}", orderId, orderStatus);
            restTemplate.put(url, request);
        } catch (Exception e) {
            LOG.error("通知订单服务失败, orderId: {}, error: {}", orderId, e.getMessage());
        }
    }

    public Payment getPaymentByOrderId(String orderId) {
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Payment::getOrderId, orderId);
        wrapper.orderByDesc(Payment::getCreatedAt);
        List<Payment> payments = paymentMapper.selectList(wrapper);
        return payments.isEmpty() ? null : payments.get(0);
    }

    public Payment getPaymentByPaymentId(String paymentId) {
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Payment::getPaymentId, paymentId);
        return paymentMapper.selectOne(wrapper);
    }

    public String health() {
        return "payment-service is running";
    }

    public PaymentOrder createPaymentOrder(CreatePaymentReq req) {
        return null;
    }

    public PaymentOrder getPaymentOrder(String paymentNo) {
        return null;
    }

    public PaymentOrder getPaymentOrderByOrderNo(String orderNo) {
        return null;
    }

    public Object mockCallback(CreatePaymentReq req) {
        return null;
    }

    public PaymentOrder closePaymentOrder(String paymentNo) {
        return null;
    }

    public RefundOrder refund(String paymentNo, RefundReq req) {
        return null;
    }
}
