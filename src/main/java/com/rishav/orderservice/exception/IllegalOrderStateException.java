package com.rishav.orderservice.exception;

import com.rishav.orderservice.entity.OrderStatus;

public class IllegalOrderStateException extends RuntimeException {

    public IllegalOrderStateException(OrderStatus current, OrderStatus target) {
        super(String.format("Cannot transition order from %s to %s", current, target));
    }
}
