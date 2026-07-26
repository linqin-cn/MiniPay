package com.minipay.common.req;

import java.math.BigDecimal;

/**
 * 订单请求参数
 */
public class OrderReq {
    private Long orderId;
    private BigDecimal amount;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "OrderReq{" +
                "orderId=" + orderId +
                ", amount=" + amount +
                '}';
    }
}
