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

    // 获取购物车商品
    @GetMapping
    public CommonResp<List<CartItem>> listCart() { return new CommonResp<>(200, "查询购物车成功", cartService.listCart(), true); }

    // 加入购物车
    @PostMapping("/items")
    public CommonResp<CartItem> addItem(@RequestBody CartItemReq req) { return new CommonResp<>(200, "加入购物车成功", cartService.addItem(req), true); }

    // 刷新购物车
    @PutMapping("/items/{id}")
    public CommonResp<CartItem> updateItem(@PathVariable Long id, @RequestBody CartItemReq req) { return new CommonResp<>(200, "更新购物车成功", cartService.updateItem(id, req), true); }

    // 删除购物车商品
    @DeleteMapping("/items/{id}")
    public CommonResp<Void> deleteItem(@PathVariable Long id) { cartService.deleteItem(id); return new CommonResp<>(200, "删除购物车商品成功", null, true); }

    // 更新选中状态
    @PutMapping("/items/{id}/selected")
    public CommonResp<CartItem> updateSelected(@PathVariable Long id, @RequestBody CartItemReq req) { return new CommonResp<>(200, "更新选中状态成功", cartService.updateSelected(id, req.getSelected()), true); }

    // 删除选中商品
    @DeleteMapping("/selected")
    public CommonResp<Void> deleteSelected() { cartService.deleteSelected(); return new CommonResp<>(200, "删除已选商品成功", null, true); }
}
