package com.flashsale.catalog.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductRepository extends MongoRepository<Product, String> {
    Page<Product> findByActiveTrue(Pageable pageable);
    Page<Product> findByActiveTrueAndCategoryIgnoreCase(String category, Pageable pageable);
    Page<Product> findByActiveTrueAndNameContainingIgnoreCase(String query, Pageable pageable);
    Page<Product> findByActiveTrueAndCategoryIgnoreCaseAndNameContainingIgnoreCase(String category, String query, Pageable pageable);
}