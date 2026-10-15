package com.ecommerce.model;

public enum OrderStatus {
    PLACED, PROCESSING, SHIPPED, DELIVERED, CANCELLED;

    public boolean isFinal() {
        return this == DELIVERED || this == CANCELLED;
    }
}
