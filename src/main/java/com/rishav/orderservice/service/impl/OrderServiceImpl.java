package com.rishav.orderservice.service.impl;

import com.rishav.orderservice.cache.OrderCacheService;
import com.rishav.orderservice.dto.CreateOrderRequest;
import com.rishav.orderservice.dto.OrderResponse;
import com.rishav.orderservice.dto.PagedResponse;
import com.rishav.orderservice.dto.UpdateOrderStatusRequest;
import com.rishav.orderservice.entity.Order;
import com.rishav.orderservice.entity.OrderItem;
import com.rishav.orderservice.entity.OrderStatus;
import com.rishav.orderservice.event.OrderEventPublisher;
import com.rishav.orderservice.exception.IllegalOrderStateException;
import com.rishav.orderservice.exception.OrderNotFoundException;
import com.rishav.orderservice.repository.OrderRepository;
import com.rishav.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderCacheService orderCacheService;
    private final OrderEventPublisher eventPublisher;

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for customer: {}", request.getCustomerId());

        Order order = Order.builder()
                .customerId(request.getCustomerId())
                .status(OrderStatus.PENDING)
                .currency(request.getCurrency())
                .shippingAddress(request.getShippingAddress())
                .paymentMethod(request.getPaymentMethod())
                .notes(request.getNotes())
                .totalAmount(BigDecimal.ZERO)
                .build();

        request.getItems().forEach(itemReq -> {
            OrderItem item = OrderItem.builder()
                    .productId(itemReq.getProductId())
                    .productName(itemReq.getProductName())
                    .sku(itemReq.getSku())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(itemReq.getUnitPrice())
                    .discountAmount(itemReq.getDiscountAmount() != null ? itemReq.getDiscountAmount() : BigDecimal.ZERO)
                    .build();
            order.addItem(item);
        });

        Order saved = orderRepository.save(order);
        log.info("Order created with id: {}", saved.getId());

        eventPublisher.publishOrderCreated(saved);
        OrderResponse response = toResponse(saved);
        orderCacheService.cacheOrder(response);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID orderId) {
        return orderCacheService.getCachedOrder(orderId)
                .orElseGet(() -> {
                    Order order = orderRepository.findById(orderId)
                            .orElseThrow(() -> new OrderNotFoundException(orderId));
                    OrderResponse response = toResponse(order);
                    orderCacheService.cacheOrder(response);
                    return response;
                });
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getOrdersByCustomer(UUID customerId, Pageable pageable) {
        Page<Order> page = orderRepository.findByCustomerId(customerId, pageable);
        return buildPagedResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        Page<Order> page = orderRepository.findByStatus(status, pageable);
        return buildPagedResponse(page);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.getStatus().canTransitionTo(request.getStatus())) {
            throw new IllegalOrderStateException(order.getStatus(), request.getStatus());
        }

        OrderStatus previous = order.getStatus();
        order.setStatus(request.getStatus());

        if (request.getStatus() == OrderStatus.DELIVERED) {
            order.setCompletedAt(Instant.now());
        }

        Order updated = orderRepository.save(order);
        log.info("Order {} transitioned from {} to {}", orderId, previous, request.getStatus());

        eventPublisher.publishOrderStatusChanged(updated, previous);
        orderCacheService.evictOrder(orderId);

        return toResponse(updated);
    }

    @Override
    @Transactional
    public void cancelOrder(UUID orderId, String reason) {
        UpdateOrderStatusRequest req = new UpdateOrderStatusRequest();
        req.setStatus(OrderStatus.CANCELLED);
        req.setReason(reason);
        updateOrderStatus(orderId, req);
        log.info("Order {} cancelled. Reason: {}", orderId, reason);
    }

    private PagedResponse<OrderResponse> buildPagedResponse(Page<Order> page) {
        List<OrderResponse> content = page.getContent().stream().map(this::toResponse).toList();
        return PagedResponse.<OrderResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    private OrderResponse toResponse(Order order) {
        List<OrderResponse.OrderItemResponse> items = order.getItems().stream()
                .map(i -> OrderResponse.OrderItemResponse.builder()
                        .id(i.getId())
                        .productId(i.getProductId())
                        .productName(i.getProductName())
                        .sku(i.getSku())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .discountAmount(i.getDiscountAmount())
                        .lineTotal(i.getLineTotal())
                        .build())
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomerId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .shippingAddress(order.getShippingAddress())
                .paymentMethod(order.getPaymentMethod())
                .notes(order.getNotes())
                .items(items)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .completedAt(order.getCompletedAt())
                .build();
    }
}
