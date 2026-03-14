package com.flashsale.cart.api;

import com.flashsale.cart.api.dto.CartResponse;
import com.flashsale.cart.api.dto.UpsertCartItemRequest;
import com.flashsale.cart.service.CartService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/{userId}")
    public CartResponse get(@PathVariable String userId) {
        return cartService.getCart(userId);
    }

    @PostMapping("/{userId}/items")
    public CartResponse add(@PathVariable String userId, @Valid @RequestBody UpsertCartItemRequest req) {
        return cartService.addItem(userId, req.sku(), req.qty());
    }

    @PutMapping("/{userId}/items/{sku}")
    public CartResponse setQty(
            @PathVariable String userId,
            @PathVariable String sku,
            @RequestParam int qty
    ) {
        return cartService.setQty(userId, sku, qty);
    }

    @DeleteMapping("/{userId}/items/{sku}")
    public CartResponse remove(@PathVariable String userId, @PathVariable String sku) {
        return cartService.removeItem(userId, sku);
    }

    @DeleteMapping("/{userId}")
    public void clear(@PathVariable String userId) {
        cartService.clearCart(userId);
    }
}