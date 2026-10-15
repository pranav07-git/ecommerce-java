package com.ecommerce.service;

/** A business-rule violation whose message is safe to show to the user. */
public class ShopException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ShopException(String message) {
        super(message);
    }
}
