package com.ecommerce.ui;

import com.ecommerce.model.Product;
import com.ecommerce.model.User;
import com.ecommerce.service.SellerReport;
import com.ecommerce.service.Shop;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

class SellerMenu extends Menu {
    SellerMenu(Shop shop, Console c, User me) {
        super(shop, c, me);
    }

    @Override
    void run() {
        alerts();
        loop("SELLER DASHBOARD - " + me.name, List.of(
                "1) My product listings", "2) Add product", "3) Edit product", "4) Remove product",
                "5) Update inventory", "6) Inventory overview (chart + low-stock alerts)",
                "7) Orders for my products", "8) Update order status", "9) Sales performance", "0) Logout"),
                9, choice -> {
                    switch (choice) {
                        case 1 -> ProductUi.list(shop, shop.productsOf(me));
                        case 2 -> ProductUi.add(shop, c, me);
                        case 3 -> ProductUi.edit(shop, c, me);
                        case 4 -> ProductUi.remove(shop, c, me);
                        case 5 -> updateInventory();
                        case 6 -> inventoryOverview();
                        case 7 -> ProductUi.orderTable(shop, shop.ordersForSeller(me), me);
                        case 8 -> {
                            ProductUi.orderTable(shop, shop.ordersForSeller(me), me);
                            ProductUi.updateOrderStatus(shop, c, me);
                        }
                        case 9 -> salesPerformance();
                        default -> { }
                    }
                });
    }

    private void alerts() {
        List<Product> low = shop.productsOf(me).stream().filter(Product::isLowStock).toList();
        if (!low.isEmpty()) {
            System.out.println("\n!! LOW STOCK: " + String.join(", ",
                    low.stream().map(p -> p.name + " (" + p.stock + ")").toList()));
        }
    }

    private void updateInventory() {
        ProductUi.list(shop, shop.productsOf(me));
        Product p = shop.manageable(c.id("Product id"), me);
        int stock = c.integerWithDefault("New stock level", p.stock, 0, 1_000_000);
        shop.setStock(p.id, stock, me);
        System.out.println("Inventory for \"" + p.name + "\" updated to " + stock + ".");
    }

    private void inventoryOverview() {
        Console.title("Inventory levels");
        List<Product> mine = shop.productsOf(me);
        if (mine.isEmpty()) {
            System.out.println("  (no products)");
            return;
        }
        int max = mine.stream().mapToInt(p -> p.stock).max().orElse(1);
        for (Product p : mine) {
            System.out.printf("%-28s %-30s %4d%s%n", Console.clip(p.name, 28), Console.bar(p.stock, max, 30), p.stock,
                    p.isLowStock() ? "  <-- LOW (threshold " + p.lowStockThreshold + ")" : "");
        }
    }

    private void salesPerformance() {
        SellerReport r = shop.sellerReport(me);
        Console.title("Sales performance");
        System.out.println("Revenue: " + Shop.money(r.revenue()) + "   Units sold: " + r.unitsSold()
                + "   Orders: " + r.orderCount() + "   (cancelled orders excluded)");
        System.out.println("\nRevenue by day:");
        BigDecimal max = r.revenueByDay().values().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ONE);
        r.revenueByDay().forEach((day, amount) -> System.out.printf("  %s %-30s %s%n", day,
                Console.bar(amount.doubleValue(), max.doubleValue(), 30), Shop.money(amount)));
        if (r.revenueByDay().isEmpty()) System.out.println("  (no sales yet)");
        System.out.println("\nTop products (units):");
        int maxU = r.topProducts().values().stream().max(Integer::compare).orElse(1);
        for (Map.Entry<String, Integer> e : r.topProducts().entrySet()) {
            System.out.printf("  %-28s %-30s %d%n", Console.clip(e.getKey(), 28), Console.bar(e.getValue(), maxU, 30), e.getValue());
        }
        if (r.topProducts().isEmpty()) System.out.println("  (no sales yet)");
    }
}
