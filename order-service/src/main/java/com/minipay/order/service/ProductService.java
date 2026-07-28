package com.minipay.order.service;

import com.minipay.common.resp.CommonResp;
import com.minipay.order.model.Product;
import com.minipay.order.model.ProductSku;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ProductService {
    @Value("${minipay.services.product-url:http://localhost:8084/api/products}")
    private String productServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 根据SKU ID查询SKU信息
     * @param skuId SKU ID
     * @return SKU信息
     */
    public ProductSku getSkuById(Long skuId) {
        if (skuId == null) {
            throw new IllegalArgumentException("skuId 不能为空");
        }
        ResponseEntity<CommonResp<ProductSku>> response = restTemplate.exchange(
                productServiceUrl + "/skus/" + skuId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<CommonResp<ProductSku>>() {}
        );
        CommonResp<ProductSku> body = response.getBody();
        if (body == null || !body.isSuccess() || body.getData() == null) {
            throw new IllegalArgumentException("SKU 不存在：" + skuId);
        }
        return body.getData();
    }

    /**
     * 根据产品ID查询产品信息
     * @param productId 产品ID
     * @return 产品信息
     */
    public Product getProductById(Long productId) {
        if (productId == null) {
            return null;
        }
        /*订单服务向商品服务发送一个 GET 请求，
            请求地址是 http://localhost:8084/api/products/{productId}，
            不带请求头，也不带请求体，
            希望返回的数据格式是 CommonResp<Product>，
            最后把完整 HTTP 响应保存到 response 里。
        */
        ResponseEntity<CommonResp<Product>> response = restTemplate.exchange(
                productServiceUrl + "/" + productId, // url
                HttpMethod.GET, //  请求方式
                null, // 请求体HttpEntity<?>，null = 无请求头、无请求Body
                new ParameterizedTypeReference<CommonResp<Product>>() {} //要接收的泛型返回类型
        );
        CommonResp<Product> body = response.getBody();
        return body == null || !body.isSuccess() ? null : body.getData();
    }
}
