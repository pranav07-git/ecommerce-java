package com.ecommerce.repo;

import com.ecommerce.model.Order;
import com.ecommerce.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByBuyerOrderByCreatedAtDesc(User buyer);
    List<Order> findAllByOrderByCreatedAtDesc();
    boolean existsByBuyer(User buyer);

    @Query("select distinct o from Order o join o.items i where i.product.seller = :seller order by o.createdAt desc")
    List<Order> findForSeller(@Param("seller") User seller);
}
