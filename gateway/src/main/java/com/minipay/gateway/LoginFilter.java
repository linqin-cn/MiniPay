package com.minipay.gateway;

import com.minipay.gateway.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class LoginFilter implements GlobalFilter, Ordered {

    private static final Logger LOG = LoggerFactory.getLogger(LoginFilter.class);

    private static final List<String> WHITE_LIST = Arrays.asList(
            "/admin",
            "/api/login",
            "/api/users/login",
            "/api/users/register",
            "/api/products",
            "/api/payments/health",
            "/api/orders/health"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        
        // 检查是否在白名单中
        for (String whitePath : WHITE_LIST) {
            if (path.startsWith(whitePath)) {
                LOG.info("该请求不需要登录验证：{}", path);
                return chain.filter(exchange);
            }
        }
        
        // 获取当前token
        String token = exchange.getRequest().getHeaders().getFirst("token");
        LOG.info("当前请求的token为：{}", token);
        
        // 校验token
        if (token == null || token.isEmpty()) {
            LOG.info("token为空，请求被拦截");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        
        boolean validate = JwtUtil.validate(token);
        if (!validate) {
            LOG.info("token无效，请求被拦截");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        
        LOG.info("token有效，请求继续");
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
