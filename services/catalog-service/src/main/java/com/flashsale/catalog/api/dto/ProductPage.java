package com.flashsale.catalog.api.dto;

import com.flashsale.catalog.product.Product;
import org.springframework.data.domain.Page;
import java.util.List;

/** Explicit response contract retaining the pagination fields used by the storefront. */
public record ProductPage(List<Product> content, int number, int size, long totalElements,
                          int totalPages, boolean first, boolean last, boolean empty,
                          int numberOfElements) {
    public static ProductPage from(Page<Product> page) {
        return new ProductPage(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast(),
                page.isEmpty(), page.getNumberOfElements());
    }
}
