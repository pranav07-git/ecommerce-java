package com.ecommerce;

import com.ecommerce.model.Role;
import com.ecommerce.model.Store;
import com.ecommerce.model.User;
import com.ecommerce.service.Seeder;
import com.ecommerce.service.Shop;
import com.ecommerce.service.ShopException;
import com.ecommerce.ui.Console;
import com.ecommerce.ui.Menus;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        Path file = Path.of("data", "store.ser");
        Shop shop = new Shop(Store.load(file), file);
        if (shop.isEmpty()) {
            Seeder.seed(shop);
            System.out.println("First run: demo data created.");
            System.out.println("  admin@shop.com / admin123, seller@shop.com / seller123, buyer@shop.com / buyer123");
        }
        Console c = new Console();
        System.out.println("\nWelcome to ShopHub (console e-commerce)");
        try {
            while (true) {
                Console.title("MAIN MENU");
                System.out.println("  1) Login\n  2) Register as buyer or seller\n  0) Exit");
                int choice = c.integer("Choose", 0, 2);
                if (choice == 0) break;
                try {
                    if (choice == 1) login(shop, c);
                    else register(shop, c);
                } catch (ShopException e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        } catch (IllegalStateException endOfInput) {
            // stdin closed: leave quietly
        }
        System.out.println("Goodbye!");
    }

    private static void login(Shop shop, Console c) {
        String email = c.required("Email");
        String pw = c.required("Password");
        User u = shop.login(email, pw).orElseThrow(() -> new ShopException("Invalid credentials or disabled account"));
        shop.log(u.email, "Logged in");
        Menus.open(shop, c, u);
        shop.log(u.email, "Logged out");
    }

    private static void register(Shop shop, Console c) {
        String name = c.required("Name");
        String email = c.required("Email");
        String pw = c.required("Password (min 6 chars)");
        System.out.println("  1) Buyer\n  2) Seller");
        Role role = c.integer("I want to", 1, 2) == 1 ? Role.BUYER : Role.SELLER;
        shop.register(name, email, pw, role, email);
        System.out.println("Account created. You can log in now.");
    }
}
