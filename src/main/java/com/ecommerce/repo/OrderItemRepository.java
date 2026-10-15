package com.ecommerce.repo;

import com.ecommerce.model.OrderItem;
import com.ecommerce.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    @Query("select i from OrderItem i where i.product.seller = :seller and i.order.status <> com.ecommerce.model.OrderStatus.CANCELLED")
    List<OrderItem> findSoldBy(@Param("seller") User seller);
}
