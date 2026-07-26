package com.minipay.order.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单项模型类，表示订单中的每一项商品
 */
@Data
@TableName("order_item")
public class OrderItem {
    // 订单id、订单号、商品id、skuId、商品标题、sku名称、商品图片、单价、数量、总金额、创建时间
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long productId;
    private Long skuId;
    private String productTitle;
    private String skuName;
    private String productImage;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String Title; // 商品名
    @TableField(exist = false)
    private String image; // 商品图片
    @TableField(exist = false)
    private BigDecimal price; // 商品价格
}
