package com.example.oms.orderservice.order.domain;

import java.math.BigDecimal;
import java.util.UUID;

public class OrderItem {

    private UUID productId;
    private int quantity;
    private BigDecimal price;

    public OrderItem() {
    }

    public OrderItem(UUID productId, int quantity, BigDecimal price) {
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
    }

    public UUID getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal totalPrice() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
