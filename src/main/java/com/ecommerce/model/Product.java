package com.ecommerce.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class Product implements Serializable {
    private static final long serialVersionUID = 1L;

    public final long id;
    public long sellerId;
    public String name;
    public String description;
    public BigDecimal price;
    public int stock;
    public int lowStockThreshold = 5;
    public String category;
    /** Soft-delete flag so past orders keep working. */
    public boolean active = true;

    public Product(long id, long sellerId, String name) {
        this.id = id;
        this.sellerId = sellerId;
        this.name = name;
    }

    public boolean isLowStock() {
        return stock <= lowStockThreshold;
    }
}
