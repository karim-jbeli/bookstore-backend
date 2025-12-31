package com.example.orderservice.repository;

import com.example.orderservice.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByUserId(Long userId);

    List<Order> findByStatus(Order.OrderStatus status);

    List<Order> findByPaymentStatus(Order.PaymentStatus paymentStatus);

    List<Order> findByUserIdAndStatus(Long userId, Order.OrderStatus status);

    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate")
    List<Order> findOrdersBetweenDates(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT o FROM Order o WHERE o.totalAmount > :minAmount")
    List<Order> findOrdersWithAmountGreaterThan(Double minAmount);

    long countByUserId(Long userId);

    @Query("SELECT SUM(o.finalAmount) FROM Order o WHERE o.userId = :userId")
    Double getTotalSpentByUser(Long userId);
}