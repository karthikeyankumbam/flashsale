package com.flashsale.order.kafka;

import com.flashsale.order.events.InventoryResultEvent;
import com.flashsale.order.events.PaymentResultEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    private Map<String, Object> baseProps(String groupId) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }

    // ---------- InventoryResult consumer ----------
    @Bean
    public ConsumerFactory<String, InventoryResultEvent> inventoryResultConsumerFactory() {
        JsonDeserializer<InventoryResultEvent> valueDeserializer =
                new JsonDeserializer<>(InventoryResultEvent.class, false);
        valueDeserializer.addTrustedPackages("com.flashsale.order.events", "com.flashsale.inventory.events");

        return new DefaultKafkaConsumerFactory<>(
                baseProps("order-service"),
                new StringDeserializer(),
                valueDeserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, InventoryResultEvent>
    inventoryResultKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, InventoryResultEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(inventoryResultConsumerFactory());
        return factory;
    }

    // ---------- PaymentResult consumer ----------
    @Bean
    public ConsumerFactory<String, PaymentResultEvent> paymentResultConsumerFactory() {
        JsonDeserializer<PaymentResultEvent> valueDeserializer =
                new JsonDeserializer<>(PaymentResultEvent.class, false);
        valueDeserializer.addTrustedPackages("com.flashsale.order.events", "com.flashsale.payment.events");

        // Use a separate group to avoid old bad offsets (recommended)
        return new DefaultKafkaConsumerFactory<>(
                baseProps("order-service-pay-v2"),
                new StringDeserializer(),
                valueDeserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PaymentResultEvent>
    paymentResultKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, PaymentResultEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(paymentResultConsumerFactory());
        return factory;
    }
}