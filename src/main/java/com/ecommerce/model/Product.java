package com.ecommerce.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    private int stock;

    private int lowStockThreshold = 5;

    private String category;

    private String imageUrl;

    /** Soft-delete flag: removed products stay in the DB so past orders keep working. */
    private boolean active = true;

    @ManyToOne(optional = false)
    private User seller;

    private LocalDateTime createdAt = LocalDateTime.now();

    public boolean isLowStock() {
        return stock <= lowStockThreshold;
    }
}
