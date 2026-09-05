package com.flashsale.catalog.product;

public class DuplicateSkuException extends RuntimeException {
    public DuplicateSkuException(String sku) { super("SKU already exists: " + sku); }
}
