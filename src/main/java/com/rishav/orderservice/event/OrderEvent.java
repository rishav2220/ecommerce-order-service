package com.rishav.orderservice.event;

import com.rishav.orderservice.entity.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class OrderEvent {

    private String eventId;
    private String eventType;
    private UUID orderId;
    private UUID customerId;
    private OrderStatus currentStatus;
    private OrderStatus previousStatus;
    private BigDecimal totalAmount;
    private String currency;
    private Instant occurredAt;
}
