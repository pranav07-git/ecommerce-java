package com.ecommerce.ui;

import com.ecommerce.model.User;
import com.ecommerce.service.Shop;

/** Entry point of the UI package: opens the menu that matches the user's role. */
public final class Menus {
    private Menus() {
    }

    public static void open(Shop shop, Console c, User user) {
        switch (user.role) {
            case ADMIN -> new AdminMenu(shop, c, user).run();
            case SELLER -> new SellerMenu(shop, c, user).run();
            case BUYER -> new BuyerMenu(shop, c, user).run();
        }
    }
}
