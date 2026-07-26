package com.minipay.order.dto;

import java.math.BigDecimal;

/**
 * 订单项请求参数
 */
public class OrderItemReq {
    private Long skuId;
    private Integer quantity;
    private BigDecimal price;

    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
