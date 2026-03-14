package com.flashsale.cart.api.dto;

import java.util.List;

public record CartResponse(String userId, List<CartItemResponse> items) {}