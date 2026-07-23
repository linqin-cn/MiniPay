package com.minipay.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minipay.payment.model.RefundOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RefundOrderMapper extends BaseMapper<RefundOrder> {
}
