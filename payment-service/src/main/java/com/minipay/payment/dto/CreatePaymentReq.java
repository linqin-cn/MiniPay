package com.minipay.payment.dto;

import java.math.BigDecimal;

/**
 * 创建支付请求 DTO
 */
public class CreatePaymentReq {
    // 订单号
    private String orderNo;
    // 支付金额
    private BigDecimal payAmount;
    // 支付渠道
    private String payChannel;

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public BigDecimal getPayAmount() { return payAmount; }
    public void setPayAmount(BigDecimal payAmount) { this.payAmount = payAmount; }
    public String getPayChannel() { return payChannel; }
    public void setPayChannel(String payChannel) { this.payChannel = payChannel; }
}
