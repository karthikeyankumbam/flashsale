package com.flashsale.cart.service;

import com.flashsale.cart.api.dto.CartItemResponse;
import com.flashsale.cart.api.dto.CartResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class CartService {

    private static final Duration CART_TTL = Duration.ofDays(7);
    private final StringRedisTemplate redis;

    public CartService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    private String key(String userId) {
        return "cart:" + userId;
    }

    public CartResponse getCart(String userId) {
        Map<Object, Object> entries = redis.opsForHash().entries(key(userId));

        List<CartItemResponse> items = entries.entrySet().stream()
                .map(e -> new CartItemResponse(String.valueOf(e.getKey()), Integer.parseInt(String.valueOf(e.getValue()))))
                .sorted(Comparator.comparing(CartItemResponse::sku))
                .toList();

        return new CartResponse(userId, items);
    }

    public CartResponse addItem(String userId, String sku, int qty) {
        String k = key(userId);
        // atomic: increment qty if exists else create
        redis.opsForHash().increment(k, sku, qty);
        redis.expire(k, CART_TTL);
        return getCart(userId);
    }

    public CartResponse setQty(String userId, String sku, int qty) {
        if (qty <= 0) throw new IllegalArgumentException("qty must be >= 1");
        String k = key(userId);
        redis.opsForHash().put(k, sku, String.valueOf(qty));
        redis.expire(k, CART_TTL);
        return getCart(userId);
    }

    public CartResponse removeItem(String userId, String sku) {
        redis.opsForHash().delete(key(userId), sku);
        return getCart(userId);
    }

    public void clearCart(String userId) {
        redis.delete(key(userId));
    }
}