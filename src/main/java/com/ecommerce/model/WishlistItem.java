package com.ecommerce.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "product_id"}))
@Getter @Setter @NoArgsConstructor
public class WishlistItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User user;

    @ManyToOne(optional = false)
    private Product product;

    /** Price when the product was added, used to show price changes. */
    @Column(precision = 12, scale = 2)
    private BigDecimal priceWhenAdded;

    private LocalDateTime addedAt = LocalDateTime.now();

    public BigDecimal getPriceChange() {
        return product.getPrice().subtract(priceWhenAdded);
    }
}
