package com.minipay.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minipay.merchant.model.Merchant;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MerchantMapper extends BaseMapper<Merchant> {
}
