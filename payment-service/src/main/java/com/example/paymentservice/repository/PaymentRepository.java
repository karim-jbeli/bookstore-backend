package com.example.paymentservice.repository;

import com.example.paymentservice.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentReference(String paymentReference);

    Optional<Payment> findByGatewayTransactionId(String gatewayTransactionId);

    List<Payment> findByOrderId(Long orderId);

    List<Payment> findByUserId(Long userId);

    List<Payment> findByStatus(Payment.PaymentStatus status);

    List<Payment> findByPaymentMethod(Payment.PaymentMethod paymentMethod);

    @Query("SELECT p FROM Payment p WHERE p.createdAt BETWEEN :startDate AND :endDate")
    List<Payment> findPaymentsBetweenDates(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT p FROM Payment p WHERE p.amount > :minAmount")
    List<Payment> findPaymentsWithAmountGreaterThan(Double minAmount);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'SUCCEEDED' AND p.createdAt BETWEEN :startDate AND :endDate")
    Double getTotalRevenueBetweenDates(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = 'SUCCEEDED'")
    Long countSuccessfulPayments();

    @Query("SELECT p.paymentMethod, COUNT(p) FROM Payment p WHERE p.status = 'SUCCEEDED' GROUP BY p.paymentMethod")
    List<Object[]> countPaymentsByMethod();
}