package com.ecommerce.service;

import com.ecommerce.model.*;
import com.ecommerce.repo.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BuyerService {
    private final ViewHistoryRepository history;
    private final WishlistRepository wishlists;
    private final AddressRepository addresses;
    private final PaymentMethodRepository payments;
    private final UserRepository users;

    public BuyerService(ViewHistoryRepository history, WishlistRepository wishlists, AddressRepository addresses,
                        PaymentMethodRepository payments, UserRepository users) {
        this.history = history;
        this.wishlists = wishlists;
        this.addresses = addresses;
        this.payments = payments;
        this.users = users;
    }

    @Transactional
    public void recordView(User user, Product product) {
        history.deleteByUserAndProduct(user, product);
        ViewHistory v = new ViewHistory();
        v.setUser(user);
        v.setProduct(product);
        history.save(v);
    }

    public boolean isWishlisted(User user, Product product) {
        return wishlists.findByUserAndProduct(user, product).isPresent();
    }

    /** Adds to the wishlist, or removes it if already there. Returns true when now wishlisted. */
    @Transactional
    public boolean toggleWishlist(User user, Product product) {
        var existing = wishlists.findByUserAndProduct(user, product);
        if (existing.isPresent()) {
            wishlists.delete(existing.get());
            return false;
        }
        WishlistItem w = new WishlistItem();
        w.setUser(user);
        w.setProduct(product);
        w.setPriceWhenAdded(product.getPrice());
        wishlists.save(w);
        return true;
    }

    @Transactional
    public void updateProfile(User user, String name, String phone) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name is required");
        User u = users.findById(user.getId()).orElseThrow();
        u.setName(name.trim());
        u.setPhone(phone == null || phone.isBlank() ? null : phone.trim());
    }

    public void addAddress(User user, String label, String line) {
        if (label == null || label.isBlank() || line == null || line.isBlank()) {
            throw new IllegalArgumentException("Address label and details are required");
        }
        Address a = new Address();
        a.setUser(user);
        a.setLabel(label.trim());
        a.setLine(line.trim());
        addresses.save(a);
    }

    public void deleteAddress(User user, Long id) {
        Address a = addresses.findById(id).orElseThrow(() -> new IllegalArgumentException("Address not found"));
        if (!a.getUser().getId().equals(user.getId())) throw new AccessDeniedException("Not your address");
        addresses.delete(a);
    }

    public void addPayment(User user, String label) {
        if (label == null || label.isBlank()) throw new IllegalArgumentException("Payment method label is required");
        PaymentMethod p = new PaymentMethod();
        p.setUser(user);
        p.setLabel(label.trim());
        payments.save(p);
    }

    public void deletePayment(User user, Long id) {
        PaymentMethod p = payments.findById(id).orElseThrow(() -> new IllegalArgumentException("Payment method not found"));
        if (!p.getUser().getId().equals(user.getId())) throw new AccessDeniedException("Not your payment method");
        payments.delete(p);
    }
}
