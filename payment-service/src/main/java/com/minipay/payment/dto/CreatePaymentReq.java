package com.minipay.payment.dto;

import java.math.BigDecimal;

public class CreatePaymentReq {
    private String orderNo;
    private BigDecimal payAmount;
    private String payChannel;

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public BigDecimal getPayAmount() { return payAmount; }
    public void setPayAmount(BigDecimal payAmount) { this.payAmount = payAmount; }
    public String getPayChannel() { return payChannel; }
    public void setPayChannel(String payChannel) { this.payChannel = payChannel; }
}
