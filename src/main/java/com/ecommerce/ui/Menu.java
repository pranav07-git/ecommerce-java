package com.ecommerce.ui;

import com.ecommerce.model.User;
import com.ecommerce.service.Shop;
import com.ecommerce.service.ShopException;

import java.util.List;
import java.util.function.IntConsumer;

/** Base class for the role menus: shows options, runs the chosen action, reports business errors. */
abstract class Menu {
    protected final Shop shop;
    protected final Console c;
    protected final User me;

    Menu(Shop shop, Console c, User me) {
        this.shop = shop;
        this.c = c;
        this.me = me;
    }

    abstract void run();

    protected void loop(String title, List<String> options, int maxChoice, IntConsumer action) {
        while (true) {
            Console.title(title);
            options.forEach(o -> System.out.println("  " + o));
            int choice = c.integer("Choose", 0, maxChoice);
            if (choice == 0) return;
            try {
                action.accept(choice);
            } catch (ShopException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }
}
