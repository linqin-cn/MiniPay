package com.minipay.promotion.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券实体类
 */
@Data
@TableName("coupon")
public class Coupon {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    // 优惠金额
    private BigDecimal discountAmount;
    // 使用优惠券的最低消费金额
    private BigDecimal thresholdAmount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
