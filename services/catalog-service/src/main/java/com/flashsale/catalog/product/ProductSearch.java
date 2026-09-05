package com.flashsale.catalog.product;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.util.Locale;
import java.util.Set;

public record ProductSearch(String category, String query, String currency, Long minPrice,
                            Long maxPrice, int page, int size, String sort) {
    public ProductSearch {
        category = clean(category);
        query = clean(query);
        currency = clean(currency);
        if (currency != null) currency = currency.toUpperCase(Locale.ROOT);
        if (page < 0) throw new IllegalArgumentException("page must be zero or greater");
        if (size < 1 || size > 100) throw new IllegalArgumentException("size must be between 1 and 100");
        if ((minPrice != null && minPrice < 0) || (maxPrice != null && maxPrice < 0)) {
            throw new IllegalArgumentException("Price filters must be zero or greater");
        }
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new IllegalArgumentException("minPrice must not exceed maxPrice");
        }
        if (sort == null || !Set.of("name-asc", "name-desc", "price-asc", "price-desc", "newest", "updated").contains(sort)) {
            throw new IllegalArgumentException("sort must be name-asc, name-desc, price-asc, price-desc, newest or updated");
        }
    }

    public PageRequest pageable() {
        Sort ordering = switch (sort) {
            case "name-asc" -> Sort.by(Sort.Direction.ASC, "name");
            case "name-desc" -> Sort.by(Sort.Direction.DESC, "name");
            case "price-asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price-desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "updated" -> Sort.by(Sort.Direction.DESC, "updatedAt");
            default -> throw new IllegalArgumentException(
                    "sort must be name-asc, name-desc, price-asc, price-desc, newest or updated");
        };
        // A unique tie-breaker keeps products with equal sort values on predictable pages.
        return PageRequest.of(page, size, ordering.and(Sort.by("sku")));
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
