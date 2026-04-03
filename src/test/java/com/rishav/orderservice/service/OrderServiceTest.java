package com.rishav.orderservice.service;

import com.rishav.orderservice.cache.OrderCacheService;
import com.rishav.orderservice.dto.CreateOrderRequest;
import com.rishav.orderservice.dto.OrderResponse;
import com.rishav.orderservice.dto.UpdateOrderStatusRequest;
import com.rishav.orderservice.entity.Order;
import com.rishav.orderservice.entity.OrderStatus;
import com.rishav.orderservice.event.OrderEventPublisher;
import com.rishav.orderservice.exception.IllegalOrderStateException;
import com.rishav.orderservice.exception.OrderNotFoundException;
import com.rishav.orderservice.repository.OrderRepository;
import com.rishav.orderservice.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Unit Tests")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderCacheService orderCacheService;

    @Mock
    private OrderEventPublisher eventPublisher;

    @InjectMocks
    private OrderServiceImpl orderService;

    private UUID customerId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        orderId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should create order successfully")
    void shouldCreateOrderSuccessfully() {
        CreateOrderRequest request = buildCreateOrderRequest();

        Order savedOrder = buildOrder(orderId, customerId, OrderStatus.PENDING);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderCacheService.getCachedOrder(any())).thenReturn(Optional.empty());

        OrderResponse response = orderService.createOrder(request);

        assertThat(response).isNotNull();
        assertThat(response.getCustomerId()).isEqualTo(customerId);
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);

        verify(orderRepository).save(any(Order.class));
        verify(eventPublisher).publishOrderCreated(any(Order.class));
        verify(orderCacheService).cacheOrder(any(OrderResponse.class));
    }

    @Test
    @DisplayName("Should throw OrderNotFoundException when order does not exist")
    void shouldThrowWhenOrderNotFound() {
        when(orderCacheService.getCachedOrder(orderId)).thenReturn(Optional.empty());
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(orderId))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining(orderId.toString());
    }

    @Test
    @DisplayName("Should return cached order on cache hit")
    void shouldReturnCachedOrder() {
        OrderResponse cachedResponse = OrderResponse.builder()
                .id(orderId)
                .customerId(customerId)
                .status(OrderStatus.CONFIRMED)
                .build();

        when(orderCacheService.getCachedOrder(orderId)).thenReturn(Optional.of(cachedResponse));

        OrderResponse response = orderService.getOrderById(orderId);

        assertThat(response).isEqualTo(cachedResponse);
        verifyNoInteractions(orderRepository);
    }

    @Test
    @DisplayName("Should update order status and evict cache")
    void shouldUpdateOrderStatus() {
        Order order = buildOrder(orderId, customerId, OrderStatus.PENDING);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        UpdateOrderStatusRequest req = new UpdateOrderStatusRequest();
        req.setStatus(OrderStatus.CONFIRMED);

        OrderResponse response = orderService.updateOrderStatus(orderId, req);

        assertThat(response).isNotNull();
        verify(orderCacheService).evictOrder(orderId);
        verify(eventPublisher).publishOrderStatusChanged(any(Order.class), eq(OrderStatus.PENDING));
    }

    @Test
    @DisplayName("Should reject invalid status transitions")
    void shouldRejectInvalidStatusTransition() {
        Order order = buildOrder(orderId, customerId, OrderStatus.DELIVERED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        UpdateOrderStatusRequest req = new UpdateOrderStatusRequest();
        req.setStatus(OrderStatus.PENDING);

        assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, req))
                .isInstanceOf(IllegalOrderStateException.class);
    }

    private CreateOrderRequest buildCreateOrderRequest() {
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

    private Order buildOrder(UUID id, UUID customerId, OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setCustomerId(customerId);
        order.setStatus(status);
        order.setTotalAmount(new BigDecimal("1299.99"));
        order.setCurrency("USD");
        order.setShippingAddress("123 Main St");
        order.setPaymentMethod("CREDIT_CARD");
        order.setItems(new ArrayList<>());
        return order;
    }
}
