package com.ecommerce.repo;

import com.ecommerce.model.Product;
import com.ecommerce.model.User;
import com.ecommerce.model.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<WishlistItem, Long> {
    List<WishlistItem> findByUserOrderByAddedAtDesc(User user);
    Optional<WishlistItem> findByUserAndProduct(User user, Product product);
    void deleteByUser(User user);
}
