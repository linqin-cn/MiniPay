package com.minipay.cart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minipay.cart.model.CartItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CartItemMapper extends BaseMapper<CartItem> {
}
