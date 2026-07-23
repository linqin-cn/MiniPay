package com.minipay.logistics.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("logistics_order")
public class LogisticsOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private String logisticsNo;
    private String carrierName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
