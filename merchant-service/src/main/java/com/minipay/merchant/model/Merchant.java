package com.minipay.merchant.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商家实体类--一个商家可用开多个店铺，如大疆有大疆官方旗舰店、大疆授权专卖店等
 */
@Data
@TableName("merchant")
public class Merchant {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    // 商店名称
    private String merchantName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
