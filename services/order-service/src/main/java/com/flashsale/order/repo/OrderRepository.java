package com.flashsale.order.repo;

import com.flashsale.order.domain.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {
    Optional<OrderEntity> findByUserIdAndIdempotencyKey(String userId, String idempotencyKey);
    List<OrderEntity> findByUserIdOrderByCreatedAtDesc(String userId);
}