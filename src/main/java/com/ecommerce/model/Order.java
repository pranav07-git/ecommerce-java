package com.ecommerce.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class Order implements Serializable {
    private static final long serialVersionUID = 1L;

    public final long id;
    public final long buyerId;
    public final List<OrderItem> items;
    public final String shippingAddress;
    public final String paymentMethod;
    public final LocalDateTime createdAt = LocalDateTime.now();
    public OrderStatus status = OrderStatus.PLACED;

    public Order(long id, long buyerId, List<OrderItem> items, String shippingAddress, String paymentMethod) {
        this.id = id;
        this.buyerId = buyerId;
        this.items = items;
        this.shippingAddress = shippingAddress;
        this.paymentMethod = paymentMethod;
    }

    public BigDecimal total() {
        return items.stream().map(OrderItem::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<OrderItem> itemsOf(long sellerId) {
        return items.stream().filter(i -> i.sellerId() == sellerId).toList();
    }
}
