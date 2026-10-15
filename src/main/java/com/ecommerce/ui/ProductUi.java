package com.ecommerce.ui;

import com.ecommerce.model.Product;
import com.ecommerce.model.Role;
import com.ecommerce.model.User;
import com.ecommerce.service.Shop;

import java.util.List;

/** Product screens shared by the admin and seller menus. */
final class ProductUi {
    private ProductUi() {
    }

    static void list(Shop shop, List<Product> products) {
        Console.table(List.of("ID", "Name", "Seller", "Category", "Price", "Stock"),
                products.stream().map(p -> List.of(String.valueOf(p.id), Console.clip(p.name, 30),
                        shop.user(p.sellerId).name, p.category, Shop.money(p.price),
                        p.stock + (p.isLowStock() ? " LOW" : ""))).toList());
    }

    static void add(Shop shop, Console c, User actor) {
        User owner = actor;
        if (actor.role == Role.ADMIN) {
            owner = shop.user(c.id("Seller user id"));
        }
        String name = c.required("Name");
        String desc = c.line("Description");
        var price = c.money("Price", null);
        int stock = c.integer("Stock", 0, 1_000_000);
        int threshold = c.integerWithDefault("Low-stock alert threshold", 5, 0, 1_000_000);
        String category = c.line("Category");
        Product p = shop.addProduct(owner, name, desc, price, stock, threshold, category, actor);
        System.out.println("Product \"" + p.name + "\" (#" + p.id + ") listed successfully.");
    }

    static void edit(Shop shop, Console c, User actor) {
        Product p = shop.manageable(c.id("Product id"), actor);
        String name = c.withDefault("Name", p.name);
        String desc = c.withDefault("Description", p.description);
        var price = c.money("Price", p.price);
        int stock = c.integerWithDefault("Stock", p.stock, 0, 1_000_000);
        int threshold = c.integerWithDefault("Low-stock alert threshold", p.lowStockThreshold, 0, 1_000_000);
        String category = c.withDefault("Category", p.category);
        shop.updateProduct(p.id, name, desc, price, stock, threshold, category, actor);
        System.out.println("Product \"" + name + "\" updated successfully.");
    }

    static void remove(Shop shop, Console c, User actor) {
        Product p = shop.manageable(c.id("Product id"), actor);
        if (c.confirm("Remove \"" + p.name + "\"?")) {
            shop.removeProduct(p.id, actor);
            System.out.println("Product deleted successfully.");
        }
    }

    static void orderTable(Shop shop, List<com.ecommerce.model.Order> orders, User viewer) {
        Console.table(List.of("Order", "Date", "Buyer", "Items", "Total", "Status"),
                orders.stream().map(o -> {
                    var items = viewer.role == Role.SELLER ? o.itemsOf(viewer.id) : o.items;
                    var total = items.stream().map(com.ecommerce.model.OrderItem::subtotal)
                            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                    String desc = String.join(", ", items.stream().map(i -> i.quantity() + "x " + i.productName()).toList());
                    return List.of("#" + o.id, o.createdAt.toString().substring(0, 16).replace('T', ' '),
                            shop.user(o.buyerId).name, Console.clip(desc, 45), Shop.money(total), o.status.name());
                }).toList());
    }

    static void updateOrderStatus(Shop shop, Console c, User actor) {
        var order = shop.visibleOrder(c.id("Order id"), actor);
        if (order.status.isFinal()) {
            System.out.println("Order #" + order.id + " is already " + order.status + " and cannot change.");
            return;
        }
        var all = com.ecommerce.model.OrderStatus.values();
        System.out.println("Current status: " + order.status);
        for (int i = 0; i < all.length; i++) System.out.println("  " + (i + 1) + ") " + all[i]);
        var next = all[c.integer("New status", 1, all.length) - 1];
        shop.updateStatus(order.id, next, actor);
        System.out.println("Order #" + order.id + " is now " + next + ".");
    }
}
