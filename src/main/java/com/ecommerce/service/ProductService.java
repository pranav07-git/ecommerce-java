package com.ecommerce.service;

import com.ecommerce.model.Product;
import com.ecommerce.model.Role;
import com.ecommerce.model.User;
import com.ecommerce.repo.ProductRepository;
import com.ecommerce.web.ProductForm;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
public class ProductService {
    private final ProductRepository products;
    private final UserService users;
    private final ActivityService activity;

    public ProductService(ProductRepository products, UserService users, ActivityService activity) {
        this.products = products;
        this.users = users;
        this.activity = activity;
    }

    public Product getActive(Long id) {
        return products.findById(id).filter(Product::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
    }

    public List<Product> allActive() {
        return products.findByActiveTrueOrderByIdDesc();
    }

    public List<Product> forSeller(User seller) {
        return products.findBySellerAndActiveTrueOrderByIdDesc(seller);
    }

    public List<String> categories() {
        return allActive().stream().map(Product::getCategory)
                .filter(c -> c != null && !c.isBlank()).distinct().sorted().toList();
    }

    public List<Product> search(String q, String category, BigDecimal min, BigDecimal max, String sort) {
        String needle = q == null ? "" : q.trim().toLowerCase();
        Comparator<Product> order = switch (sort == null ? "" : sort) {
            case "price_asc" -> Comparator.comparing(Product::getPrice);
            case "price_desc" -> Comparator.comparing(Product::getPrice).reversed();
            case "name" -> Comparator.comparing(p -> p.getName().toLowerCase());
            default -> Comparator.comparing(Product::getId).reversed();
        };
        return allActive().stream()
                .filter(p -> needle.isEmpty() || p.getName().toLowerCase().contains(needle)
                        || (p.getDescription() != null && p.getDescription().toLowerCase().contains(needle)))
                .filter(p -> category == null || category.isBlank() || category.equals(p.getCategory()))
                .filter(p -> min == null || p.getPrice().compareTo(min) >= 0)
                .filter(p -> max == null || p.getPrice().compareTo(max) <= 0)
                .sorted(order)
                .toList();
    }

    /** Loads an active product and checks that the actor may manage it (admins: any, sellers: own). */
    public Product requireManageable(Long id, User actor) {
        Product p = getActive(id);
        if (actor.getRole() != Role.ADMIN && !p.getSeller().getId().equals(actor.getId())) {
            throw new AccessDeniedException("Not your product");
        }
        return p;
    }

    public ProductForm toForm(Product p) {
        ProductForm f = new ProductForm();
        f.setName(p.getName());
        f.setDescription(p.getDescription());
        f.setPrice(p.getPrice());
        f.setStock(p.getStock());
        f.setLowStockThreshold(p.getLowStockThreshold());
        f.setCategory(p.getCategory());
        f.setImageUrl(p.getImageUrl());
        f.setSellerId(p.getSeller().getId());
        return f;
    }

    @Transactional
    public Product create(ProductForm f, User actor) {
        Product p = new Product();
        p.setSeller(resolveSeller(f, actor));
        apply(p, f);
        products.save(p);
        activity.log(actor.getEmail(), "Product listed: " + p.getName());
        return p;
    }

    @Transactional
    public Product update(Long id, ProductForm f, User actor) {
        Product p = requireManageable(id, actor);
        if (actor.getRole() == Role.ADMIN) p.setSeller(resolveSeller(f, actor));
        apply(p, f);
        activity.log(actor.getEmail(), "Product updated: " + p.getName());
        return p;
    }

    @Transactional
    public void remove(Long id, User actor) {
        Product p = requireManageable(id, actor);
        p.setActive(false);
        activity.log(actor.getEmail(), "Product removed: " + p.getName());
    }

    @Transactional
    public Product setStock(Long id, int stock, User actor) {
        if (stock < 0) throw new IllegalArgumentException("Stock cannot be negative");
        Product p = requireManageable(id, actor);
        int old = p.getStock();
        p.setStock(stock);
        activity.log(actor.getEmail(), "Inventory of " + p.getName() + ": " + old + " -> " + stock);
        return p;
    }

    private User resolveSeller(ProductForm f, User actor) {
        if (actor.getRole() != Role.ADMIN) return actor;
        if (f.getSellerId() == null) throw new IllegalArgumentException("Choose a seller for this product");
        User seller = users.get(f.getSellerId());
        if (seller.getRole() != Role.SELLER) throw new IllegalArgumentException("Selected user is not a seller");
        return seller;
    }

    private void apply(Product p, ProductForm f) {
        p.setName(f.getName().trim());
        p.setDescription(f.getDescription());
        p.setPrice(f.getPrice());
        p.setStock(f.getStock());
        p.setLowStockThreshold(f.getLowStockThreshold());
        p.setCategory(f.getCategory() == null || f.getCategory().isBlank() ? null : f.getCategory().trim());
        p.setImageUrl(f.getImageUrl() == null || f.getImageUrl().isBlank() ? null : f.getImageUrl().trim());
    }
}
