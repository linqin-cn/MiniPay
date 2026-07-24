package com.minipay.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minipay.common.enums.OrderStatus;
import com.minipay.order.dto.OrderItemReq;
import com.minipay.order.mapper.OrderStatusLogMapper;
import com.minipay.order.model.OrderStatusLog;
import com.minipay.order.model.ProductSku;
import com.minipay.order.mapper.OrderMapper;
import com.minipay.order.dto.CreateOrderReq;
import com.minipay.order.dto.OrderConfirmResp;
import com.minipay.order.model.Order;
import com.minipay.order.model.OrderItem;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class OrderService {
    private static final Logger LOG = LoggerFactory.getLogger(OrderService.class);

    private static final Set<String> VALID_STATUSES = new HashSet<>(Arrays.asList("PENDING", "PAID", "FAILED"));

    @Resource
    private OrderMapper orderMapper;
    @Resource
    private ProductService productService;
    @Resource
    private OrderStatusLogMapper orderStatusLogMapper;

    // TODO后面替换为createTradeOrder
    public Order createOrder(BigDecimal amount, String description) {
        Order order = new Order();
        order.setOrderId(UUID.randomUUID().toString().replace("-", ""));
        order.setAmount(amount);
        order.setDescription(description);
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.insert(order);
        LOG.info("创建订单");
        return order;
    }

    public Order getOrder(String orderId) {
        LOG.info("查询订单, orderId: {}", orderId);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderId, orderId);
        return orderMapper.selectOne(wrapper);
    }

    public List<Order> getOrderList() {
        LOG.info("查询所有订单");
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Order::getCreatedAt);
        return orderMapper.selectList(wrapper);
    }

    // 确认订单，计算金额
    public OrderConfirmResp confirmOrder(CreateOrderReq req) {
        OrderConfirmResp resp = new OrderConfirmResp();
        // 普通 double / float 存金额会出现浮点精度丢失（例如 0.1+0.2≠0.3），所以钱、金额、价格必须统一用 BigDecimal 类型存储计算。
        BigDecimal totalAmount = BigDecimal.ZERO; // 商品原价合计，初始0元


        List<OrderItem> orderItems = new ArrayList<>();

        if (req.getItems() != null) {
            for (OrderItemReq itemReq : req.getItems()) {
                if (itemReq.getSkuId() == null || itemReq.getQuantity() == null
                        || itemReq.getQuantity() <= 0) {
                    throw new IllegalArgumentException("商品 SKU 和数量不能为空，且数量必须大于 0");
                }

                // 应调用商品服务，根据 skuId 查询真实商品信息
                ProductSku sku = productService.getSkuById(itemReq.getSkuId());
                if (sku == null) {
                    throw new IllegalArgumentException(
                            "SKU 不存在：" + itemReq.getSkuId()
                    );
                }

                if (sku.getStock() < itemReq.getQuantity()) {
                    throw new IllegalStateException(
                            "商品库存不足：" + sku.getTitle()
                    );
                }

                BigDecimal itemTotal = sku.getPrice().multiply(
                        BigDecimal.valueOf(itemReq.getQuantity())
                );

                OrderItem orderItem = new OrderItem();
                orderItem.setSkuId(itemReq.getSkuId());
                orderItem.setProductId(sku.getProductId());
                orderItem.setTitle(sku.getTitle());
                orderItem.setImage(sku.getImage());
                orderItem.setPrice(sku.getPrice());
                orderItem.setQuantity(itemReq.getQuantity());
                orderItem.setTotalAmount(itemTotal);

                orderItems.add(orderItem);
                totalAmount = totalAmount.add(itemTotal);
            }
        }

        BigDecimal discountAmount = BigDecimal.ZERO; // 优惠减免金额，初始0元
        BigDecimal freightAmount = BigDecimal.ZERO; // 运费，初始0元
        BigDecimal payAmount = totalAmount
                .subtract(discountAmount)
                .add(freightAmount);

        resp.setTotalAmount(totalAmount);
        resp.setDiscountAmount(discountAmount);
        resp.setFreightAmount(freightAmount);
        resp.setPayAmount(payAmount);
        resp.setItems(req.getItems());
        return resp;
    }

    // 创建订单
    public Order createTradeOrder(CreateOrderReq req) {
        if (req == null || req.getItems() == null || req.getItems().isEmpty()) {
            throw new IllegalArgumentException("订单商品不能为空");
        }
        // 计算订单的应付金额、优惠金额、运费等信息
        OrderConfirmResp confirmResp = confirmOrder(req);

        Order order = new Order();
        order.setOrderId(UUID.randomUUID().toString().replace("-", ""));
        order.setAmount(confirmResp.getPayAmount());
        order.setDescription(req.getRemark());
        order.setStatus(OrderStatus.CREATED.name());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        orderMapper.insert(order);
        LOG.info("订单已创建, orderId: {}, status: {}", order.getOrderId(), OrderStatus.CREATED.name());

        return order;
    }

    // 获取订单信息
    public Order getOrderByOrderNo(String orderNo) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderId, orderNo);
        return orderMapper.selectOne(wrapper);
    }

    // 取消订单
    public Order cancelOrder(String orderNo) {
        Order order = getOrderByOrderNo(orderNo);

        if (order == null) {
            throw new IllegalArgumentException("订单不存在：" + orderNo);
        }

        // 订单不是 CREATED 并且订单也不是 PAYING那么不能取消
        if (!OrderStatus.CREATED.name().equals(order.getStatus())
                && !OrderStatus.PAYING.name().equals(order.getStatus())) {
            throw new IllegalArgumentException("订单状态不允许取消：" + order.getStatus());
        }

        return changeStatus(
                order,
                OrderStatus.CANCELLED.name(),
                "取消订单"
        );
    }

    // 更改支付状态为成功
    public Order markPaid(String orderNo) {
        Order order = getOrderByOrderNo(orderNo);
        if (order == null) {
            return null;
        }

        if (!OrderStatus.CREATED.name().equals(order.getStatus())
                && !OrderStatus.PAYING.name().equals(order.getStatus())) {
            throw new IllegalArgumentException("订单状态不允许支付：" + order.getStatus());
        }

        return changeStatus(order, OrderStatus.PAID.name(), "支付成功");
    }

    // 更改订单状态为商家发货
    public Order shipOrder(String orderNo) {
        Order order = getOrderByOrderNo(orderNo);
        if (order == null) {
            return null;
        }

        if (!OrderStatus.PAID.name().equals(order.getStatus())) {
            return null;
        }

        return changeStatus(order, OrderStatus.SHIPPED.name(), "商家发货");
    }

    // 更改订单状态为用户确认收货
    public Order receiveOrder(String orderNo) {
        Order order = getOrderByOrderNo(orderNo);
        if (order == null) {
            return null;
        }

        if (!OrderStatus.SHIPPED.name().equals(order.getStatus())) {
            return null;
        }

        return changeStatus(order, OrderStatus.RECEIVED.name(), "用户确认收货");
    }

    private Order changeStatus(Order order, String newStatus, String remark) {
        String oldStatus = order.getStatus();
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);

        saveStatusLog(order.getOrderId(), oldStatus, newStatus, remark);
        return order;
    }

    // 记录状态日志
    private void saveStatusLog(String orderNo, String fromStatus, String toStatus, String remark) {
        OrderStatusLog log = new OrderStatusLog();
        log.setOrderNo(orderNo);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setRemark(remark);
        log.setCreatedAt(LocalDateTime.now());
        orderStatusLogMapper.insert(log);
    }

    public Order updateOrderStatus(String orderId, String status) {
        LOG.info("更新订单状态, orderId: {}, status: {}", orderId, status);
        if (!VALID_STATUSES.contains(status)) {
            LOG.warn("非法状态: {}", status);
            return null;
        }
        Order order = getOrder(orderId);
        if (order == null) {
            LOG.warn("订单不存在, orderId: {}", orderId);
            return null;
        }
        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        return order;
    }

    public String health() {
        LOG.info("订单服务健康检查");
        return "order-service is running";
    }
}
