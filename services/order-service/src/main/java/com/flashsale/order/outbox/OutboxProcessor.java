package com.flashsale.order.outbox;

import com.flashsale.order.events.OrderCreatedEvent;
import com.flashsale.order.repo.OutboxPollingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
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
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutboxProcessor(OutboxPollingRepository pollingRepo,
                           KafkaTemplate<String, Object> kafkaTemplate,
                           ObjectMapper objectMapper) {
        this.pollingRepo = pollingRepo;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "2000")
    @Transactional
    public void process() {
        List<OutboxEventEntity> batch = pollingRepo.findNextBatch(25);
        if (batch.isEmpty()) return;

        for (OutboxEventEntity e : batch) {
            try {
                if ("OrderCreated".equals(e.getEventType())) {
                    OrderCreatedEvent event = objectMapper.readValue(e.getPayloadJson(), OrderCreatedEvent.class);
                    kafkaTemplate.send("flashsale.order.events", event.orderId(), event);
                    log.info("OUTBOX published OrderCreated to Kafka: orderId={}", event.orderId());
                    e.markSent();
                } else {
                    // other events later
                    log.info("OUTBOX skipping unsupported eventType={}", e.getEventType());
                    e.markSent();
                }
            } catch (Exception ex) {
                e.incrementAttempt(ex.getMessage());
                e.markFailed(ex.getMessage());
                log.error("OUTBOX publish failed: id={}, err={}", e.getId(), ex.getMessage(), ex);
            }
        }
    }
}