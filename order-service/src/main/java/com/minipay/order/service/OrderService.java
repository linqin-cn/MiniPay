package com.minipay.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minipay.common.enums.OrderStatus;
import com.minipay.common.util.JwtUtil;
import com.minipay.order.dto.CreateOrderReq;
import com.minipay.order.dto.OrderConfirmResp;
import com.minipay.order.dto.OrderItemReq;
import com.minipay.order.mapper.OrderItemMapper;
import com.minipay.order.mapper.OrderMapper;
import com.minipay.order.mapper.OrderStatusLogMapper;
import com.minipay.order.model.Order;
import com.minipay.order.model.OrderItem;
import com.minipay.order.model.OrderStatusLog;
import com.minipay.order.model.Product;
import com.minipay.order.model.ProductSku;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class OrderService {
    private static final Logger LOG = LoggerFactory.getLogger(OrderService.class);
    // 定义一个 Set 来存储合法的旧版订单状态
    private static final Set<String> VALID_LEGACY_STATUSES = new HashSet<>(Arrays.asList("PENDING", "PAID", "FAILED"));
    private static final String INVENTORY_SERVICE_URL = "http://localhost:8087/api/inventory";

    private final RestTemplate restTemplate = new RestTemplate();

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private OrderItemMapper orderItemMapper;

    @Resource
    private OrderStatusLogMapper orderStatusLogMapper;

    @Resource
    private ProductService productService;

    @Resource
    private HttpServletRequest request;

    /**
     * 创建旧版订单
     */
    public Order createOrder(BigDecimal amount, String description) {
        Order order = new Order();
        String orderId = UUID.randomUUID().toString().replace("-", "");
        order.setOrderId(orderId);
        order.setOrderNo(orderId);
        order.setUserId(currentUserId());
        order.setAmount(amount);
        order.setDescription(description);
        order.setTotalAmount(amount);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setFreightAmount(BigDecimal.ZERO);
        order.setPayAmount(amount);
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.insert(order);
        LOG.info("创建旧版订单, orderId: {}", orderId);
        return order;
    }

    /**
     * 根据id查询订单
     */
    public Order getOrder(String orderId) {
        LOG.info("查询订单, orderId: {}", orderId);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderId, orderId).or().eq(Order::getOrderNo, orderId);
        Order order = orderMapper.selectOne(wrapper);
        // 获取订单项，存入order,order表中没有订单项，@TableField(exist = false)
        fillOrderItems(order);
        return order;
    }

    /**
     * 查询当前用户的订单列表
     */
    public List<Order> getOrderList() {
        LOG.info("查询所有订单");
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, currentUserId());
        wrapper.orderByDesc(Order::getCreatedAt);
        List<Order> orders = orderMapper.selectList(wrapper);
        orders.forEach(this::fillOrderItems);
        return orders;
    }

    /**
     * 确认订单
     */
    public OrderConfirmResp confirmOrder(CreateOrderReq req) {
        // 创建订单项列表，并计算总金额、折扣金额、运费和应付金额
        List<OrderItem> orderItems = buildOrderItems(null, req);
        // 计算总金额
        BigDecimal totalAmount = orderItems.stream()
                .map(OrderItem::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal freightAmount = totalAmount.compareTo(new BigDecimal("99.00")) >= 0 ? BigDecimal.ZERO : new BigDecimal("12.00");
        BigDecimal payAmount = totalAmount.subtract(discountAmount).add(freightAmount);

        // 创建订单确认返回对象
        OrderConfirmResp resp = new OrderConfirmResp();
        resp.setTotalAmount(totalAmount);
        resp.setDiscountAmount(discountAmount);
        resp.setFreightAmount(freightAmount);
        resp.setPayAmount(payAmount);
        resp.setItems(req.getItems());
        return resp;
    }

    /**
     * 创建交易订单
     */
    @Transactional
    public Order createTradeOrder(CreateOrderReq req) {
        if (req == null || req.getItems() == null || req.getItems().isEmpty()) {
            throw new IllegalArgumentException("订单商品不能为空");
        }

        // 生成订单号
        String orderNo = generateOrderNo();
        // 从req中获取订单项列表，并锁定库存
        List<OrderItem> orderItems = buildOrderItems(orderNo, req);
        lockInventory(orderNo, orderItems);
        BigDecimal totalAmount = orderItems.stream()
                // 获取每个订单项的总金额，并累加计算订单总金额
                .map(OrderItem::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discountAmount = BigDecimal.ZERO;
        // 订单总价 ≥ 99 元 → 运费 = 0（包邮）
        // 订单总价 ＜ 99 元 → 运费固定 12 元
        BigDecimal freightAmount = totalAmount.compareTo(new BigDecimal("99.00")) >= 0 ? BigDecimal.ZERO : new BigDecimal("12.00");
        BigDecimal payAmount = totalAmount.subtract(discountAmount).add(freightAmount);
        LocalDateTime now = LocalDateTime.now();

        // 创建订单实体并保存到数据库
        Order order = new Order();
        order.setOrderId(orderNo);
        order.setOrderNo(orderNo);
        order.setUserId(currentUserId());
        order.setAmount(payAmount);
        order.setDescription(req.getRemark());
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount);
        order.setFreightAmount(freightAmount);
        order.setPayAmount(payAmount);
        order.setStatus(OrderStatus.CREATED.name());
        order.setReceiverName("林同学");
        order.setReceiverPhone("13000000000");
        order.setReceiverAddress("上海市 上海市 浦东新区 MiniPay 路 100 号");
        order.setRemark(req.getRemark());
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        orderMapper.insert(order);

        for (OrderItem item : orderItems) {
            item.setCreatedAt(now);
            orderItemMapper.insert(item);
        }
        saveStatusLog(orderNo, null, OrderStatus.CREATED.name(), "创建交易订单");
        order.setItems(orderItems);
        LOG.info("交易订单已创建, orderNo: {}, payAmount: {}", orderNo, payAmount);
        return order;
    }

    /**
     * 根据订单号查询订单
     * @param orderNo 订单号
     * @return 订单信息
     */
    public Order getOrderByOrderNo(String orderNo) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo).or().eq(Order::getOrderId, orderNo);
        Order order = orderMapper.selectOne(wrapper);
        fillOrderItems(order);
        return order;
    }

    /**
     * 取消订单
     * @param orderNo 订单号
     * @return 订单信息
     */
    @Transactional
    public Order cancelOrder(String orderNo) {
        Order order = requireOrder(orderNo);
        if (!OrderStatus.CREATED.name().equals(order.getStatus()) && !OrderStatus.PAYING.name().equals(order.getStatus())) {
            throw new IllegalArgumentException("订单状态不允许取消：" + order.getStatus());
        }
        // 释放锁定的订单
        releaseInventory(order.getOrderNo(), order.getItems());
        order.setCancelledAt(LocalDateTime.now());
        return changeStatus(order, OrderStatus.CANCELLED.name(), "取消订单");
    }

    /**
     * 标记订单为已支付
     * @param orderNo 订单号
     * @return 订单信息
     */
    @Transactional
    public Order markPaid(String orderNo) {
        // 获取订单
        Order order = requireOrder(orderNo);
        // 如果订单状态为已支付
        if (OrderStatus.PAID.name().equals(order.getStatus())) {
            // 填充订单项信息，返回订单
            fillOrderItems(order);
            return order;
        }
        // 如果订单状态不是已创建或支付中，则抛出异常
        if (!OrderStatus.CREATED.name().equals(order.getStatus()) && !OrderStatus.PAYING.name().equals(order.getStatus())) {
            throw new IllegalArgumentException("订单状态不允许支付：" + order.getStatus());
        }
        order.setPaidAt(LocalDateTime.now());
        return changeStatus(order, OrderStatus.PAID.name(), "支付成功");
    }

    /**
     * 标记订单为已发货
     * @param orderNo 订单号
     * @return 订单信息
     */
    @Transactional
    public Order shipOrder(String orderNo) {
        Order order = requireOrder(orderNo);
        // 若未支付
        if (!OrderStatus.PAID.name().equals(order.getStatus())) {
            throw new IllegalArgumentException("订单状态不允许发货：" + order.getStatus());
        }
        return changeStatus(order, OrderStatus.SHIPPED.name(), "商家发货");
    }

    /**
     * 标记订单为已收货
     * @param orderNo 订单号
     * @return 订单信息
     */
    @Transactional
    public Order receiveOrder(String orderNo) {
        Order order = requireOrder(orderNo);
        if (!OrderStatus.SHIPPED.name().equals(order.getStatus())) {
            throw new IllegalArgumentException("订单状态不允许确认收货：" + order.getStatus());
        }
        // 设置订单完成时间为当前时间
        order.setCompletedAt(LocalDateTime.now());
        return changeStatus(order, OrderStatus.COMPLETED.name(), "用户确认收货");
    }

    /**
     * 更新订单状态
     * @param orderId 订单号
     * @param status 状态
     * @return 订单信息
     */
    public Order updateOrderStatus(String orderId, String status) {
        LOG.info("更新旧版订单状态, orderId: {}, status: {}", orderId, status);
        if (!VALID_LEGACY_STATUSES.contains(status)) {
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
        fillOrderItems(order);
        return order;
    }

    /**
     * 构建订单项
     * @param orderNo 订单号
     * @param req 创建订单请求
     * @return 订单项列表
     */
    private List<OrderItem> buildOrderItems(String orderNo, CreateOrderReq req) {
        if (req == null || req.getItems() == null || req.getItems().isEmpty()) {
            throw new IllegalArgumentException("订单商品不能为空");
        }
        List<OrderItem> orderItems = new ArrayList<>();
        for (OrderItemReq itemReq : req.getItems()) {
            if (itemReq.getSkuId() == null || itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                throw new IllegalArgumentException("商品 SKU 和数量不能为空，且数量必须大于 0");
            }
            //  获取商品 SKU 信息，如果 SKU 不存在或已下架，则抛出异常
            ProductSku sku = productService.getSkuById(itemReq.getSkuId());
            if (!"ON_SALE".equals(sku.getStatus())) {
                throw new IllegalStateException("商品 SKU 已下架：" + itemReq.getSkuId());
            }
            // 获取商品信息
            Product product = productService.getProductById(sku.getProductId());
            //  商品项总价格
            BigDecimal itemTotal = sku.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            OrderItem orderItem = new OrderItem();
            orderItem.setOrderNo(orderNo);
            orderItem.setSkuId(sku.getId());
            orderItem.setProductId(sku.getProductId());
            orderItem.setProductTitle(product == null ? sku.getSkuName() : product.getTitle());
            orderItem.setSkuName(sku.getSkuName());
            orderItem.setProductImage(product == null ? null : product.getMainImage());
            orderItem.setUnitPrice(sku.getPrice());
            orderItem.setQuantity(itemReq.getQuantity());
            orderItem.setTotalAmount(itemTotal);
            orderItems.add(orderItem);
        }
        return orderItems;
    }

    /**
     * 根据订单号查询订单
     * @param orderNo 订单号
     * @return 订单信息
     */
    private Order requireOrder(String orderNo) {
        Order order = getOrderByOrderNo(orderNo);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在：" + orderNo);
        }
        return order;
    }

    // 获取当前登录用户id
    private Long currentUserId() {
        String token = request.getHeader("token");
        Long userId = token == null || token.isEmpty() ? null : JwtUtil.getUserId(token);
        return userId == null ? 1L : userId;
    }

    // 改变数据库中订单状态
    private Order changeStatus(Order order, String newStatus, String remark) {
        String oldStatus = order.getStatus();
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        // 存入Log表
        saveStatusLog(order.getOrderNo(), oldStatus, newStatus, remark);
        fillOrderItems(order);
        return order;
    }

    /**
     * 保存订单状态日志
     * @param orderNo 订单号
     * @param fromStatus 原状态
     * @param toStatus 新状态
     * @param remark 备注
     */
    private void saveStatusLog(String orderNo, String fromStatus, String toStatus, String remark) {
        OrderStatusLog log = new OrderStatusLog();
        log.setOrderNo(orderNo);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setRemark(remark);
        log.setCreatedAt(LocalDateTime.now());
        orderStatusLogMapper.insert(log);
    }

    /**
     * 填充订单项信息
     * @param order 订单信息
     */
    private void fillOrderItems(Order order) {
        if (order == null || order.getOrderNo() == null) {
            return;
        }
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        // 根据订单号查询所有订单项
        wrapper.eq(OrderItem::getOrderNo, order.getOrderNo());
        // 将订单项列表设置到订单对象中items 属性
        order.setItems(orderItemMapper.selectList(wrapper));
    }

    /**
     * 锁定库存
     * @param orderNo 订单号
     * @param items 订单项列表
     */
    private void lockInventory(String orderNo, List<OrderItem> items) {
        for (OrderItem item : items) {
            try {
                // 转发到http://localhost:8087/api/inventory/lock 接口，锁定库存
                restTemplate.postForObject(INVENTORY_SERVICE_URL + "/lock", inventoryReq(orderNo, item), Object.class);
            } catch (Exception e) {
                LOG.warn("库存服务锁定失败，继续创建订单以便本地单服务调试, orderNo: {}, skuId: {}, error: {}", orderNo, item.getSkuId(), e.getMessage());
            }
        }
    }

    /**
     * 释放库存
     * @param orderNo 订单号
     * @param items 订单项列表
     */
    private void releaseInventory(String orderNo, List<OrderItem> items) {
        if (items == null) {
            return;
        }
        for (OrderItem item : items) {
            try {
                // inventoryReq(orderNo, item)为body请求参数，Object.class为返回类型
                restTemplate.postForObject(INVENTORY_SERVICE_URL + "/release", inventoryReq(orderNo, item), Object.class);
            } catch (Exception e) {
                LOG.warn("库存服务释放失败, orderNo: {}, skuId: {}, error: {}", orderNo, item.getSkuId(), e.getMessage());
            }
        }
    }

    /**
     * 构建库存请求
     * @param orderNo 订单号
     * @param item 订单项
     * @return 库存请求参数
     */
    private Map<String, Object> inventoryReq(String orderNo, OrderItem item) {
        Map<String, Object> req = new HashMap<>();
        req.put("orderNo", orderNo);
        req.put("skuId", item.getSkuId());
        req.put("quantity", item.getQuantity());
        return req;
    }

    /**
     * UUID生成订单号
     * @return 订单号
     */
    private String generateOrderNo() {
        return "O" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
