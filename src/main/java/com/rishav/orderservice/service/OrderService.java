package com.rishav.orderservice.service;

import com.rishav.orderservice.dto.CreateOrderRequest;
import com.rishav.orderservice.dto.OrderResponse;
import com.rishav.orderservice.dto.PagedResponse;
import com.rishav.orderservice.dto.UpdateOrderStatusRequest;
import com.rishav.orderservice.entity.OrderStatus;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request);

    OrderResponse getOrderById(UUID orderId);

    PagedResponse<OrderResponse> getOrdersByCustomer(UUID customerId, Pageable pageable);

    PagedResponse<OrderResponse> getOrdersByStatus(OrderStatus status, Pageable pageable);

    OrderResponse updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request);

    void cancelOrder(UUID orderId, String reason);
}
