package com.minipay.cart.dto;

/**
 * 购物车商品请求参数
 */
public class CartItemReq {
    // 商品id
    private Long productId;
    // 商品具体skuId
    private Long skuId;
    //  商品数量
    private Integer quantity;
    // 是否选中
    private Boolean selected;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Boolean getSelected() { return selected; }
    public void setSelected(Boolean selected) { this.selected = selected; }
}
