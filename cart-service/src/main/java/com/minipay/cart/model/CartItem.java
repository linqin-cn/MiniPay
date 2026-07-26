package com.minipay.cart.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据库实体
 */
@Data
@TableName("cart_item")
public class CartItem {
    // id为主键，策略为数据库自增
    @TableId(type = IdType.AUTO)
    private Long id;
    // 用户id
    private Long userId;
    // 商品id
    private Long productId;
    // 商品具体skuId
    private Long skuId;
    //  商品数量
    private Integer quantity;
    // 是否选中
    private Boolean selected;
    // 商品加入购物车时间
    private LocalDateTime createdAt;
    // 商品信息更新时间--数量更改，型号
    private LocalDateTime updatedAt;
}
