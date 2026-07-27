package com.minipay.product.controller;

import com.minipay.common.resp.CommonResp;
import com.minipay.product.dto.ProductCreateReq;
import com.minipay.product.dto.ProductQueryReq;
import com.minipay.product.model.Product;
import com.minipay.product.model.ProductSku;
import com.minipay.product.service.ProductService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    @Resource
    private ProductService productService;

    @GetMapping
    public CommonResp<List<Product>> listProducts(ProductQueryReq req) {
        return new CommonResp<>(200, "商品列表查询成功", productService.listProducts(req), true);
    }

    @GetMapping("/{id}")
    public CommonResp<Product> getProduct(@PathVariable Long id) {
        return new CommonResp<>(200, "商品查询成功", productService.getProduct(id), true);
    }

    @GetMapping("/{id}/skus")
    public CommonResp<List<ProductSku>> listSkus(@PathVariable Long id) {
        return new CommonResp<>(200, "商品SKU列表查询成功", productService.listSkus(id), true);
    }

    @GetMapping("/skus/{skuId}")
    public CommonResp<ProductSku> getSku(@PathVariable Long skuId) {
        ProductSku sku = productService.getSku(skuId);
        if (sku == null) {
            return new CommonResp<>(404, "商品SKU不存在", null, false);
        }
        return new CommonResp<>(200, "商品SKU查询成功", sku, true);
    }

    @PostMapping("/{id}/skus")
    public CommonResp<ProductSku> createSku(@PathVariable Long id, @RequestBody ProductCreateReq req) {
        ProductSku sku = productService.createSku(id, req);
        if (sku == null) {
            return new CommonResp<>(404, "商品不存在", null, false);
        }
        return new CommonResp<>(200, "商品SKU创建成功", sku, true);
    }

    @PutMapping("/skus/{skuId}")
    public CommonResp<ProductSku> updateSku(@PathVariable Long skuId, @RequestBody ProductCreateReq req) {
        ProductSku sku = productService.updateSku(skuId, req);
        if (sku == null) {
            return new CommonResp<>(404, "商品SKU不存在", null, false);
        }
        return new CommonResp<>(200, "商品SKU更新成功", sku, true);
    }

    @PostMapping
    public CommonResp<Product> createProduct(@RequestBody ProductCreateReq req) {
        return new CommonResp<>(200, "商品创建成功", productService.createProduct(req), true);
    }

    @PutMapping("/{id}")
    public CommonResp<Product> updateProduct(@PathVariable Long id, @RequestBody ProductCreateReq req) {
        return new CommonResp<>(200, "商品更新成功", productService.updateProduct(id, req), true);
    }

    @PutMapping("/{id}/on-sale")
    public CommonResp<Product> onSale(@PathVariable Long id) {
        return new CommonResp<>(200, "商品上架成功", productService.onSale(id), true);
    }

    @PutMapping("/{id}/off-sale")
    public CommonResp<Product> offSale(@PathVariable Long id) {
        return new CommonResp<>(200, "商品下架成功", productService.offSale(id), true);
    }

    @PutMapping("/{id}/archive")
    public CommonResp<Product> archive(@PathVariable Long id) {
        return new CommonResp<>(200, "商品归档成功", productService.archive(id), true);
    }

    @PutMapping("/{id}/restore")
    public CommonResp<Product> restoreArchived(@PathVariable Long id) {
        return new CommonResp<>(200, "商品恢复成功", productService.restoreArchived(id), true);
    }

    @DeleteMapping("/{id}")
    public CommonResp<Product> deleteProduct(@PathVariable Long id) {
        return new CommonResp<>(200, "商品删除成功", productService.deleteProduct(id), true);
    }
}
