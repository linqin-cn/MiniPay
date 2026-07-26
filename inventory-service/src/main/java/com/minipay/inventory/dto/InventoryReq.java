package com.minipay.inventory.dto;

public class InventoryReq {
    // 订单号
    private String orderNo;
    // 商品skuId
    private Long skuId;
    // 商品数量
    private Integer quantity;

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
