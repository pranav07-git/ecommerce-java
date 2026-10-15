package com.ecommerce.service;

import com.ecommerce.model.Role;
import com.ecommerce.model.User;
import com.ecommerce.repo.*;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {
    private final UserRepository users;
    private final ProductRepository products;
    private final OrderRepository orders;
    private final WishlistRepository wishlists;
    private final ViewHistoryRepository history;
    private final AddressRepository addresses;
    private final PaymentMethodRepository payments;
    private final PasswordEncoder encoder;
    private final ActivityService activity;

    public UserService(UserRepository users, ProductRepository products, OrderRepository orders,
                       WishlistRepository wishlists, ViewHistoryRepository history, AddressRepository addresses,
                       PaymentMethodRepository payments, PasswordEncoder encoder, ActivityService activity) {
        this.users = users;
        this.products = products;
        this.orders = orders;
        this.wishlists = wishlists;
        this.history = history;
        this.addresses = addresses;
        this.payments = payments;
        this.encoder = encoder;
        this.activity = activity;
    }

    /** The logged-in user, or null for anonymous visitors. */
    public User currentOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return users.findByEmail(auth.getName()).orElse(null);
    }

    public User current() {
        User u = currentOrNull();
        if (u == null) throw new IllegalStateException("Not logged in");
        return u;
    }

    public List<User> all() {
        return users.findAll();
    }

    public List<User> sellers() {
        return users.findByRoleAndEnabledTrue(Role.SELLER);
    }

    public User get(Long id) {
        return users.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @Transactional
    public User create(String name, String email, String rawPassword, Role role, String actor) {
        String mail = email.trim().toLowerCase();
        if (users.existsByEmail(mail)) throw new IllegalArgumentException("Email already registered: " + mail);
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        User u = new User();
        u.setName(name.trim());
        u.setEmail(mail);
        u.setPassword(encoder.encode(rawPassword));
        u.setRole(role);
        users.save(u);
        activity.log(actor, "User created: " + mail + " (" + role + ")");
        return u;
    }

    @Transactional
    public User update(Long id, String name, String email, Role role, boolean enabled, String rawPassword, User actor) {
        User u = get(id);
        String mail = email.trim().toLowerCase();
        users.findByEmail(mail).ifPresent(other -> {
            if (!other.getId().equals(id)) throw new IllegalArgumentException("Email already registered: " + mail);
        });
        if (u.getId().equals(actor.getId()) && (role != Role.ADMIN || !enabled)) {
            throw new IllegalArgumentException("You cannot demote or disable your own account");
        }
        u.setName(name.trim());
        u.setEmail(mail);
        u.setRole(role);
        u.setEnabled(enabled);
        if (rawPassword != null && !rawPassword.isBlank()) {
            if (rawPassword.length() < 6) throw new IllegalArgumentException("Password must be at least 6 characters");
            u.setPassword(encoder.encode(rawPassword));
        }
        activity.log(actor.getEmail(), "User updated: " + mail + " (" + role + (enabled ? "" : ", disabled") + ")");
        return u;
    }

    /**
     * Deletes the user. Accounts that own products or orders are disabled instead,
     * so order history and listings stay consistent.
     */
    @Transactional
    public String delete(Long id, User actor) {
        User u = get(id);
        if (u.getId().equals(actor.getId())) throw new IllegalArgumentException("You cannot delete your own account");
        if (products.existsBySeller(u) || orders.existsByBuyer(u)) {
            u.setEnabled(false);
            activity.log(actor.getEmail(), "User disabled (has history): " + u.getEmail());
            return "User " + u.getEmail() + " has products/orders, so the account was disabled instead of deleted.";
        }
        wishlists.deleteByUser(u);
        history.deleteByUser(u);
        addresses.deleteByUser(u);
        payments.deleteByUser(u);
        users.delete(u);
        activity.log(actor.getEmail(), "User deleted: " + u.getEmail());
        return "User " + u.getEmail() + " deleted successfully.";
    }
}
