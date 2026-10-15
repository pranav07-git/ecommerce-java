package com.ecommerce.model;

import java.io.Serializable;
import java.math.BigDecimal;

public record WishlistItem(long userId, long productId, BigDecimal priceWhenAdded) implements Serializable {
}
