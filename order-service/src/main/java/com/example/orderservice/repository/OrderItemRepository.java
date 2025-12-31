package com.example.orderservice.repository;

import com.example.orderservice.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByBookId(Long bookId);

    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.id = :orderId")
    List<OrderItem> findByOrderId(Long orderId);

    @Query("SELECT SUM(oi.quantity) FROM OrderItem oi WHERE oi.bookId = :bookId")
    Long getTotalQuantitySoldByBookId(Long bookId);
}