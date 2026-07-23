package com.minipay.cart.controller;

import com.minipay.cart.dto.CartItemReq;
import com.minipay.cart.model.CartItem;
import com.minipay.cart.service.CartService;
import com.minipay.common.resp.CommonResp;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    @Resource
    private CartService cartService;

    @GetMapping
    public CommonResp<List<CartItem>> listCart() { return new CommonResp<>(200, "TODO", cartService.listCart(), true); }

    @PostMapping("/items")
    public CommonResp<CartItem> addItem(@RequestBody CartItemReq req) { return new CommonResp<>(200, "TODO", cartService.addItem(req), true); }

    @PutMapping("/items/{id}")
    public CommonResp<CartItem> updateItem(@PathVariable Long id, @RequestBody CartItemReq req) { return new CommonResp<>(200, "TODO", cartService.updateItem(id, req), true); }

    @DeleteMapping("/items/{id}")
    public CommonResp<Void> deleteItem(@PathVariable Long id) { cartService.deleteItem(id); return new CommonResp<>(200, "TODO", null, true); }

    @PutMapping("/items/{id}/selected")
    public CommonResp<CartItem> updateSelected(@PathVariable Long id, @RequestBody CartItemReq req) { return new CommonResp<>(200, "TODO", cartService.updateSelected(id, req.getSelected()), true); }

    @DeleteMapping("/selected")
    public CommonResp<Void> deleteSelected() { cartService.deleteSelected(); return new CommonResp<>(200, "TODO", null, true); }

    @GetMapping("/health")
    public CommonResp<String> health() { return new CommonResp<>(200, "success", "cart-service is running", true); }
}
