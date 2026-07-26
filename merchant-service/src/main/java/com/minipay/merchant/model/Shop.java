package com.minipay.merchant.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 店铺实体类--具体的商铺
 */
@Data
@TableName("shop")
public class Shop {
    @TableId(type = IdType.AUTO)
    private Long id;
    // 属于哪个商家
    private Long merchantId;
    private String shopName;
    private String logo;
    private String status;
}
