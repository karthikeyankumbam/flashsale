package com.flashsale.cart.api;

import com.flashsale.cart.api.dto.CartResponse;
import com.flashsale.cart.api.dto.UpsertCartItemRequest;
import com.flashsale.cart.service.CartService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Shopping cart")
@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @Operation(summary = "View a user cart")
    @GetMapping("/{userId}")
    public CartResponse get(@PathVariable String userId) {
        return cartService.getCart(userId);
    }

    @Operation(summary = "Add an item", description = "Increments the quantity if the SKU is already in the cart.")
    @PostMapping("/{userId}/items")
    public CartResponse add(@PathVariable String userId, @Valid @RequestBody UpsertCartItemRequest req) {
        return cartService.addItem(userId, req.sku(), req.qty());
    }

    @Operation(summary = "Set the item quantity")
    @PutMapping("/{userId}/items/{sku}")
    public CartResponse setQty(
            @PathVariable String userId,
            @PathVariable String sku,
            @RequestParam int qty
    ) {
        return cartService.setQty(userId, sku, qty);
    }

    @Operation(summary = "Remove one SKU from the cart")
    @DeleteMapping("/{userId}/items/{sku}")
    public CartResponse remove(@PathVariable String userId, @PathVariable String sku) {
        return cartService.removeItem(userId, sku);
    }

    @Operation(summary = "Clear the cart")
    @DeleteMapping("/{userId}")
    public void clear(@PathVariable String userId) {
        cartService.clearCart(userId);
    }
}
