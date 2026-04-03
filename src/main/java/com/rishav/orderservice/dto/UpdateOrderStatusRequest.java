package com.rishav.orderservice.dto;

import com.rishav.orderservice.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateOrderStatusRequest {

    @NotNull(message = "Target status is required")
    private OrderStatus status;

    private String reason;
}
