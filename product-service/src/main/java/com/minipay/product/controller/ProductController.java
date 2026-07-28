package com.minipay.product.controller;

import com.minipay.common.resp.CommonResp;
import com.minipay.product.dto.ProductCreateReq;
import com.minipay.product.dto.ProductQueryReq;
import com.minipay.product.model.Product;
import com.minipay.product.model.ProductSku;
import com.minipay.product.service.ProductService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    @Resource
    private ProductService productService;

    @Value("${minipay.upload.product-image-dir:uploads/products}")
    private String productImageDir;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

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

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResp<Map<String, String>> uploadProductImage(@RequestPart("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return new CommonResp<>(400, "请选择要上传的商品图片", null, false);
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            return new CommonResp<>(400, "只支持 jpg、png、webp、gif 图片", null, false);
        }
        Path uploadDir = Paths.get(productImageDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);
        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "product-image" : file.getOriginalFilename());
        String extension = getExtension(originalName, contentType);
        String fileName = UUID.randomUUID().toString().replace("-", "") + extension;
        Path target = uploadDir.resolve(fileName).normalize();
        if (!target.startsWith(uploadDir)) {
            return new CommonResp<>(400, "图片文件名不合法", null, false);
        }
        file.transferTo(target);
        String imageUrl = "/api/products/images/" + fileName;
        return new CommonResp<>(200, "商品图片上传成功", Map.of("url", imageUrl), true);
    }

    @GetMapping("/images/{fileName:.+}")
    public ResponseEntity<byte[]> getProductImage(@PathVariable String fileName) throws IOException {
        Path uploadDir = Paths.get(productImageDir).toAbsolutePath().normalize();
        Path image = uploadDir.resolve(StringUtils.cleanPath(fileName)).normalize();
        if (!image.startsWith(uploadDir) || !Files.exists(image) || Files.isDirectory(image)) {
            return ResponseEntity.notFound().build();
        }
        String contentType = Files.probeContentType(image);
        MediaType mediaType = contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType);
        return ResponseEntity.ok().contentType(mediaType).body(Files.readAllBytes(image));
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

    private String getExtension(String fileName, String contentType) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < fileName.length() - 1) {
            String ext = fileName.substring(dotIndex).toLowerCase();
            if (Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif").contains(ext)) return ext;
        }
        return switch (contentType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }
}
