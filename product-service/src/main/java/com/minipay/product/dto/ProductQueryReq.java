package com.minipay.product.dto;

import com.minipay.common.req.PageReq;

public class ProductQueryReq extends PageReq {
    // 继承pageNo，pageSize及其 getter/setter方法
    private Long categoryId;
    private String keyword;

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
}
