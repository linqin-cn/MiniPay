package com.minipay.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minipay.inventory.model.Inventory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InventoryMapper extends BaseMapper<Inventory> {
}
