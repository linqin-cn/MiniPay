package com.minipay.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minipay.product.dto.ProductCreateReq;
import com.minipay.product.dto.ProductQueryReq;
import com.minipay.product.mapper.ProductMapper;
import com.minipay.product.mapper.ProductSkuMapper;
import com.minipay.product.model.Product;
import com.minipay.product.model.ProductSku;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import com.minipay.common.enums.ProductStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProductService {
    private static final Logger LOG = LoggerFactory.getLogger(ProductService.class);

    @Resource
    private ProductMapper productMapper;

    @Resource
    private ProductSkuMapper productSkuMapper;

    public List<Product> listProducts(ProductQueryReq req) {
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        //  按商品分类筛选 -- 如果存在CategoryId
        if (req.getCategoryId() != null) {
            queryWrapper.eq(Product::getCategoryId, req.getCategoryId());
        }
        // 按关键词模糊搜索
        if (req.getKeyword() != null && !req.getKeyword().isEmpty()) {
            queryWrapper.like(Product::getTitle, req.getKeyword());
        }
        // 按创建时间倒序
        queryWrapper.orderByDesc(Product::getCreatedAt);
        Page<Product> page = new Page<>(req.getPageNo(), req.getPageSize());
        Page<Product> result = productMapper.selectPage(page, queryWrapper);
        return result.getRecords();
    }

    public Product getProduct(Long id) {
        return productMapper.selectById(id);
    }

    public List<ProductSku> listSkus(Long productId) {
        List<ProductSku> productSkus = productSkuMapper.selectList(
                new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, productId)
        );
        return productSkus;
    }

    public Product createProduct(ProductCreateReq req) {
        LOG.info("创建商品, title: {}", req.getTitle());
        Product product = new Product();
        product.setMerchantId(req.getMerchantId());
        product.setCategoryId(req.getCategoryId());
        product.setTitle(req.getTitle());
        product.setDescription(req.getDescription());
        product.setMainImage(req.getMainImage());
        product.setStatus(ProductStatus.DRAFT.name());
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.insert(product);
        return product;
    }

    public Product updateProduct(Long id, ProductCreateReq req) {
        LOG.info("更新商品, id: {}", id);
        Product product = productMapper.selectById(id);
        if (product == null) {
            return null;
        }
        product.setMerchantId(req.getMerchantId());
        product.setCategoryId(req.getCategoryId());
        product.setTitle(req.getTitle());
        product.setDescription(req.getDescription());
        product.setMainImage(req.getMainImage());
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);
        return product;
    }

    public Product onSale(Long id) {
        LOG.info("商品上架, id: {}", id);
        Product product = productMapper.selectById(id);
        if (product == null) {
            return null;
        }
        product.setStatus(ProductStatus.ON_SALE.name());
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);
        return product;
    }

    public Product offSale(Long id) {
        LOG.info("商品下架, id: {}", id);
        Product product = productMapper.selectById(id);
        if (product == null) {
            return null;
        }
        product.setStatus(ProductStatus.OFF_SALE.name());
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);
        return product;
    }
}
