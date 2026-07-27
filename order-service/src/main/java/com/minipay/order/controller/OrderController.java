package com.minipay.order.controller;

import com.minipay.common.req.OrderReq;
import com.minipay.common.resp.CommonResp;
import com.minipay.order.dto.CreateOrderReq;
import com.minipay.order.dto.OrderConfirmResp;
import com.minipay.order.model.Order;
import com.minipay.order.service.OrderService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Resource
    private OrderService orderService;

    @PostMapping
    public CommonResp<Order> createOrder(@RequestBody OrderReq req) {
        Order order = orderService.createOrder(req.getAmount(), "");
        return new CommonResp<>(200, "创建订单成功", order, true);
    }

    // 确认订单即计算订单金额用于支付
    @PostMapping("/confirm")
    public CommonResp<OrderConfirmResp> confirmOrder(@RequestBody CreateOrderReq req) {
        return new CommonResp<>(200, "订单确认成功", orderService.confirmOrder(req), true);
    }

    // 创建交易订单
    @PostMapping("/trade")
    public CommonResp<Order> createTradeOrder(@RequestBody CreateOrderReq req) {
        return new CommonResp<>(200, "创建交易订单成功", orderService.createTradeOrder(req), true);
    }

    /**
     * 获取订单列表
     * @return 订单列表
     */
    @GetMapping
    public CommonResp<List<Order>> getOrderList() {
        List<Order> orders = orderService.getOrderList();
        return new CommonResp<>(200, "查询成功", orders, true);
    }

    /**
     * 获取指定商家的订单列表
     * @param merchantId 商家 ID
     * @return 订单列表
     */
    @GetMapping("/merchant/{merchantId}")
    public CommonResp<List<Order>> getMerchantOrderList(@PathVariable Long merchantId) {
        List<Order> orders = orderService.getOrdersByMerchantId(merchantId);
        return new CommonResp<>(200, "查询商家订单成功", orders, true);
    }

    /**
     * 根据订单ID获取订单信息
     * @param orderId 订单ID
     * @return 订单信息
     */
    @GetMapping("/{orderId}")
    public CommonResp<Order> getOrder(@PathVariable("orderId") String orderId) {
        Order order = orderService.getOrder(orderId);
        if (order == null) {
            return new CommonResp<>(404, "订单不存在", null, false);
        }
        return new CommonResp<>(200, "查询成功", order, true);
    }

    /**
     * 根据订单号获取交易订单信息
     * @param orderNo 订单号
     * @return 交易订单信息
     */
    @GetMapping("/trade/{orderNo}")
    public CommonResp<Order> getTradeOrder(@PathVariable String orderNo) {
        return new CommonResp<>(200, "获取订单成功", orderService.getOrderByOrderNo(orderNo), true);
    }

    /**
     * 取消订单
     * @param orderNo 订单号
     * @return 取消结果
     */
    @PostMapping("/{orderNo}/cancel")
    public CommonResp<Order> cancelOrder(@PathVariable String orderNo) {
        return new CommonResp<>(200, "取消订单成功", orderService.cancelOrder(orderNo), true);
    }

    /**
     * 标记订单为已支付
     * @param orderNo 订单号
     * @return 支付结果
     */
    @PostMapping("/{orderNo}/paid")
    public CommonResp<Order> markPaid(@PathVariable String orderNo) {
        return new CommonResp<>(200, "支付成功", orderService.markPaid(orderNo), true);
    }

    /**
     * 标记订单为已发货
     * @param orderNo 订单号
     * @return 发货结果
     */
    @PostMapping("/{orderNo}/ship")
    public CommonResp<Order> shipOrder(@PathVariable String orderNo) {
        return new CommonResp<>(200, "商家发货成功", orderService.shipOrder(orderNo), true);
    }

    /**
     * 用户确认收货
     * @param orderNo 订单号
     * @return 确认收货结果
     */
    @PostMapping("/{orderNo}/receive")
    public CommonResp<Order> receiveOrder(@PathVariable String orderNo) {
        return new CommonResp<>(200, "用户确认收货成功", orderService.receiveOrder(orderNo), true);
    }

    /**
     * 更新订单状态
     * @param orderId 订单ID
     * @param req 请求参数
     * @return 更新结果
     */
    @PutMapping("/{orderId}/status")
    public CommonResp<Order> updateOrderStatus(@PathVariable("orderId") String orderId, @RequestBody Map<String, String> req) {
        String status = req.get("status");
        if (status == null || status.isEmpty()) {
            return new CommonResp<>(400, "状态不能为空", null, false);
        }
        Order order = orderService.updateOrderStatus(orderId, status);
        if (order == null) {
            return new CommonResp<>(404, "订单不存在或状态非法", null, false);
        }
        return new CommonResp<>(200, "状态更新成功", order, true);
    }
}
