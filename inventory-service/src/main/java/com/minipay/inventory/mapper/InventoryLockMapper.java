package com.minipay.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minipay.inventory.model.InventoryLock;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InventoryLockMapper extends BaseMapper<InventoryLock> {
}
