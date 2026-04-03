package com.rishav.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rishav.orderservice.dto.CreateOrderRequest;
import com.rishav.orderservice.dto.OrderResponse;
import com.rishav.orderservice.entity.OrderStatus;
import com.rishav.orderservice.exception.OrderNotFoundException;
import com.rishav.orderservice.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@DisplayName("OrderController Integration Tests")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @Test
    @WithMockUser
    @DisplayName("POST /api/v1/orders - Should create order and return 201")
    void shouldCreateOrder() throws Exception {
        UUID customerId = UUID.randomUUID();
        CreateOrderRequest request = buildCreateRequest(customerId);

        OrderResponse mockResponse = OrderResponse.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("1299.99"))
                .currency("USD")
                .build();

        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.customerId").value(customerId.toString()));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/v1/orders/{id} - Should return 404 for missing order")
    void shouldReturn404ForMissingOrder() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.getOrderById(orderId)).thenThrow(new OrderNotFoundException(orderId));

        mockMvc.perform(get("/api/v1/orders/" + orderId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/orders - Should return 401 for unauthenticated request")
    void shouldReturn401ForUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    private CreateOrderRequest buildCreateRequest(UUID customerId) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(customerId);
        request.setCurrency("USD");
        request.setShippingAddress("123 Main St, Springfield, IL 62701");
        request.setPaymentMethod("CREDIT_CARD");

        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setProductId(UUID.randomUUID());
        item.setProductName("Laptop Pro X");
        item.setSku("LAPTOP-PRO-X-001");
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("1299.99"));
        item.setDiscountAmount(BigDecimal.ZERO);

        request.setItems(List.of(item));
        return request;
    }
}
