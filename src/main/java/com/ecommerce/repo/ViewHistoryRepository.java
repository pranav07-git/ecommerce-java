package com.ecommerce.repo;

import com.ecommerce.model.Product;
import com.ecommerce.model.User;
import com.ecommerce.model.ViewHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ViewHistoryRepository extends JpaRepository<ViewHistory, Long> {
    List<ViewHistory> findTop10ByUserOrderByViewedAtDesc(User user);
    void deleteByUserAndProduct(User user, Product product);
    void deleteByUser(User user);
}
