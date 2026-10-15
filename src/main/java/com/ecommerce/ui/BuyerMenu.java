package com.ecommerce.ui;

import com.ecommerce.model.*;
import com.ecommerce.service.Shop;
import com.ecommerce.service.ShopException;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class BuyerMenu extends Menu {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
    private final Map<Long, Integer> cart = new LinkedHashMap<>();

    BuyerMenu(Shop shop, Console c, User me) {
        super(shop, c, me);
    }

    @Override
    void run() {
        loop("BUYER MENU - " + me.name, List.of(
                "1) Browse / search products", "2) View a product (add to cart / wishlist)",
                "3) My cart", "4) Checkout", "5) My orders", "6) Track an order", "7) Cancel an order",
                "8) Recently viewed", "9) Wishlist", "10) Account (profile, addresses, payment methods)", "0) Logout"),
                10, choice -> {
                    switch (choice) {
                        case 1 -> browse();
                        case 2 -> viewProduct();
                        case 3 -> cartMenu();
                        case 4 -> checkout();
                        case 5 -> orderHistory();
                        case 6 -> track();
                        case 7 -> cancel();
                        case 8 -> history();
                        case 9 -> wishlist();
                        case 10 -> account();
                        default -> { }
                    }
                });
    }

    // ---------------------------------------------------------------- browse

    private void browse() {
        String q = c.line("Search text (Enter for all)");
        List<String> cats = shop.categories();
        System.out.println("Categories: " + (cats.isEmpty() ? "-" : String.join(", ", cats)));
        String cat = c.line("Category (Enter for all)");
        BigDecimal min = c.optionalMoney("Min price");
        BigDecimal max = c.optionalMoney("Max price");
        System.out.println("Sort: 1) newest  2) price low-high  3) price high-low  4) name");
        String sort = switch (c.integerWithDefault("Sort", 1, 1, 4)) {
            case 2 -> "price_asc";
            case 3 -> "price_desc";
            case 4 -> "name";
            default -> "";
        };
        List<Product> found = shop.search(q, cat, min, max, sort);
        Console.table(List.of("ID", "Name", "Category", "Price", "Stock"),
                found.stream().map(p -> List.of(String.valueOf(p.id), Console.clip(p.name, 32), p.category,
                        Shop.money(p.price), p.stock == 0 ? "OUT" : String.valueOf(p.stock))).toList());
    }

    private void viewProduct() {
        showProductDetail(shop.activeProduct(c.id("Product id")));
    }

    private void showProductDetail(Product p) {
        shop.recordView(me, p);
        System.out.println();
        System.out.println(p.name + "  -  " + Shop.money(p.price));
        System.out.println("Sold by " + shop.user(p.sellerId).name + " | Category: " + p.category
                + " | " + (p.stock == 0 ? "OUT OF STOCK" : p.stock + " in stock"));
        System.out.println(p.description);
        String act = c.line("[a] add to cart, [w] " + (shop.isWishlisted(me, p.id) ? "remove from" : "add to")
                + " wishlist, Enter to go back").toLowerCase();
        if (act.equals("a")) {
            addToCart(p);
        } else if (act.equals("w")) {
            System.out.println(shop.toggleWishlist(me, p) ? "Added to wishlist." : "Removed from wishlist.");
        }
    }

    // ------------------------------------------------------------------ cart

    private void addToCart(Product p) {
        int qty = c.integerWithDefault("Quantity", 1, 1, 10_000);
        int wanted = cart.getOrDefault(p.id, 0) + qty;
        if (wanted > p.stock) throw new ShopException("Only " + p.stock + " of " + p.name + " in stock");
        cart.put(p.id, wanted);
        System.out.println(p.name + " added to cart.");
    }

    private BigDecimal showCart() {
        BigDecimal total = BigDecimal.ZERO;
        List<List<String>> rows = new java.util.ArrayList<>();
        for (Map.Entry<Long, Integer> e : List.copyOf(cart.entrySet())) {
            try {
                Product p = shop.activeProduct(e.getKey());
                BigDecimal sub = p.price.multiply(BigDecimal.valueOf(e.getValue()));
                total = total.add(sub);
                rows.add(List.of(String.valueOf(p.id), p.name, Shop.money(p.price), String.valueOf(e.getValue()), Shop.money(sub)));
            } catch (ShopException gone) {
                cart.remove(e.getKey());
            }
        }
        Console.table(List.of("ID", "Product", "Price", "Qty", "Subtotal"), rows);
        if (!rows.isEmpty()) System.out.println("Total: " + Shop.money(total));
        return total;
    }

    private void cartMenu() {
        showCart();
        if (cart.isEmpty()) return;
        String act = c.line("[u] change quantity, [r] remove item, [x] empty cart, Enter to go back").toLowerCase();
        switch (act) {
            case "u" -> {
                long id = c.id("Product id");
                if (!cart.containsKey(id)) throw new ShopException("That product is not in your cart");
                int qty = c.integer("New quantity (0 removes)", 0, 10_000);
                Product p = shop.activeProduct(id);
                if (qty > p.stock) throw new ShopException("Only " + p.stock + " in stock");
                if (qty == 0) cart.remove(id);
                else cart.put(id, qty);
            }
            case "r" -> cart.remove(c.id("Product id"));
            case "x" -> cart.clear();
            default -> { }
        }
    }

    private void checkout() {
        showCart();
        if (cart.isEmpty()) return;
        String address;
        if (me.addresses.isEmpty()) {
            address = c.required("Shipping address");
        } else {
            for (int i = 0; i < me.addresses.size(); i++) System.out.println("  " + (i + 1) + ") " + me.addresses.get(i));
            System.out.println("  0) Enter a new address");
            int pick = c.integer("Shipping address", 0, me.addresses.size());
            address = pick == 0 ? c.required("Shipping address") : me.addresses.get(pick - 1);
        }
        String payment = "Cash on Delivery";
        if (!me.paymentMethods.isEmpty()) {
            System.out.println("  0) Cash on Delivery");
            for (int i = 0; i < me.paymentMethods.size(); i++) System.out.println("  " + (i + 1) + ") " + me.paymentMethods.get(i));
            int pick = c.integer("Payment method", 0, me.paymentMethods.size());
            if (pick > 0) payment = me.paymentMethods.get(pick - 1);
        }
        if (!c.confirm("Place this order")) return;
        Order o = shop.placeOrder(me, cart, address, payment);
        cart.clear();
        System.out.println("Order #" + o.id + " confirmed! Total: " + Shop.money(o.total()) + " | Status: " + o.status);
    }

    // ---------------------------------------------------------------- orders

    private void orderHistory() {
        Console.table(List.of("Order", "Date", "Items", "Total", "Status"),
                shop.ordersOfBuyer(me).stream().map(o -> List.of("#" + o.id, o.createdAt.format(DATE),
                        String.valueOf(o.items.size()), Shop.money(o.total()), o.status.name())).toList());
    }

    private void track() {
        Order o = shop.visibleOrder(c.id("Order id"), me);
        System.out.println("\nOrder #" + o.id + " placed " + o.createdAt.format(DATE));
        System.out.println("Ship to: " + o.shippingAddress + " | Payment: " + o.paymentMethod);
        o.items.forEach(i -> System.out.println("  " + i.quantity() + " x " + i.productName() + " @ "
                + Shop.money(i.unitPrice()) + " = " + Shop.money(i.subtotal())));
        System.out.println("Total: " + Shop.money(o.total()));
        if (o.status == OrderStatus.CANCELLED) {
            System.out.println("Status: CANCELLED");
            return;
        }
        StringBuilder sb = new StringBuilder("Progress: ");
        OrderStatus[] steps = {OrderStatus.PLACED, OrderStatus.PROCESSING, OrderStatus.SHIPPED, OrderStatus.DELIVERED};
        for (OrderStatus s : steps) {
            sb.append(s.ordinal() < o.status.ordinal() ? "[x] " : s == o.status ? "[>] " : "[ ] ").append(s).append("  ");
        }
        System.out.println(sb);
    }

    private void cancel() {
        long id = c.id("Order id");
        if (c.confirm("Cancel order #" + id + "?")) {
            shop.cancelByBuyer(id, me);
            System.out.println("Order #" + id + " cancelled.");
        }
    }

    // ------------------------------------------------- history and wishlist

    private void history() {
        Console.table(List.of("ID", "Product", "Price", "Viewed", "Availability"),
                shop.recentViews(me, 10).stream().map(v -> {
                    Product p = shop.anyProduct(v.productId()).orElseThrow();
                    String avail = !p.active ? "unavailable" : p.stock == 0 ? "out of stock" : "in stock";
                    return List.of(String.valueOf(p.id), p.name, Shop.money(p.price), v.viewedAt().format(DATE), avail);
                }).toList());
        long id = optionalId("Product id to revisit/buy (Enter to go back)");
        if (id > 0) showProductDetail(shop.activeProduct(id));
    }

    private void wishlist() {
        Console.table(List.of("ID", "Product", "Price when added", "Now", "Change"),
                shop.wishlistOf(me).stream().map(w -> {
                    Product p = shop.anyProduct(w.productId()).orElseThrow();
                    BigDecimal diff = p.price.subtract(w.priceWhenAdded());
                    String change = diff.signum() == 0 ? "no change"
                            : (diff.signum() > 0 ? "up +" : "down -") + Shop.money(diff.abs());
                    return List.of(String.valueOf(p.id), p.name + (p.active ? "" : " (unavailable)"),
                            Shop.money(w.priceWhenAdded()), Shop.money(p.price), change);
                }).toList());
        long id = optionalId("Product id to view/remove (Enter to go back)");
        if (id > 0) showProductDetail(shop.activeProduct(id));
    }

    private long optionalId(String prompt) {
        String s = c.line(prompt);
        if (s.isEmpty()) return -1;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            throw new ShopException("Enter a numeric id");
        }
    }

    // --------------------------------------------------------------- account

    private void account() {
        while (true) {
            Console.title("Account overview");
            System.out.println("Name: " + me.name + " | Email: " + me.email + " | Phone: " + (me.phone.isEmpty() ? "-" : me.phone));
            System.out.println("Addresses:");
            for (int i = 0; i < me.addresses.size(); i++) System.out.println("  " + (i + 1) + ") " + me.addresses.get(i));
            System.out.println("Payment methods (labels only):");
            for (int i = 0; i < me.paymentMethods.size(); i++) System.out.println("  " + (i + 1) + ") " + me.paymentMethods.get(i));
            System.out.println("[p] edit profile  [a] add address  [d] delete address  "
                    + "[m] add payment method  [x] delete payment method  Enter to go back");
            try {
                switch (c.line("Choose").toLowerCase()) {
                    case "p" -> shop.updateProfile(me, c.withDefault("Name", me.name), c.withDefault("Phone", me.phone));
                    case "a" -> shop.addAddress(me, c.required("Address"));
                    case "d" -> {
                        if (me.addresses.isEmpty()) throw new ShopException("No addresses to delete");
                        shop.removeAddress(me, c.integer("Address number", 1, me.addresses.size()) - 1);
                    }
                    case "m" -> shop.addPaymentMethod(me, c.required("Label (e.g. Visa ending 4242 - never a full card number)"));
                    case "x" -> {
                        if (me.paymentMethods.isEmpty()) throw new ShopException("No payment methods to delete");
                        shop.removePaymentMethod(me, c.integer("Payment method number", 1, me.paymentMethods.size()) - 1);
                    }
                    default -> { return; }
                }
            } catch (ShopException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }
}
