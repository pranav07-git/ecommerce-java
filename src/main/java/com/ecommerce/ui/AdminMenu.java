package com.ecommerce.ui;

import com.ecommerce.model.LogEntry;
import com.ecommerce.model.Role;
import com.ecommerce.model.User;
import com.ecommerce.service.Shop;

import java.time.format.DateTimeFormatter;
import java.util.List;

class AdminMenu extends Menu {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd MMM HH:mm:ss");

    AdminMenu(Shop shop, Console c, User me) {
        super(shop, c, me);
    }

    @Override
    void run() {
        loop("ADMIN DASHBOARD - " + me.name, List.of(
                "1) List users", "2) Create user", "3) Edit user", "4) Delete user",
                "5) List products", "6) Add product", "7) Edit product", "8) Remove product",
                "9) List orders", "10) Update order status",
                "11) Recent system activity", "12) Live activity monitor", "0) Logout"),
                12, choice -> {
                    switch (choice) {
                        case 1 -> listUsers();
                        case 2 -> createUser();
                        case 3 -> editUser();
                        case 4 -> deleteUser();
                        case 5 -> ProductUi.list(shop, shop.activeProducts());
                        case 6 -> ProductUi.add(shop, c, me);
                        case 7 -> ProductUi.edit(shop, c, me);
                        case 8 -> ProductUi.remove(shop, c, me);
                        case 9 -> ProductUi.orderTable(shop, shop.allOrders(), me);
                        case 10 -> {
                            ProductUi.orderTable(shop, shop.allOrders(), me);
                            ProductUi.updateOrderStatus(shop, c, me);
                        }
                        case 11 -> printLog(30);
                        case 12 -> liveMonitor();
                        default -> { }
                    }
                });
    }

    private void listUsers() {
        Console.table(List.of("ID", "Name", "Email", "Role", "Status"),
                shop.users().stream().map(u -> List.of(String.valueOf(u.id), u.name, u.email, u.role.name(),
                        u.enabled ? "active" : "disabled")).toList());
    }

    private void createUser() {
        String name = c.required("Name");
        String email = c.required("Email");
        Role role = pickRole(null);
        String pw = c.required("Password (min 6 chars)");
        User u = shop.register(name, email, pw, role, me.email);
        System.out.println("User " + u.email + " created successfully.");
    }

    private void editUser() {
        User u = shop.user(c.id("User id"));
        String name = c.withDefault("Name", u.name);
        String email = c.withDefault("Email", u.email);
        Role role = pickRole(u.role);
        String en = c.line("Enabled? (y/n) [" + (u.enabled ? "y" : "n") + "]");
        boolean enabled = en.isEmpty() ? u.enabled : en.equalsIgnoreCase("y");
        String pw = c.line("New password (Enter to keep)");
        shop.updateUser(u.id, name, email, role, enabled, pw, me);
        System.out.println("User " + email + " updated successfully.");
    }

    private void deleteUser() {
        User u = shop.user(c.id("User id"));
        if (c.confirm("Delete " + u.email + "?")) System.out.println(shop.deleteUser(u.id, me));
    }

    private Role pickRole(Role current) {
        Role[] roles = Role.values();
        for (int i = 0; i < roles.length; i++) System.out.println("  " + (i + 1) + ") " + roles[i]);
        if (current == null) return roles[c.integer("Role", 1, roles.length) - 1];
        return roles[c.integerWithDefault("Role", current.ordinal() + 1, 1, roles.length) - 1];
    }

    private void printLog(int n) {
        for (LogEntry e : shop.recentLog(n)) {
            System.out.println("[" + e.time().format(TIME) + "] " + e.actor() + ": " + e.message());
        }
    }

    /** Redraws the activity feed every 2 seconds until the admin presses Enter. */
    private void liveMonitor() {
        c.drainInput();
        while (!c.hasPendingInput()) {
            System.out.print("\033[H\033[2J");
            System.out.println("=== LIVE SYSTEM ACTIVITY (press Enter to stop) ===");
            printLog(15);
            System.out.flush();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        c.drainInput();
    }
}
