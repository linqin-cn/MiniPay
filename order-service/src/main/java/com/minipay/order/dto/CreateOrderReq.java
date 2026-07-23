package com.minipay.order.dto;

import java.util.List;

public class CreateOrderReq {
    private Long addressId;
    private Long couponId;
    private String remark;
    private List<OrderItemReq> items;

    public Long getAddressId() { return addressId; }
    public void setAddressId(Long addressId) { this.addressId = addressId; }
    public Long getCouponId() { return couponId; }
    public void setCouponId(Long couponId) { this.couponId = couponId; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public List<OrderItemReq> getItems() { return items; }
    public void setItems(List<OrderItemReq> items) { this.items = items; }
}
