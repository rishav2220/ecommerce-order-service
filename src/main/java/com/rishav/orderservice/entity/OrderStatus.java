package com.rishav.orderservice.entity;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PAYMENT_PROCESSING,
    PAYMENT_FAILED,
    PAID,
    PREPARING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REFUNDED;

    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case PENDING -> target == CONFIRMED || target == CANCELLED;
            case CONFIRMED -> target == PAYMENT_PROCESSING || target == CANCELLED;
            case PAYMENT_PROCESSING -> target == PAID || target == PAYMENT_FAILED;
            case PAYMENT_FAILED -> target == PAYMENT_PROCESSING || target == CANCELLED;
            case PAID -> target == PREPARING || target == CANCELLED;
            case PREPARING -> target == SHIPPED;
            case SHIPPED -> target == DELIVERED;
            case DELIVERED -> target == REFUNDED;
            default -> false;
        };
    }
}
