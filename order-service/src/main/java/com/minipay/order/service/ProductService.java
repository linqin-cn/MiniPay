package com.minipay.order.service;

import com.minipay.order.model.ProductSku;
import com.minipay.order.mapper.ProductSkuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductSkuMapper productSkuMapper;

    public ProductSku getSkuById(Long skuId) {
        if (skuId == null) {
            throw new IllegalArgumentException("skuId 不能为空");
        }

        ProductSku sku = productSkuMapper.selectById(skuId);

        if (sku == null) {
            throw new IllegalArgumentException("SKU 不存在：" + skuId);
        }

        return sku;
    }
}