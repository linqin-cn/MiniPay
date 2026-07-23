package com.minipay.cart.service;

import com.minipay.cart.dto.CartItemReq;
import com.minipay.cart.model.CartItem;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class CartService {
    public List<CartItem> listCart() { return Collections.emptyList(); }
    public CartItem addItem(CartItemReq req) { return null; }
    public CartItem updateItem(Long id, CartItemReq req) { return null; }
    public void deleteItem(Long id) { }
    public CartItem updateSelected(Long id, Boolean selected) { return null; }
    public void deleteSelected() { }
}
