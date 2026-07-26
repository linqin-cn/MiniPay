package com.minipay.payment.dto;

import java.math.BigDecimal;

/**
 * 退款请求参数
 */
public class RefundReq {
//    退款金额
    private BigDecimal refundAmount;
//    退款原因
    private String reason;

    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
