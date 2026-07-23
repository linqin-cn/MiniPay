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
        return new CommonResp<>(200, "TODO", productService.listProducts(req), true);
    }

    @GetMapping("/{id}")
    public CommonResp<Product> getProduct(@PathVariable Long id) {
        return new CommonResp<>(200, "TODO", productService.getProduct(id), true);
    }

    @GetMapping("/{id}/skus")
    public CommonResp<List<ProductSku>> listSkus(@PathVariable Long id) {
        return new CommonResp<>(200, "TODO", productService.listSkus(id), true);
    }

    @PostMapping
    public CommonResp<Product> createProduct(@RequestBody ProductCreateReq req) {
        return new CommonResp<>(200, "TODO", productService.createProduct(req), true);
    }

    @PutMapping("/{id}")
    public CommonResp<Product> updateProduct(@PathVariable Long id, @RequestBody ProductCreateReq req) {
        return new CommonResp<>(200, "TODO", productService.updateProduct(id, req), true);
    }

    @PutMapping("/{id}/on-sale")
    public CommonResp<Product> onSale(@PathVariable Long id) {
        return new CommonResp<>(200, "TODO", productService.onSale(id), true);
    }

    @PutMapping("/{id}/off-sale")
    public CommonResp<Product> offSale(@PathVariable Long id) {
        return new CommonResp<>(200, "TODO", productService.offSale(id), true);
    }

    @GetMapping("/health")
    public CommonResp<String> health() {
        return new CommonResp<>(200, "success", "product-service is running", true);
    }
}
