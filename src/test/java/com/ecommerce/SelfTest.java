package com.ecommerce;

import com.ecommerce.model.*;
import com.ecommerce.service.*;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Dependency-free tests: run with ./test.sh (exits non-zero on failure). */
public class SelfTest {
    private static int checks = 0;

    public static void main(String[] args) throws Exception {
        Shop shop = new Shop(new Store(), null);
        Seeder.seed(shop);
        User admin = shop.login("admin@shop.com", "admin123").orElseThrow();
        User seller = shop.login("seller@shop.com", "seller123").orElseThrow();
        User seller2 = shop.login("seller2@shop.com", "seller123").orElseThrow();
        User buyer = shop.login("buyer@shop.com", "buyer123").orElseThrow();

        // auth
        check(shop.login("buyer@shop.com", "wrong").isEmpty(), "wrong password rejected");
        check(shop.login("BUYER@shop.com", "buyer123").isPresent(), "email is case-insensitive");
        fails(() -> shop.register("X", "buyer@shop.com", "secret1", Role.BUYER, "t"), "duplicate email");
        fails(() -> shop.register("X", "bad-email", "secret1", Role.BUYER, "t"), "invalid email");
        fails(() -> shop.register("X", "x@y.com", "123", Role.BUYER, "t"), "short password");

        // search
        check(shop.search("mug", null, null, null, null).size() == 1, "search by text");
        check(shop.search(null, "Home", null, null, "price_asc").get(0).name.startsWith("Desk"), "filter + sort");
        check(shop.search(null, null, new BigDecimal("100"), null, null).size() == 1, "min price filter");

        // purchase decrements stock; insufficient stock is all-or-nothing
        Product lamp = byName(shop, "Desk Lamp");
        Product hoodie = byName(shop, "Cotton Hoodie");
        int lampStock = lamp.stock;
        fails(() -> shop.placeOrder(buyer, Map.of(lamp.id, 1, hoodie.id, 99), "addr", null), "over-stock order");
        check(lamp.stock == lampStock, "failed order leaves stock untouched");
        Order o = shop.placeOrder(buyer, Map.of(lamp.id, 2), "1 Test Rd", "Cash on Delivery");
        check(lamp.stock == lampStock - 2, "stock decremented");
        check(o.total().compareTo(new BigDecimal("49.98")) == 0, "order total");
        fails(() -> shop.placeOrder(buyer, Map.of(), "a", null), "empty cart");

        // fulfilment permissions
        fails(() -> shop.updateStatus(o.id, OrderStatus.SHIPPED, seller), "unrelated seller blocked");
        fails(() -> shop.updateStatus(o.id, OrderStatus.SHIPPED, buyer), "buyer cannot set status");
        shop.updateStatus(o.id, OrderStatus.SHIPPED, seller2);
        fails(() -> shop.cancelByBuyer(o.id, buyer), "cannot cancel shipped order");
        shop.updateStatus(o.id, OrderStatus.DELIVERED, admin);
        fails(() -> shop.updateStatus(o.id, OrderStatus.CANCELLED, admin), "delivered is final");

        // cancel restores stock
        Order o2 = shop.placeOrder(buyer, Map.of(lamp.id, 3), "2 Test Rd", null);
        shop.cancelByBuyer(o2.id, buyer);
        check(lamp.stock == lampStock - 2, "cancel restores stock");

        // product ownership and soft delete
        fails(() -> shop.manageable(lamp.id, seller), "seller cannot manage other's product");
        fails(() -> shop.setStock(lamp.id, -1, seller2), "negative stock");
        fails(() -> shop.updateProduct(lamp.id, "Lamp", "d", new BigDecimal("0"), 1, 1, "c", seller2), "zero price");
        shop.removeProduct(lamp.id, seller2);
        check(shop.search("lamp", null, null, null, null).isEmpty(), "removed product hidden");
        check(shop.order(o.id).items.get(0).productName().startsWith("Desk"), "order history survives removal");

        // reports
        SellerReport rep = shop.sellerReport(seller2);
        check(rep.unitsSold() == 2 && rep.revenue().compareTo(new BigDecimal("49.98")) == 0, "seller report ignores cancelled");

        // wishlist & history
        Product mug = byName(shop, "Ceramic");
        check(shop.toggleWishlist(buyer, mug), "wishlist add");
        shop.updateProduct(mug.id, mug.name, mug.description, new BigDecimal("19.90"), mug.stock, 5, mug.category, seller2);
        check(shop.wishlistOf(buyer).get(0).priceWhenAdded().compareTo(new BigDecimal("29.90")) == 0, "wishlist remembers price");
        check(!shop.toggleWishlist(buyer, mug), "wishlist remove");
        shop.recordView(buyer, mug);
        shop.recordView(buyer, mug);
        check(shop.recentViews(buyer, 10).size() == 1, "history de-duplicates");

        // user management
        fails(() -> shop.deleteUser(admin.id, admin), "admin cannot delete self");
        check(shop.deleteUser(buyer.id, admin).contains("disabled"), "user with history is disabled");
        check(shop.login("buyer@shop.com", "buyer123").isEmpty(), "disabled user cannot log in");
        User tmp = shop.register("Tmp", "tmp@shop.com", "secret1", Role.BUYER, "t");
        check(shop.deleteUser(tmp.id, admin).contains("deleted"), "clean user is deleted");

        // persistence round-trip
        Path f = Files.createTempFile("shop", ".ser");
        Store s = new Store();
        Shop persisted = new Shop(s, f);
        Seeder.seed(persisted);
        Shop reloaded = new Shop(Store.load(f), f);
        check(reloaded.login("admin@shop.com", "admin123").isPresent(), "data survives restart");
        check(reloaded.allOrders().size() == 1, "orders survive restart");
        Files.deleteIfExists(f);

        System.out.println("OK - " + checks + " checks passed");
    }

    private static Product byName(Shop shop, String prefix) {
        return shop.activeProducts().stream().filter(p -> p.name.startsWith(prefix)).findFirst().orElseThrow();
    }

    private static void check(boolean ok, String what) {
        checks++;
        if (!ok) {
            System.err.println("FAIL: " + what);
            System.exit(1);
        }
    }

    private static void fails(Runnable r, String what) {
        try {
            r.run();
        } catch (ShopException expected) {
            checks++;
            return;
        }
        System.err.println("FAIL (no ShopException): " + what);
        System.exit(1);
    }
}
