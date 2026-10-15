package com.ecommerce.service;

import com.ecommerce.model.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** All business rules of the platform. Every mutation is saved to disk straight away. */
public class Shop {
    private final Store store;
    private final Path file;

    public Shop(Store store, Path file) {
        this.store = store;
        this.file = file;
    }

    private void save() {
        store.save(file);
    }

    // ------------------------------------------------------------------ users

    public Optional<User> login(String email, String password) {
        return store.users.stream()
                .filter(u -> u.email.equalsIgnoreCase(email.trim()) && u.enabled && matches(u, password))
                .findFirst();
    }

    public User register(String name, String email, String password, Role role, String actor) {
        if (name == null || name.isBlank()) throw new ShopException("Name is required");
        String mail = email == null ? "" : email.trim().toLowerCase();
        if (!mail.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) throw new ShopException("Enter a valid email address");
        if (findUserByEmail(mail).isPresent()) throw new ShopException("Email already registered: " + mail);
        User u = new User(store.nextId("user"), name.trim(), mail, role);
        setPassword(u, password);
        store.users.add(u);
        log(actor, "User created: " + mail + " (" + role + ")");
        return u;
    }

    public List<User> users() {
        return List.copyOf(store.users);
    }

    public List<User> sellers() {
        return store.users.stream().filter(u -> u.role == Role.SELLER && u.enabled).toList();
    }

    public User user(long id) {
        return store.users.stream().filter(u -> u.id == id).findFirst()
                .orElseThrow(() -> new ShopException("User #" + id + " not found"));
    }

    public User updateUser(long id, String name, String email, Role role, boolean enabled, String newPassword, User actor) {
        User u = user(id);
        String mail = email.trim().toLowerCase();
        findUserByEmail(mail).ifPresent(o -> {
            if (o.id != id) throw new ShopException("Email already registered: " + mail);
        });
        if (u.id == actor.id && (role != Role.ADMIN || !enabled)) {
            throw new ShopException("You cannot demote or disable your own account");
        }
        if (name.isBlank()) throw new ShopException("Name is required");
        u.name = name.trim();
        u.email = mail;
        u.role = role;
        u.enabled = enabled;
        if (newPassword != null && !newPassword.isEmpty()) setPassword(u, newPassword);
        log(actor.email, "User updated: " + mail + " (" + role + (enabled ? "" : ", disabled") + ")");
        return u;
    }

    /** Deletes the user, or disables the account if it owns products or orders. Returns a message. */
    public String deleteUser(long id, User actor) {
        User u = user(id);
        if (u.id == actor.id) throw new ShopException("You cannot delete your own account");
        boolean hasHistory = store.products.stream().anyMatch(p -> p.sellerId == id)
                || store.orders.stream().anyMatch(o -> o.buyerId == id);
        if (hasHistory) {
            u.enabled = false;
            log(actor.email, "User disabled (has history): " + u.email);
            return "User " + u.email + " has products/orders, so the account was disabled instead of deleted.";
        }
        store.users.remove(u);
        store.wishlist.removeIf(w -> w.userId() == id);
        store.views.removeIf(v -> v.userId() == id);
        log(actor.email, "User deleted: " + u.email);
        return "User " + u.email + " deleted successfully.";
    }

    public void updateProfile(User u, String name, String phone) {
        if (name.isBlank()) throw new ShopException("Name is required");
        u.name = name.trim();
        u.phone = phone.trim();
        save();
    }

    public void addAddress(User u, String address) {
        if (address.isBlank()) throw new ShopException("Address cannot be empty");
        u.addresses.add(address.trim());
        save();
    }

    public void removeAddress(User u, int index) {
        if (index < 0 || index >= u.addresses.size()) throw new ShopException("No such address");
        u.addresses.remove(index);
        save();
    }

    public void addPaymentMethod(User u, String label) {
        if (label.isBlank()) throw new ShopException("Payment method label cannot be empty");
        u.paymentMethods.add(label.trim());
        save();
    }

    public void removePaymentMethod(User u, int index) {
        if (index < 0 || index >= u.paymentMethods.size()) throw new ShopException("No such payment method");
        u.paymentMethods.remove(index);
        save();
    }

    private Optional<User> findUserByEmail(String email) {
        return store.users.stream().filter(u -> u.email.equalsIgnoreCase(email)).findFirst();
    }

    private void setPassword(User u, String password) {
        if (password == null || password.length() < 6) throw new ShopException("Password must be at least 6 characters");
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        u.salt = Base64.getEncoder().encodeToString(salt);
        u.passwordHash = hash(u.salt, password);
    }

    private boolean matches(User u, String password) {
        return MessageDigest.isEqual(hash(u.salt, password).getBytes(StandardCharsets.UTF_8),
                u.passwordHash.getBytes(StandardCharsets.UTF_8));
    }

    private static String hash(String salt, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(md.digest((salt + password).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // --------------------------------------------------------------- products

    public Product activeProduct(long id) {
        return store.products.stream().filter(p -> p.id == id && p.active).findFirst()
                .orElseThrow(() -> new ShopException("Product #" + id + " not found"));
    }

    public Optional<Product> anyProduct(long id) {
        return store.products.stream().filter(p -> p.id == id).findFirst();
    }

    public List<Product> activeProducts() {
        return store.products.stream().filter(p -> p.active).toList();
    }

    public List<Product> productsOf(User seller) {
        return store.products.stream().filter(p -> p.active && p.sellerId == seller.id).toList();
    }

    public List<String> categories() {
        return activeProducts().stream().map(p -> p.category).filter(c -> c != null && !c.isBlank())
                .distinct().sorted().toList();
    }

    public List<Product> search(String query, String category, BigDecimal min, BigDecimal max, String sort) {
        String q = query == null ? "" : query.trim().toLowerCase();
        Comparator<Product> order = switch (sort == null ? "" : sort) {
            case "price_asc" -> Comparator.comparing((Product p) -> p.price);
            case "price_desc" -> Comparator.comparing((Product p) -> p.price).reversed();
            case "name" -> Comparator.comparing((Product p) -> p.name.toLowerCase());
            default -> Comparator.comparingLong((Product p) -> p.id).reversed();
        };
        return activeProducts().stream()
                .filter(p -> q.isEmpty() || p.name.toLowerCase().contains(q) || p.description.toLowerCase().contains(q))
                .filter(p -> category == null || category.isBlank() || category.equalsIgnoreCase(p.category))
                .filter(p -> min == null || p.price.compareTo(min) >= 0)
                .filter(p -> max == null || p.price.compareTo(max) <= 0)
                .sorted(order).toList();
    }

    /** Admins may manage any product, sellers only their own. */
    public Product manageable(long id, User actor) {
        Product p = activeProduct(id);
        if (actor.role != Role.ADMIN && p.sellerId != actor.id) throw new ShopException("That is not your product");
        return p;
    }

    public Product addProduct(User sellerOwner, String name, String description, BigDecimal price, int stock,
                              int threshold, String category, User actor) {
        if (sellerOwner.role != Role.SELLER) throw new ShopException("Products must belong to a seller");
        Product p = new Product(store.nextId("product"), sellerOwner.id, "");
        fill(p, name, description, price, stock, threshold, category);
        store.products.add(p);
        log(actor.email, "Product listed: " + p.name);
        return p;
    }

    public Product updateProduct(long id, String name, String description, BigDecimal price, int stock,
                                 int threshold, String category, User actor) {
        Product p = manageable(id, actor);
        fill(p, name, description, price, stock, threshold, category);
        log(actor.email, "Product updated: " + p.name);
        return p;
    }

    public void removeProduct(long id, User actor) {
        Product p = manageable(id, actor);
        p.active = false;
        log(actor.email, "Product removed: " + p.name);
    }

    public Product setStock(long id, int stock, User actor) {
        if (stock < 0) throw new ShopException("Stock cannot be negative");
        Product p = manageable(id, actor);
        int old = p.stock;
        p.stock = stock;
        log(actor.email, "Inventory of " + p.name + ": " + old + " -> " + stock);
        return p;
    }

    private void fill(Product p, String name, String description, BigDecimal price, int stock, int threshold, String category) {
        if (name == null || name.isBlank()) throw new ShopException("Product name is required");
        if (price == null || price.signum() <= 0) throw new ShopException("Price must be greater than 0");
        if (price.scale() > 2) throw new ShopException("Price can have at most 2 decimals");
        if (stock < 0 || threshold < 0) throw new ShopException("Stock and threshold cannot be negative");
        p.name = name.trim();
        p.description = description == null ? "" : description.trim();
        p.price = price;
        p.stock = stock;
        p.lowStockThreshold = threshold;
        p.category = category == null ? "" : category.trim();
        save();
    }

    // ----------------------------------------------------------------- orders

    public Order order(long id) {
        return store.orders.stream().filter(o -> o.id == id).findFirst()
                .orElseThrow(() -> new ShopException("Order #" + id + " not found"));
    }

    public List<Order> allOrders() {
        return store.orders.stream().sorted(Comparator.comparingLong((Order o) -> o.id).reversed()).toList();
    }

    public List<Order> ordersOfBuyer(User buyer) {
        return allOrders().stream().filter(o -> o.buyerId == buyer.id).toList();
    }

    public List<Order> ordersForSeller(User seller) {
        return allOrders().stream().filter(o -> !o.itemsOf(seller.id).isEmpty()).toList();
    }

    /** Order the user is allowed to see: any for admins, own for buyers, ones with own products for sellers. */
    public Order visibleOrder(long id, User user) {
        Order o = order(id);
        boolean ok = switch (user.role) {
            case ADMIN -> true;
            case BUYER -> o.buyerId == user.id;
            case SELLER -> !o.itemsOf(user.id).isEmpty();
        };
        if (!ok) throw new ShopException("Order #" + id + " not found");
        return o;
    }

    /** Places an order for the cart (productId -> quantity). All-or-nothing: stock is checked before anything changes. */
    public Order placeOrder(User buyer, Map<Long, Integer> cart, String address, String payment) {
        if (cart.isEmpty()) throw new ShopException("Your cart is empty");
        if (address == null || address.isBlank()) throw new ShopException("Shipping address is required");
        List<OrderItem> items = new ArrayList<>();
        for (Map.Entry<Long, Integer> e : cart.entrySet()) {
            Product p = activeProduct(e.getKey());
            if (e.getValue() < 1) throw new ShopException("Invalid quantity for " + p.name);
            if (p.stock < e.getValue()) throw new ShopException("Only " + p.stock + " left in stock for " + p.name);
            items.add(new OrderItem(p.id, p.sellerId, p.name, e.getValue(), p.price));
        }
        items.forEach(i -> activeProduct(i.productId()).stock -= i.quantity());
        Order o = new Order(store.nextId("order"), buyer.id, items, address.trim(),
                payment == null || payment.isBlank() ? "Cash on Delivery" : payment);
        store.orders.add(o);
        log(buyer.email, "Order #" + o.id + " placed (total " + money(o.total()) + ")");
        return o;
    }

    /** Status update by an admin, or by a seller whose product is in the order. */
    public Order updateStatus(long orderId, OrderStatus status, User actor) {
        if (actor.role == Role.BUYER) throw new ShopException("Buyers cannot change order status");
        Order o = visibleOrder(orderId, actor);
        transition(o, status, actor);
        return o;
    }

    public Order cancelByBuyer(long orderId, User buyer) {
        Order o = visibleOrder(orderId, buyer);
        if (o.status != OrderStatus.PLACED) {
            throw new ShopException("Only orders that have not started processing can be cancelled");
        }
        transition(o, OrderStatus.CANCELLED, buyer);
        return o;
    }

    private void transition(Order o, OrderStatus next, User actor) {
        if (o.status.isFinal()) throw new ShopException("Order #" + o.id + " is already " + o.status);
        if (o.status == next) throw new ShopException("Order #" + o.id + " is already " + next);
        if (next == OrderStatus.CANCELLED) {
            o.items.forEach(i -> anyProduct(i.productId()).ifPresent(p -> p.stock += i.quantity()));
        }
        OrderStatus old = o.status;
        o.status = next;
        log(actor.email, "Order #" + o.id + " status " + old + " -> " + next);
    }

    // ---------------------------------------------------- wishlist & history

    public void recordView(User buyer, Product p) {
        store.views.removeIf(v -> v.userId() == buyer.id && v.productId() == p.id);
        store.views.add(new ViewRecord(buyer.id, p.id, LocalDateTime.now()));
        save();
    }

    public List<ViewRecord> recentViews(User buyer, int limit) {
        List<ViewRecord> mine = store.views.stream().filter(v -> v.userId() == buyer.id).collect(Collectors.toList());
        Collections.reverse(mine);
        return mine.stream().limit(limit).toList();
    }

    public boolean isWishlisted(User buyer, long productId) {
        return store.wishlist.stream().anyMatch(w -> w.userId() == buyer.id && w.productId() == productId);
    }

    /** Adds or removes the product; returns true if it is now on the wishlist. */
    public boolean toggleWishlist(User buyer, Product p) {
        if (store.wishlist.removeIf(w -> w.userId() == buyer.id && w.productId() == p.id)) {
            save();
            return false;
        }
        store.wishlist.add(new WishlistItem(buyer.id, p.id, p.price));
        save();
        return true;
    }

    public List<WishlistItem> wishlistOf(User buyer) {
        return store.wishlist.stream().filter(w -> w.userId() == buyer.id).toList();
    }

    // ---------------------------------------------------------------- reports

    public SellerReport sellerReport(User seller) {
        BigDecimal revenue = BigDecimal.ZERO;
        int units = 0;
        Set<Long> orderIds = new HashSet<>();
        Map<LocalDate, BigDecimal> byDay = new TreeMap<>();
        Map<String, Integer> byProduct = new HashMap<>();
        for (Order o : store.orders) {
            if (o.status == OrderStatus.CANCELLED) continue;
            for (OrderItem i : o.itemsOf(seller.id)) {
                revenue = revenue.add(i.subtotal());
                units += i.quantity();
                orderIds.add(o.id);
                byDay.merge(o.createdAt.toLocalDate(), i.subtotal(), BigDecimal::add);
                byProduct.merge(i.productName(), i.quantity(), Integer::sum);
            }
        }
        Map<String, Integer> top = new LinkedHashMap<>();
        byProduct.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(5)
                .forEach(e -> top.put(e.getKey(), e.getValue()));
        return new SellerReport(revenue, units, orderIds.size(), byDay, top);
    }

    // --------------------------------------------------------------- activity

    public void log(String actor, String message) {
        store.log.add(new LogEntry(store.nextId("log"), LocalDateTime.now(), actor, message));
        save();
    }

    public List<LogEntry> recentLog(int limit) {
        int from = Math.max(0, store.log.size() - limit);
        return List.copyOf(store.log.subList(from, store.log.size()));
    }

    public boolean isEmpty() {
        return store.users.isEmpty();
    }

    public static String money(BigDecimal v) {
        return String.format("$%,.2f", v);
    }
}
