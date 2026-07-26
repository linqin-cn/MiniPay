package com.minipay.cart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minipay.cart.model.CartItem;
import org.apache.ibatis.annotations.Mapper;
//  基于mybatis-plus 继承BaseMapper
@Mapper
public interface CartItemMapper extends BaseMapper<CartItem> {
}
