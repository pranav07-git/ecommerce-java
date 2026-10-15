package com.ecommerce.config;

import com.ecommerce.model.*;
import com.ecommerce.repo.UserRepository;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.ProductService;
import com.ecommerce.service.UserService;
import com.ecommerce.web.ProductForm;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/** Creates demo accounts and products on first start (empty database only). */
@Component
public class DataSeeder implements CommandLineRunner {
    private final UserRepository userRepo;
    private final UserService users;
    private final ProductService products;
    private final OrderService orders;

    public DataSeeder(UserRepository userRepo, UserService users, ProductService products, OrderService orders) {
        this.userRepo = userRepo;
        this.users = users;
        this.products = products;
        this.orders = orders;
    }

    @Override
    public void run(String... args) {
        if (userRepo.count() > 0) return;
        User admin = users.create("Admin", "admin@shop.com", "admin123", Role.ADMIN, "system");
        User seller = users.create("Sam Seller", "seller@shop.com", "seller123", Role.SELLER, "system");
        User seller2 = users.create("Sara Store", "seller2@shop.com", "seller123", Role.SELLER, "system");
        User buyer = users.create("Bella Buyer", "buyer@shop.com", "buyer123", Role.BUYER, "system");

        Product phone = product(seller, "Nova X Smartphone", "6.5\" OLED, 128GB, dual camera.", "299.99", 25, "Electronics");
        Product buds = product(seller, "AirBuds Pro", "Noise-cancelling wireless earbuds.", "79.00", 4, "Electronics");
        product(seller, "Trail Running Shoes", "Lightweight shoes with grippy sole.", "64.50", 40, "Sports");
        product(seller2, "Ceramic Coffee Mug Set", "Set of 4 handmade mugs.", "29.90", 60, "Home");
        product(seller2, "Cotton Hoodie", "Soft unisex hoodie, 3 colours.", "45.00", 3, "Clothing");
        product(seller2, "Desk Lamp LED", "Dimmable, USB powered.", "24.99", 18, "Home");

        orders.place(buyer, Map.of(phone.getId(), 1, buds.getId(), 2), "12 Market Street, Springfield", "Cash on Delivery");
    }

    private Product product(User seller, String name, String desc, String price, int stock, String category) {
        ProductForm f = new ProductForm();
        f.setName(name);
        f.setDescription(desc);
        f.setPrice(new BigDecimal(price));
        f.setStock(stock);
        f.setLowStockThreshold(5);
        f.setCategory(category);
        return products.create(f, seller);
    }
}
