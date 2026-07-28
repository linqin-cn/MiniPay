package com.minipay.payment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minipay.payment.config.AlipayConfig;
import com.minipay.payment.dto.CreatePaymentReq;
import com.minipay.payment.dto.RefundReq;
import com.minipay.payment.mapper.PaymentMapper;
import com.minipay.payment.mapper.PaymentFlowMapper;
import com.minipay.payment.mapper.PaymentOrderMapper;
import com.minipay.payment.mapper.RefundOrderMapper;
import com.minipay.payment.model.Payment;
import com.minipay.payment.model.PaymentFlow;
import com.minipay.payment.model.PaymentOrder;
import com.minipay.payment.model.RefundOrder;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
    private PaymentOrderMapper paymentOrderMapper;

    @Resource
    private PaymentFlowMapper paymentFlowMapper;

    @Resource
    private RefundOrderMapper refundOrderMapper;

    @Resource
    private AlipayConfig alipayConfig;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${minipay.services.order-url:http://localhost:8081/api/orders}")
    private String orderServiceUrl;

    @Value("${minipay.services.inventory-url:http://localhost:8086/api/inventory}")
    private String inventoryServiceUrl;

    /**
     * 创建支付订单
     * @param orderId 订单ID
     * @param amount 支付金额
     * @return Payment 支付订单
     */
    @CacheEvict(cacheNames = {
        "payment:legacy:order",
        "payment:legacy:payment",
        "payment:order:paymentNo",
        "payment:order:orderNo"
    }, allEntries = true)
    public Payment createPayment(String orderId, BigDecimal amount) {
        LOG.info("创建支付订单, orderId: {}, amount: {}", orderId, amount);

        // 如果已有待支付的记录，直接返回（避免重复创建）
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Payment::getOrderId, orderId);
        wrapper.eq(Payment::getStatus, "PENDING");
        wrapper.orderByDesc(Payment::getCreatedAt);
        List<Payment> existing = paymentMapper.selectList(wrapper);
        // 如果已经有待支付记录
        if (!existing.isEmpty()) {
            LOG.info("复用已有待支付记录, paymentId: {}", existing.get(0).getPaymentId());
            return existing.get(0);
        }

        // 没有待支付记录则新建支付实体类对象
        Payment payment = new Payment();
        payment.setPaymentId(UUID.randomUUID().toString().replace("-", ""));
        payment.setOrderId(orderId);
        payment.setAmount(amount);
        payment.setStatus("PENDING");
        payment.setCreatedAt(LocalDateTime.now());
        paymentMapper.insert(payment);
        return payment;
    }

    /**
     *  随机数模拟支付成功
     */
    @CacheEvict(cacheNames = {
        "payment:legacy:order",
        "payment:legacy:payment",
        "payment:order:paymentNo",
        "payment:order:orderNo"
    }, allEntries = true)
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

    /**
     * 支付成后更新状态
     */
    @CacheEvict(cacheNames = {
        "payment:legacy:order",
        "payment:legacy:payment",
        "payment:order:paymentNo",
        "payment:order:orderNo"
    }, allEntries = true)
    public void updatePaymentSuccess(String paymentId, String tradeNo) {
        LOG.info("更新支付成功状态, paymentId: {}, tradeNo: {}", paymentId, tradeNo);
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Payment::getPaymentId, paymentId);
        Payment payment = paymentMapper.selectOne(wrapper);
        // 如果支付实体类不为空并且状态为Pending则进入if块更新支付状态为SUCCESS
        if (payment != null && "PENDING".equals(payment.getStatus())) {
            payment.setStatus("SUCCESS");
            payment.setTradeNo(tradeNo);
            payment.setPaidAt(LocalDateTime.now());
            paymentMapper.updateById(payment);
            updateOrderStatus(payment.getOrderId(), "PAID");
        }
    }

    /**
     * 查询支付宝支付返回的支付状态
     */
    @CacheEvict(cacheNames = {
        "payment:legacy:order",
        "payment:legacy:payment",
        "payment:order:paymentNo",
        "payment:order:orderNo"
    }, allEntries = true)
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

    /**
     * 更新订单状态
     */
    private void updateOrderStatus(String orderId, String paymentStatus) {
        try {
            // 支付层的成功标识 SUCCESS → 订单服务识别的状态值 PAID(已支付)
            String orderStatus = switch (paymentStatus) {
                case "SUCCESS", "PAID" -> "PAID";
                case "FAILED" -> "CLOSED";
                case "PENDING", "WAITING" -> "PAYING";
                default -> paymentStatus;
            };
            String url = orderServiceUrl + "/" + orderId + "/status";
            Map<String, String> request = new HashMap<>();
            request.put("status", orderStatus);
            LOG.info("通知订单服务更新状态, orderId: {}, status: {}", orderId, orderStatus);
            // 发送 PUT 请求，执行远程调用更新订单状态
            restTemplate.put(url, request);
        } catch (Exception e) {
            LOG.error("通知订单服务失败, orderId: {}, error: {}", orderId, e.getMessage());
        }
    }

    // 根据订单ID去数据库查询支付
    @Cacheable(cacheNames = "payment:legacy:order", key = "#root.target.cacheKey(#orderId)", unless = "#result == null")
    public Payment getPaymentByOrderId(String orderId) {
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Payment::getOrderId, orderId);
        wrapper.orderByDesc(Payment::getCreatedAt);
        List<Payment> payments = paymentMapper.selectList(wrapper);
        return payments.isEmpty() ? null : payments.get(0);
    }

    // 根据支付id查询
    @Cacheable(cacheNames = "payment:legacy:payment", key = "#root.target.cacheKey(#paymentId)", unless = "#result == null")
    public Payment getPaymentByPaymentId(String paymentId) {
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Payment::getPaymentId, paymentId);
        return paymentMapper.selectOne(wrapper);
    }

    /**
     * 创建支付订单
     */
    @CacheEvict(cacheNames = {
        "payment:legacy:order",
        "payment:legacy:payment",
        "payment:order:paymentNo",
        "payment:order:orderNo"
    }, allEntries = true)
    public PaymentOrder createPaymentOrder(CreatePaymentReq req) {
        validatePaymentReq(req);
        Map<String, Object> order = requireTradeOrder(req.getOrderNo());
        Long orderUserId = toLong(order.get("userId"));
        if (orderUserId == null) {
            throw new IllegalStateException("订单用户不存在，无法创建支付单");
        }
        validatePayAmount(req, order);
        // 已有则复用返回
        PaymentOrder existing = getPaymentOrderByOrderNo(req.getOrderNo());
        if (existing != null && !"CLOSED".equals(existing.getStatus())) {
            if (!orderUserId.equals(existing.getUserId())) {
                existing.setUserId(orderUserId);
                existing.setUpdatedAt(LocalDateTime.now());
                paymentOrderMapper.updateById(existing);
            }
            LOG.info("复用已有支付单, paymentNo: {}, orderNo: {}", existing.getPaymentNo(), req.getOrderNo());
            return existing;
        }

        // 没有，创建
        LocalDateTime now = LocalDateTime.now();
        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setPaymentNo(generateNo("PAY"));
        paymentOrder.setOrderNo(req.getOrderNo());
        paymentOrder.setUserId(orderUserId);
        paymentOrder.setPayAmount(req.getPayAmount());
        paymentOrder.setPayChannel(req.getPayChannel() == null ? "ALIPAY_MOCK" : req.getPayChannel());
        paymentOrder.setStatus("WAITING");
        paymentOrder.setCreatedAt(now);
        paymentOrder.setUpdatedAt(now);
        paymentOrderMapper.insert(paymentOrder);
        saveFlow(paymentOrder, "WAITING", "创建支付单");
        return paymentOrder;
    }

    // 使用支付流水号查询支付单
    @Cacheable(cacheNames = "payment:order:paymentNo", key = "#root.target.cacheKey(#paymentNo)", unless = "#result == null")
    public PaymentOrder getPaymentOrder(String paymentNo) {
        LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentOrder::getPaymentNo, paymentNo);
        return paymentOrderMapper.selectOne(wrapper);
    }

    // 使用订单号查
    @Cacheable(cacheNames = "payment:order:orderNo", key = "#root.target.cacheKey(#orderNo)", unless = "#result == null")
    public PaymentOrder getPaymentOrderByOrderNo(String orderNo) {
        LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentOrder::getOrderNo, orderNo);
        wrapper.orderByDesc(PaymentOrder::getCreatedAt);
        List<PaymentOrder> orders = paymentOrderMapper.selectList(wrapper);
        return orders.isEmpty() ? null : orders.get(0);
    }

    // 模拟支付
    @CacheEvict(cacheNames = {
        "payment:legacy:order",
        "payment:legacy:payment",
        "payment:order:paymentNo",
        "payment:order:orderNo"
    }, allEntries = true)
    public PaymentOrder mockCallback(CreatePaymentReq req) {
        validatePaymentReq(req);
        // 创建支付实体类
        PaymentOrder paymentOrder = createPaymentOrder(req);
        // 状态为成功则直接返回实体类
        if ("SUCCESS".equals(paymentOrder.getStatus())) {
            return paymentOrder;
        }
        // 支付状态不是成功则设置状态为成功后返回
        paymentOrder.setStatus("SUCCESS");
        paymentOrder.setPaidAt(LocalDateTime.now());
        paymentOrder.setUpdatedAt(LocalDateTime.now());
        paymentOrderMapper.updateById(paymentOrder);
        saveFlow(paymentOrder, "SUCCESS", "模拟支付成功");
        notifyOrderPaid(paymentOrder.getOrderNo());
        deductInventory(paymentOrder.getOrderNo());
        return paymentOrder;
    }

    // 关闭支付订单
    @CacheEvict(cacheNames = {
        "payment:legacy:order",
        "payment:legacy:payment",
        "payment:order:paymentNo",
        "payment:order:orderNo"
    }, allEntries = true)
    public PaymentOrder closePaymentOrder(String paymentNo) {
        PaymentOrder paymentOrder = getPaymentOrder(paymentNo);
        if (paymentOrder == null) {
            return null;
        }
        // 支付成功状态不允许关闭
        if ("SUCCESS".equals(paymentOrder.getStatus())) {
            return paymentOrder;
        }
        // 更改为关闭状态
        paymentOrder.setStatus("CLOSED");
        paymentOrder.setClosedAt(LocalDateTime.now());
        paymentOrder.setUpdatedAt(LocalDateTime.now());
        paymentOrderMapper.updateById(paymentOrder);
        saveFlow(paymentOrder, "CLOSED", "关闭支付单");
        return paymentOrder;
    }

    // 退款
    @CacheEvict(cacheNames = {
        "payment:legacy:order",
        "payment:legacy:payment",
        "payment:order:paymentNo",
        "payment:order:orderNo"
    }, allEntries = true)
    public RefundOrder refund(String paymentNo, RefundReq req) {
        PaymentOrder paymentOrder = getPaymentOrder(paymentNo);
        if (paymentOrder == null) {
            return null;
        }
        // 如果支付订单状态不是成功，则不能退款
        if (!"SUCCESS".equals(paymentOrder.getStatus())) {
            return null;
        }
        // 新建退款对象
        RefundOrder refundOrder = new RefundOrder();
        refundOrder.setRefundNo(generateNo("REF"));
        refundOrder.setPaymentNo(paymentOrder.getPaymentNo());
        refundOrder.setOrderNo(paymentOrder.getOrderNo());
        refundOrder.setRefundAmount(req.getRefundAmount() == null ? paymentOrder.getPayAmount() : req.getRefundAmount());
        refundOrder.setReason(req.getReason());
        refundOrder.setStatus("CREATED");
        refundOrder.setCreatedAt(LocalDateTime.now());
        refundOrder.setUpdatedAt(LocalDateTime.now());
        refundOrderMapper.insert(refundOrder);
        return refundOrder; // 返回退款对象，但未实现实际退款逻辑TODO
    }

    // 验证PaymentReq
    private void validatePaymentReq(CreatePaymentReq req) {
        if (req == null || req.getOrderNo() == null || req.getOrderNo().isEmpty()) {
            throw new IllegalArgumentException("订单号不能为空");
        }
        if (req.getPayAmount() == null || req.getPayAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("支付金额必须大于0");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> requireTradeOrder(String orderNo) {
        Map<String, Object> orderResp = restTemplate.getForObject(orderServiceUrl + "/trade/" + orderNo, Map.class);
        if (orderResp == null || !(orderResp.get("data") instanceof Map<?, ?> order)) {
            throw new IllegalArgumentException("订单不存在，无法创建支付单");
        }
        return (Map<String, Object>) order;
    }

    private void validatePayAmount(CreatePaymentReq req, Map<String, Object> order) {
        BigDecimal orderPayAmount = toBigDecimal(order.get("payAmount"));
        if (orderPayAmount == null) {
            orderPayAmount = toBigDecimal(order.get("amount"));
        }
        if (orderPayAmount != null && req.getPayAmount().compareTo(orderPayAmount) != 0) {
            throw new IllegalArgumentException("支付金额与订单应付金额不一致");
        }
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        return Long.valueOf(String.valueOf(value));
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal bigDecimal) return bigDecimal;
        if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
        return new BigDecimal(String.valueOf(value));
    }

    // 新建保存至支付流水实体类
    private void saveFlow(PaymentOrder paymentOrder, String status, String message) {
        // 新建支付流水实体类
        PaymentFlow flow = new PaymentFlow();
        flow.setPaymentNo(paymentOrder.getPaymentNo());
        flow.setOrderNo(paymentOrder.getOrderNo());
        flow.setPayChannel(paymentOrder.getPayChannel());
        flow.setAmount(paymentOrder.getPayAmount());
        flow.setStatus(status);
        flow.setResponseBody(message);
        flow.setCreatedAt(LocalDateTime.now());
        paymentFlowMapper.insert(flow);// 保存至表
    }

    // 请求order服务，告知支付成功
    private void notifyOrderPaid(String orderNo) {
        try {
            String url = orderServiceUrl + "/" + orderNo + "/paid";
            LOG.info("通知订单服务支付成功, orderNo: {}", orderNo);
            restTemplate.postForObject(url, null, String.class);
        } catch (Exception e) {
            LOG.error("通知订单服务支付成功失败, orderNo: {}, error: {}", orderNo, e.getMessage());
        }
    }

    // 减少库存
    @SuppressWarnings("unchecked")
    private void deductInventory(String orderNo) {
        try {
            Map<String, Object> orderResp = restTemplate.getForObject(orderServiceUrl + "/trade/" + orderNo, Map.class);
            // 如果请求后返回的结果为空或者不是Map类型，则直接返回
            if (orderResp == null || !(orderResp.get("data") instanceof Map<?, ?> order)) {
                return;
            }
            // 获取订单中的商品列表，如果不是List类型，则直接返回
            Object itemsValue = order.get("items");
            if (!(itemsValue instanceof List<?> items)) {
                return;
            }
            // 遍历商品列表，逐个扣减库存
            for (Object itemValue : items) {
                if (!(itemValue instanceof Map<?, ?> item)) {
                    continue;
                }
                Map<String, Object> req = new HashMap<>();
                req.put("orderNo", orderNo);
                req.put("skuId", item.get("skuId"));
                req.put("quantity", item.get("quantity"));
                restTemplate.postForObject(inventoryServiceUrl + "/deduct", req, Object.class);
            }
        } catch (Exception e) {
            LOG.warn("库存扣减失败或库存服务未启动, orderNo: {}, error: {}", orderNo, e.getMessage());
        }
    }

    // 生成单号
    private String generateNo(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    public String cacheKey(String value) {
        return value == null ? "null" : value.trim();
    }
}
