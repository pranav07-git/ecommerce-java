package com.ecommerce.model;

import java.io.Serializable;
import java.math.BigDecimal;

/** Snapshot of a purchased product (name and price as at purchase time). */
public record OrderItem(long productId, long sellerId, String productName, int quantity, BigDecimal unitPrice)
        implements Serializable {

    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
