package com.flashsale.order.repo;

import com.flashsale.order.outbox.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface OutboxPollingRepository extends JpaRepository<OutboxEventEntity, UUID> {

    @Query(value = """
      select * from outbox_events
      where status = 'NEW'
      order by created_at
      limit :limit
      """, nativeQuery = true)
    List<OutboxEventEntity> findNextBatch(int limit);
}