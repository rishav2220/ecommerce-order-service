package com.rishav.orderservice.event;

import com.rishav.orderservice.entity.Order;
import com.rishav.orderservice.entity.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @Value("${app.kafka.topics.order-events}")
    private String orderEventsTopic;

    public void publishOrderCreated(Order order) {
        OrderEvent event = OrderEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("ORDER_CREATED")
                .orderId(order.getId())
                .customerId(order.getCustomerId())
                .currentStatus(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .occurredAt(Instant.now())
                .build();

        publish(event);
    }

    public void publishOrderStatusChanged(Order order, OrderStatus previousStatus) {
        OrderEvent event = OrderEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("ORDER_STATUS_CHANGED")
                .orderId(order.getId())
                .customerId(order.getCustomerId())
                .currentStatus(order.getStatus())
                .previousStatus(previousStatus)
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .occurredAt(Instant.now())
                .build();

        publish(event);
    }

    private void publish(OrderEvent event) {
        CompletableFuture<SendResult<String, OrderEvent>> future =
                kafkaTemplate.send(orderEventsTopic, event.getOrderId().toString(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish event {} for order {}: {}",
                        event.getEventType(), event.getOrderId(), ex.getMessage());
            } else {
                log.debug("Published event {} for order {} to partition {} offset {}",
                        event.getEventType(),
                        event.getOrderId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
