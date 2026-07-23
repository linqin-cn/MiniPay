package com.minipay.payment.dto;

import java.math.BigDecimal;

public class RefundReq {
    private BigDecimal refundAmount;
    private String reason;

    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
