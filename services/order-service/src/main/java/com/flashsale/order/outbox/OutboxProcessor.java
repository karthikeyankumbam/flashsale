package com.flashsale.order.outbox;

import com.flashsale.order.repo.OutboxPollingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@EnableScheduling
@Component
public class OutboxProcessor {

    private static final Logger log = LoggerFactory.getLogger(OutboxProcessor.class);

    private final OutboxPollingRepository pollingRepo;

    public OutboxProcessor(OutboxPollingRepository pollingRepo) {
        this.pollingRepo = pollingRepo;
    }

    // every 2 seconds (dev)
    @Scheduled(fixedDelayString = "2000")
    @Transactional
    public void process() {
        List<OutboxEventEntity> batch = pollingRepo.findNextBatch(25);
        if (batch.isEmpty()) return;

        for (OutboxEventEntity e : batch) {
            try {
                // ✅ For now we "simulate publish"
                // Later: publish to Kafka and only then markSent()
                log.info("OUTBOX publish simulated: id={}, type={}, payload={}", e.getId(), e.getEventType(), e.getPayloadJson());

                e.markSent();
                // JPA dirty checking will persist changes
            } catch (Exception ex) {
                e.incrementAttempt(ex.getMessage());
                e.markFailed(ex.getMessage());
                log.error("OUTBOX failed: id={}, err={}", e.getId(), ex.getMessage(), ex);
            }
        }
    }
}