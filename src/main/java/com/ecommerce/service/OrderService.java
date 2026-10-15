package com.ecommerce.service;

import com.ecommerce.model.*;
import com.ecommerce.repo.OrderRepository;
import com.ecommerce.repo.ProductRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class OrderService {
    private final OrderRepository orders;
    private final ProductRepository products;
    private final ActivityService activity;

    public OrderService(OrderRepository orders, ProductRepository products, ActivityService activity) {
        this.orders = orders;
        this.products = products;
        this.activity = activity;
    }

    public Order get(Long id) {
        return orders.findById(id).orElseThrow(() -> new IllegalArgumentException("Order not found"));
    }

    /** Order visible to the user: own order for buyers, containing own products for sellers, any for admins. */
    public Order getVisible(Long id, User user) {
        Order o = get(id);
        boolean ok = switch (user.getRole()) {
            case ADMIN -> true;
            case BUYER -> o.getBuyer().getId().equals(user.getId());
            case SELLER -> !o.itemsOf(user).isEmpty();
        };
        if (!ok) throw new AccessDeniedException("Order not accessible");
        return o;
    }

    @Transactional
    public Order place(User buyer, Map<Long, Integer> cart, String address, String payment) {
        if (cart.isEmpty()) throw new IllegalArgumentException("Your cart is empty");
        if (address == null || address.isBlank()) throw new IllegalArgumentException("Shipping address is required");
        Order order = new Order();
        order.setBuyer(buyer);
        order.setShippingAddress(address.trim());
        order.setPaymentMethod(payment == null || payment.isBlank() ? "Cash on Delivery" : payment);
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> e : cart.entrySet()) {
            Product p = products.findById(e.getKey()).filter(Product::isActive)
                    .orElseThrow(() -> new IllegalStateException("A product in your cart is no longer available"));
            int qty = e.getValue();
            if (qty < 1) throw new IllegalArgumentException("Invalid quantity");
            if (p.getStock() < qty) {
                throw new IllegalStateException("Only " + p.getStock() + " left in stock for " + p.getName());
            }
            p.setStock(p.getStock() - qty);
            OrderItem item = new OrderItem();
            item.setProduct(p);
            item.setQuantity(qty);
            item.setUnitPrice(p.getPrice());
            order.addItem(item);
            total = total.add(item.getSubtotal());
        }
        order.setTotal(total);
        orders.save(order);
        activity.log(buyer.getEmail(), "Order #" + order.getId() + " placed (total " + total + ")");
        return order;
    }

    @Transactional
    public Order updateStatus(Long orderId, OrderStatus status, User actor) {
        Order o = getVisible(orderId, actor);
        if (actor.getRole() == Role.BUYER) throw new AccessDeniedException("Buyers cannot change order status");
        transition(o, status, actor);
        return o;
    }

    @Transactional
    public Order cancelByBuyer(Long orderId, User buyer) {
        Order o = getVisible(orderId, buyer);
        if (o.getStatus() != OrderStatus.PLACED) {
            throw new IllegalStateException("Only orders that have not started processing can be cancelled");
        }
        transition(o, OrderStatus.CANCELLED, buyer);
        return o;
    }

    private void transition(Order o, OrderStatus next, User actor) {
        if (o.getStatus().isFinal()) {
            throw new IllegalStateException("Order #" + o.getId() + " is already " + o.getStatus());
        }
        if (next == OrderStatus.CANCELLED) {
            o.getItems().forEach(i -> i.getProduct().setStock(i.getProduct().getStock() + i.getQuantity()));
        }
        OrderStatus old = o.getStatus();
        o.setStatus(next);
        activity.log(actor.getEmail(), "Order #" + o.getId() + " status " + old + " -> " + next);
    }
}
