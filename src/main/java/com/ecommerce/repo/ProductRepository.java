package com.ecommerce.repo;

import com.ecommerce.model.Product;
import com.ecommerce.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByActiveTrueOrderByIdDesc();
    List<Product> findBySellerAndActiveTrueOrderByIdDesc(User seller);
    boolean existsBySeller(User seller);
}
