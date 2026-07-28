package com.minipay.cart.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minipay.common.util.JwtUtil;
import com.minipay.cart.dto.CartItemReq;
import com.minipay.cart.mapper.CartItemMapper;
import com.minipay.cart.model.CartItem;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CartService {
    @Resource
    private CartItemMapper cartItemMapper;

    @Resource
    private HttpServletRequest request;

    // 获取购物车商品列表
    @Cacheable(cacheNames = "cart:list", key = "#root.target.currentUserId()")
    public List<CartItem> listCart() {
        LambdaQueryWrapper<CartItem> wrapper = new LambdaQueryWrapper<>();
        // 根据购物车商品表的用户id查询商品，不同用户返回对应的购物车商品列表，按加入购物车时间排序
        wrapper.eq(CartItem::getUserId, currentUserId()).orderByAsc(CartItem::getUpdatedAt);
        return cartItemMapper.selectList(wrapper);
    }

    // 购物车加入商品
    @CacheEvict(cacheNames = "cart:list", key = "#root.target.currentUserId()")
    public CartItem addItem(CartItemReq req) {
        // 校验参数是否正确
        validateItem(req);
        // 使用mybatis-plus的LambdaQueryWrapper来构建查询条件，查询当前用户购物车中是否已经存在该商品的skuId
        LambdaQueryWrapper<CartItem> wrapper = new LambdaQueryWrapper<>();
        Long userId = currentUserId();
        wrapper.eq(CartItem::getUserId, userId).eq(CartItem::getSkuId, req.getSkuId());
        // 如果已经存在，则更新数量和选中状态，否则新增一条购物车商品记录
        CartItem existing = cartItemMapper.selectOne(wrapper);
        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            // 更新数量和选中状态，数量累加，选中状态取请求参数的值，如果请求参数为空则默认选中
            existing.setQuantity(existing.getQuantity() + req.getQuantity());
            existing.setSelected(req.getSelected() == null || req.getSelected());
            existing.setUpdatedAt(now);
            cartItemMapper.updateById(existing);
            return existing;
        }
        // 如果原购物车中不存在，则加入数据库
        CartItem item = new CartItem();
        item.setUserId(userId);
        item.setProductId(req.getProductId());
        item.setSkuId(req.getSkuId());
        item.setQuantity(req.getQuantity());
        item.setSelected(req.getSelected() == null || req.getSelected());
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        // mybatis-plus的insert方法会将自增主键回填到实体对象中，所以返回的item对象中会包含数据库生成的id
        cartItemMapper.insert(item);
        return item;
    }

    // 更新购物车
    @CacheEvict(cacheNames = "cart:list", key = "#root.target.currentUserId()")
    public CartItem updateItem(Long id, CartItemReq req) {
        CartItem item = requireItem(id);
        // 如果不为空，写入数据库
        if (req.getProductId() != null) {
            item.setProductId(req.getProductId());
        }
        if (req.getSkuId() != null) {
            item.setSkuId(req.getSkuId());
        }
        if (req.getQuantity() != null) {
            if (req.getQuantity() <= 0) {
                throw new IllegalArgumentException("商品数量必须大于0");
            }
            item.setQuantity(req.getQuantity());
        }
        if (req.getSelected() != null) {
            item.setSelected(req.getSelected());
        }
        item.setUpdatedAt(LocalDateTime.now());
        cartItemMapper.updateById(item);
        return item;
    }

    // 单删某商品
    @CacheEvict(cacheNames = "cart:list", key = "#root.target.currentUserId()")
    public void deleteItem(Long id) {
        cartItemMapper.deleteById(id);
    }

    // 更新数据库中购物车商品的选中状态是否为True，写入数据库
    @CacheEvict(cacheNames = "cart:list", key = "#root.target.currentUserId()")
    public CartItem updateSelected(Long id, Boolean selected) {
        CartItem item = requireItem(id);
        // 不用selected == true，因为selected可能为null，Boolean.TRUE.equals(selected)可以避免空指针异常
        item.setSelected(Boolean.TRUE.equals(selected));
        item.setUpdatedAt(LocalDateTime.now());
        cartItemMapper.updateById(item);
        return item;
    }

    // 删除选中
    @CacheEvict(cacheNames = "cart:list", key = "#root.target.currentUserId()")
    public void deleteSelected() {
        LambdaQueryWrapper<CartItem> wrapper = new LambdaQueryWrapper<>();
        // where条件:用户id等于当前用户id，且选中状态为true
        wrapper.eq(CartItem::getUserId, currentUserId()).eq(CartItem::getSelected, true);
        cartItemMapper.delete(wrapper);
    }

    // 从数据库获取该id的商品信息
    private CartItem requireItem(Long id) {
        CartItem item = cartItemMapper.selectById(id);
        if (item == null) {
            throw new IllegalArgumentException("购物车商品不存在");
        }
        return item;
    }

    // 验证购物车商品请求参数:商品id、skuId、数量不能为空，且数量必须大于0
    private void validateItem(CartItemReq req) {
        if (req == null || req.getProductId() == null || req.getSkuId() == null || req.getQuantity() == null || req.getQuantity() <= 0) {
            throw new IllegalArgumentException("商品、SKU 和数量不能为空，且数量必须大于0");
        }
    }

    // 从请求头中获取用户id, 如果token为空则直接赋值userId为空，如果token有内容就调用JWT方法解析出userId，如果解析出来的userId为空则直接赋值为1L
    public Long currentUserId() {
        String token = request.getHeader("token");
        Long userId = token == null || token.isEmpty() ? null : JwtUtil.getUserId(token);
        return userId == null ? 1L : userId;
    }
}
