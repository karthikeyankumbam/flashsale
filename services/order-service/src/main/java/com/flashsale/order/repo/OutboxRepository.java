package com.flashsale.order.repo;

import com.flashsale.order.outbox.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEventEntity, UUID> {}