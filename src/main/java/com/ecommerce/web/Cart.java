package com.ecommerce.web;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.util.LinkedHashMap;
import java.util.Map;

@Component("cart")
@SessionScope
public class Cart {
    private final Map<Long, Integer> items = new LinkedHashMap<>();

    public void add(Long productId, int qty) {
        items.merge(productId, qty, Integer::sum);
    }

    public void set(Long productId, int qty) {
        if (qty <= 0) items.remove(productId);
        else items.put(productId, qty);
    }

    public void remove(Long productId) {
        items.remove(productId);
    }

    public void clear() {
        items.clear();
    }

    public Map<Long, Integer> items() {
        return items;
    }

    public int count() {
        return items.values().stream().mapToInt(Integer::intValue).sum();
    }
}
