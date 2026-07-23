package com.minipay.product.service;

import com.minipay.product.dto.ProductCreateReq;
import com.minipay.product.dto.ProductQueryReq;
import com.minipay.product.model.Product;
import com.minipay.product.model.ProductSku;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class ProductService {
    public List<Product> listProducts(ProductQueryReq req) {
        return Collections.emptyList();
    }

    public Product getProduct(Long id) {
        return null;
    }

    public List<ProductSku> listSkus(Long productId) {
        return Collections.emptyList();
    }

    public Product createProduct(ProductCreateReq req) {
        return null;
    }

    public Product updateProduct(Long id, ProductCreateReq req) {
        return null;
    }

    public Product onSale(Long id) {
        return null;
    }

    public Product offSale(Long id) {
        return null;
    }
}
