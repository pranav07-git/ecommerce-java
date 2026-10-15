package com.ecommerce.service;

import com.ecommerce.model.Role;
import com.ecommerce.model.User;

import java.math.BigDecimal;
import java.util.Map;

/** Demo accounts and products, created on first run (empty data only). */
public final class Seeder {
    private Seeder() {
    }

    public static void seed(Shop shop) {
        User admin = shop.register("Admin", "admin@shop.com", "admin123", Role.ADMIN, "system");
        User s1 = shop.register("Sam Seller", "seller@shop.com", "seller123", Role.SELLER, "system");
        User s2 = shop.register("Sara Store", "seller2@shop.com", "seller123", Role.SELLER, "system");
        User buyer = shop.register("Bella Buyer", "buyer@shop.com", "buyer123", Role.BUYER, "system");
        buyer.addresses.add("12 Market Street, Springfield");
        buyer.paymentMethods.add("Visa ending 4242");

        var phone = shop.addProduct(s1, "Nova X Smartphone", "6.5\" OLED, 128GB, dual camera", bd("299.99"), 25, 5, "Electronics", admin);
        var buds = shop.addProduct(s1, "AirBuds Pro", "Noise-cancelling wireless earbuds", bd("79.00"), 4, 5, "Electronics", admin);
        shop.addProduct(s1, "Trail Running Shoes", "Lightweight shoes with grippy sole", bd("64.50"), 40, 5, "Sports", admin);
        shop.addProduct(s2, "Ceramic Coffee Mug Set", "Set of 4 handmade mugs", bd("29.90"), 60, 5, "Home", admin);
        shop.addProduct(s2, "Cotton Hoodie", "Soft unisex hoodie", bd("45.00"), 3, 5, "Clothing", admin);
        shop.addProduct(s2, "Desk Lamp LED", "Dimmable, USB powered", bd("24.99"), 18, 5, "Home", admin);

        shop.placeOrder(buyer, Map.of(phone.id, 1, buds.id, 2), buyer.addresses.get(0), "Cash on Delivery");
    }

    private static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }
}
