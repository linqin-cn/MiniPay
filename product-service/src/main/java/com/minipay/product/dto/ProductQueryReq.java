package com.minipay.product.dto;

import com.minipay.common.req.PageReq;

/**
 * 查询商品请求 DTO
 */
public class ProductQueryReq extends PageReq {
    // 继承pageNo，pageSize及其 getter/setter方法
    private Long categoryId;
    private Long merchantId;
    private String keyword;
    private String status;

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
