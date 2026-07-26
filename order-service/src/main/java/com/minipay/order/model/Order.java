package com.minipay.order.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单模型
 */
@Data
@TableName("orders")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;

    // 订单号、用户ID、金额、描述、总金额、折扣金额、运费金额、支付金额、状态
    private String orderId;// 兼容旧版本
    private String orderNo;
    private Long userId;
    private BigDecimal amount;
    private String description;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal freightAmount;
    private BigDecimal payAmount;
    private String status;
    // 收货人名字、电话，地址，备注
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String remark;
    // 创建时间、支付时间、取消时间、完成时间、更新时间
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime completedAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private List<OrderItem> items;
}
