package com.flashsale.inventory.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<ReservationEntity, UUID> {
    Optional<ReservationEntity> findByOrderId(UUID orderId);
}