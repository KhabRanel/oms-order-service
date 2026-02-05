package com.example.oms.orderservice.order.domain;

public enum OrderStatus {

    NEW,
    RESERVING_STOCK,
    PENDING_PAYMENT,
    PAID,
    CONFIRMED,
    CANCELED
}