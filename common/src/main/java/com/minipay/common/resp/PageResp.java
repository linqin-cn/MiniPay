package com.minipay.common.resp;

import java.util.List;
/**
 * 分页响应结果
 * @param <T> 响应数据类型
 */
public class PageResp<T> {
    // 总记录数
    private Long total;
    // 当前页数据列表
    private List<T> list;

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }
}
