package com.flashsale.payment.kafka;

import com.flashsale.payment.domain.PaymentEntity;
import com.flashsale.payment.events.PaymentRequestedEvent;
import com.flashsale.payment.events.PaymentResultEvent;
import com.flashsale.payment.service.PaymentProcessor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentCommandsListener {

    private final PaymentProcessor processor;
    private final KafkaTemplate<String, PaymentResultEvent> kafkaTemplate;

    public PaymentCommandsListener(PaymentProcessor processor, KafkaTemplate<String, PaymentResultEvent> kafkaTemplate) {
        this.processor = processor;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(
            topics = "flashsale.payment.commands",
            groupId = "order-service-pay-v2",
            containerFactory = "paymentRequestedKafkaListenerContainerFactory"
    )
    public void onPaymentRequested(PaymentRequestedEvent req) {
        PaymentEntity payment = processor.process(req);
        String orderId = req.orderId();

        PaymentResultEvent result = new PaymentResultEvent(
                orderId,
                payment.getStatus(),
                payment.getFailureReason(),
                payment.getId().toString()
        );

        kafkaTemplate.send("flashsale.payment.events", orderId, result);
    }
}