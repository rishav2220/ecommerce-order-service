package com.rishav.orderservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreateOrderRequest {

    @NotNull(message = "Customer ID is required")
    private UUID customerId;

    @NotEmpty(message = "Order must have at least one item")
    @Valid
    private List<OrderItemRequest> items;

    @NotBlank(message = "Shipping address is required")
    @Size(max = 500)
    private String shippingAddress;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid ISO 4217 code (e.g. USD, EUR)")
    private String currency;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    @Size(max = 1000)
    private String notes;

    @Data
    public static class OrderItemRequest {

        @NotNull(message = "Product ID is required")
        private UUID productId;

        @NotBlank(message = "Product name is required")
        private String productName;

        @NotBlank(message = "SKU is required")
        private String sku;

        @NotNull
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 9999)
        private Integer quantity;

        @NotNull
        @DecimalMin(value = "0.01", message = "Unit price must be greater than 0")
        private java.math.BigDecimal unitPrice;

        @DecimalMin("0.00")
        private java.math.BigDecimal discountAmount = java.math.BigDecimal.ZERO;
    }
}
