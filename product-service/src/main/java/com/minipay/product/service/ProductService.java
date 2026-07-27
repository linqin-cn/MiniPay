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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProductService {
    private static final Logger LOG = LoggerFactory.getLogger(ProductService.class);

    @Resource
    private ProductMapper productMapper;

    @Resource
    private ProductSkuMapper productSkuMapper;

    /**
     * 查询商品列表
     * @param req 查询请求
     * @return 商品列表
     */
    public List<Product> listProducts(ProductQueryReq req) {
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        //  按商品分类筛选 -- 如果存在CategoryId
        if (req.getCategoryId() != null) {
            queryWrapper.eq(Product::getCategoryId, req.getCategoryId());
        }
        // 商家后台按当前商家过滤，避免商家之间互相看到商品
        if (req.getMerchantId() != null) {
            queryWrapper.eq(Product::getMerchantId, req.getMerchantId());
        }
        // 按关键词模糊搜索
        if (req.getKeyword() != null && !req.getKeyword().isEmpty()) {
            queryWrapper.like(Product::getTitle, req.getKeyword());
        }
        if (req.getStatus() != null && !req.getStatus().isEmpty()) {
            queryWrapper.eq(Product::getStatus, req.getStatus());
        }
        // 按创建时间倒序
        queryWrapper.orderByDesc(Product::getCreatedAt);
        Page<Product> page = new Page<>(req.getPageNo(), req.getPageSize());
        Page<Product> result = productMapper.selectPage(page, queryWrapper);
        return result.getRecords();
    }

    /**
     * 根据 ID 查询商品
     * @param id 商品 ID
     * @return 商品信息
     */
    public Product getProduct(Long id) {
        return productMapper.selectById(id);
    }

    /**
     * 查询商品 SKU 列表
     * @param productId 商品 ID
     * @return SKU 列表
     */
    public List<ProductSku> listSkus(Long productId) {
        List<ProductSku> productSkus = productSkuMapper.selectList(
                new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, productId)
        );
        return productSkus;
    }

    public ProductSku getSku(Long skuId) {
        return productSkuMapper.selectById(skuId);
    }

    public ProductSku createSku(Long productId, ProductCreateReq req) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            return null;
        }
        if (req == null || req.getPrice() == null) {
            throw new IllegalArgumentException("SKU 价格不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        ProductSku sku = new ProductSku();
        sku.setProductId(productId);
        sku.setSkuName(isBlank(req.getSkuName()) ? "默认规格" : req.getSkuName());
        sku.setPrice(req.getPrice());
        sku.setOriginalPrice(req.getOriginalPrice() == null ? req.getPrice() : req.getOriginalPrice());
        sku.setStatus(ProductStatus.ON_SALE.name());
        sku.setCreatedAt(now);
        sku.setUpdatedAt(now);
        productSkuMapper.insert(sku);
        product.setUpdatedAt(now);
        productMapper.updateById(product);
        return sku;
    }

    public ProductSku updateSku(Long skuId, ProductCreateReq req) {
        ProductSku sku = productSkuMapper.selectById(skuId);
        if (sku == null) {
            return null;
        }
        if (req != null && !isBlank(req.getSkuName())) {
            sku.setSkuName(req.getSkuName());
        }
        if (req != null && req.getPrice() != null) {
            sku.setPrice(req.getPrice());
        }
        if (req != null && req.getOriginalPrice() != null) {
            sku.setOriginalPrice(req.getOriginalPrice());
        }
        sku.setUpdatedAt(LocalDateTime.now());
        productSkuMapper.updateById(sku);
        return sku;
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
        saveDefaultSku(product.getId(), req);
        return product;
    }

    public Product updateProduct(Long id, ProductCreateReq req) {
        LOG.info("更新商品, id: {}", id);
        Product product = productMapper.selectById(id);
        if (product == null) {
            return null;
        }
        product.setCategoryId(req.getCategoryId());
        product.setTitle(req.getTitle());
        product.setDescription(req.getDescription());
        product.setMainImage(req.getMainImage());
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);
        saveDefaultSku(product.getId(), req);
        return product;
    }

    public Product onSale(Long id) {
        LOG.info("商品上架, id: {}", id);
        Product product = productMapper.selectById(id);
        if (product == null) {
            return null;
        }
        // 更新状态为上架
        product.setStatus(ProductStatus.ON_SALE.name());
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);
        return product;
    }

    /**
     * 商品下架
     * @param id 商品 ID
     * @return 商品信息
     */
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

    public Product archive(Long id) {
        LOG.info("商品归档, id: {}", id);
        Product product = productMapper.selectById(id);
        if (product == null) {
            return null;
        }
        if (!ProductStatus.OFF_SALE.name().equals(product.getStatus())) {
            throw new IllegalStateException("商品必须先下架才能归档");
        }
        product.setStatus(ProductStatus.ARCHIVED.name());
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);
        return product;
    }

    public Product deleteProduct(Long id) {
        LOG.info("商品删除, id: {}", id);
        Product product = productMapper.selectById(id);
        if (product == null) {
            return null;
        }
        if (!ProductStatus.ARCHIVED.name().equals(product.getStatus())) {
            throw new IllegalStateException("商品必须先归档才能删除");
        }
        product.setStatus(ProductStatus.DELETED.name());
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);
        return product;
    }

    public Product restoreArchived(Long id) {
        LOG.info("恢复归档商品, id: {}", id);
        Product product = productMapper.selectById(id);
        if (product == null) {
            return null;
        }
        if (!ProductStatus.ARCHIVED.name().equals(product.getStatus())) {
            throw new IllegalStateException("只有归档商品可以恢复");
        }
        product.setStatus(ProductStatus.OFF_SALE.name());
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);
        return product;
    }

    /**
     * 保存默认 SKU
     * @param productId 商品 ID
     * @param req 创建请求
     */
    private void saveDefaultSku(Long productId, ProductCreateReq req) {
        if (productId == null || req.getPrice() == null) {
            return;
        }
        ProductSku sku = productSkuMapper.selectOne(
                new LambdaQueryWrapper<ProductSku>()
                        .eq(ProductSku::getProductId, productId)
                        .orderByAsc(ProductSku::getId)
                        .last("limit 1")
        );
        LocalDateTime now = LocalDateTime.now();
        if (sku == null) {
            sku = new ProductSku();
            sku.setProductId(productId);
            sku.setCreatedAt(now);
            sku.setStatus(ProductStatus.ON_SALE.name());
        }
        sku.setSkuName(isBlank(req.getSkuName()) ? "默认规格" : req.getSkuName());
        sku.setPrice(req.getPrice());
        sku.setOriginalPrice(req.getOriginalPrice() == null ? req.getPrice() : req.getOriginalPrice());
        sku.setUpdatedAt(now);
        // sku中id为空，表示是新增操作，否则是更新操作
        if (sku.getId() == null) {
            productSkuMapper.insert(sku);
        } else {
            productSkuMapper.updateById(sku);
        }
    }

    /**
     * 判断字符串是否为空
     * @param value 字符串
     * @return true 表示为空，false 表示不为空
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
