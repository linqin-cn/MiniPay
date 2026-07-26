package com.minipay.order.dto;

import java.math.BigDecimal;
import java.util.List;
/**
 * 订单确认返回参数
 */
public class OrderConfirmResp {
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal freightAmount;
    private BigDecimal payAmount;
    private List<OrderItemReq> items;

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getFreightAmount() { return freightAmount; }
    public void setFreightAmount(BigDecimal freightAmount) { this.freightAmount = freightAmount; }
    public BigDecimal getPayAmount() { return payAmount; }
    public void setPayAmount(BigDecimal payAmount) { this.payAmount = payAmount; }
    public List<OrderItemReq> getItems() { return items; }
    public void setItems(List<OrderItemReq> items) { this.items = items; }
}
