package com.ecommerce.web;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductForm {
    @NotBlank(message = "Name is required")
    @Size(max = 255)
    private String name;

    @Size(max = 2000, message = "Description is too long (max 2000)")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be at least 0.01")
    @Digits(integer = 10, fraction = 2, message = "Price must have at most 2 decimals")
    private BigDecimal price;

    @Min(value = 0, message = "Stock cannot be negative")
    private int stock;

    @Min(value = 0, message = "Threshold cannot be negative")
    private int lowStockThreshold = 5;

    private String category;
    private String imageUrl;

    /** Only used by admins to choose the owning seller. */
    private Long sellerId;
}
