package com.flashsale.catalog.product;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(String sku) { super("Product not found: " + sku); }
}
