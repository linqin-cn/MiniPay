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
        return new CommonResp<>(200, "创建成功", order, true);
    }

    @PostMapping("/confirm")
    public CommonResp<OrderConfirmResp> confirmOrder(@RequestBody CreateOrderReq req) {
        return new CommonResp<>(200, "TODO", orderService.confirmOrder(req), true);
    }

    @PostMapping("/trade")
    public CommonResp<Order> createTradeOrder(@RequestBody CreateOrderReq req) {
        return new CommonResp<>(200, "TODO", orderService.createTradeOrder(req), true);
    }

    @GetMapping
    public CommonResp<List<Order>> getOrderList() {
        List<Order> orders = orderService.getOrderList();
        return new CommonResp<>(200, "查询成功", orders, true);
    }

    @GetMapping("/{orderId}")
    public CommonResp<Order> getOrder(@PathVariable("orderId") String orderId) {
        Order order = orderService.getOrder(orderId);
        if (order == null) {
            return new CommonResp<>(404, "订单不存在", null, false);
        }
        return new CommonResp<>(200, "查询成功", order, true);
    }

    @GetMapping("/trade/{orderNo}")
    public CommonResp<Order> getTradeOrder(@PathVariable String orderNo) {
        return new CommonResp<>(200, "TODO", orderService.getOrderByOrderNo(orderNo), true);
    }

    @PostMapping("/{orderNo}/cancel")
    public CommonResp<Order> cancelOrder(@PathVariable String orderNo) {
        return new CommonResp<>(200, "TODO", orderService.cancelOrder(orderNo), true);
    }

    @PostMapping("/{orderNo}/paid")
    public CommonResp<Order> markPaid(@PathVariable String orderNo) {
        return new CommonResp<>(200, "TODO", orderService.markPaid(orderNo), true);
    }

    @PostMapping("/{orderNo}/ship")
    public CommonResp<Order> shipOrder(@PathVariable String orderNo) {
        return new CommonResp<>(200, "TODO", orderService.shipOrder(orderNo), true);
    }

    @PostMapping("/{orderNo}/receive")
    public CommonResp<Order> receiveOrder(@PathVariable String orderNo) {
        return new CommonResp<>(200, "TODO", orderService.receiveOrder(orderNo), true);
    }

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

    @GetMapping("/health")
    public CommonResp<String> health() {
        String result = orderService.health();
        return new CommonResp<>(200, "success", result, true);
    }
}
