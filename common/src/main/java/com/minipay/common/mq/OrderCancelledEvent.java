package com.minipay.common.mq;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderCancelledEvent implements Serializable {
    private String orderNo;
    private Long userId;
    private LocalDateTime cancelledAt;
    private List<OrderItemMessage> items = new ArrayList<>();

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

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public List<OrderItemMessage> getItems() {
        return items;
    }

    public void setItems(List<OrderItemMessage> items) {
        this.items = items == null ? new ArrayList<>() : items;
    }
}
