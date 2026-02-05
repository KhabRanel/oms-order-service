package com.example.oms.orderservice.order.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class Order {

    private UUID id;
    private UUID userId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private List<OrderItem> items;
    private Instant createdAt;

    public Order() {
    }

    public Order(
            UUID id,
            UUID userId,
            BigDecimal totalAmount,
            List<OrderItem> items
    ) {
        this.id = id;
        this.userId = userId;
        this.status = OrderStatus.NEW;
        this.totalAmount = totalAmount;
        this.items = items;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}
