package com.minipay.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minipay.product.model.Product;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
