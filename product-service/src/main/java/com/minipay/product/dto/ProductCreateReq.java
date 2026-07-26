package com.minipay.product.dto;

import java.math.BigDecimal;

/**
 * 创建商品请求 DTO
 */
public class ProductCreateReq {
    private Long merchantId;
    private Long categoryId;
    private String title;
    private String description;
    private String mainImage;
    private String skuName;
    private BigDecimal price;
    private BigDecimal originalPrice;

    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getMainImage() { return mainImage; }
    public void setMainImage(String mainImage) { this.mainImage = mainImage; }

    public String getSkuName() { return skuName; }
    public void setSkuName(String skuName) { this.skuName = skuName; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }
}
