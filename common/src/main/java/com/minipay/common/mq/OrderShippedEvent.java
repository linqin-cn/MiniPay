package com.minipay.common.mq;

import java.io.Serializable;
import java.time.LocalDateTime;

public class OrderShippedEvent implements Serializable {
    private String orderNo;
    private Long userId;
    private LocalDateTime shippedAt;

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDateTime getShippedAt() {
        return shippedAt;
    }

    public void setShippedAt(LocalDateTime shippedAt) {
        this.shippedAt = shippedAt;
    }
}
