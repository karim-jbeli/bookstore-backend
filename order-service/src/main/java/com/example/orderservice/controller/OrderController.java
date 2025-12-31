package com.example.orderservice.controller;

import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Endpoints pour la gestion des commandes")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Créer une nouvelle commande")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest orderRequest) {
        return ResponseEntity.ok(orderService.createOrder(orderRequest));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer une commande par ID")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping("/number/{orderNumber}")
    @Operation(summary = "Récupérer une commande par numéro")
    public ResponseEntity<OrderResponse> getOrderByNumber(@PathVariable String orderNumber) {
        return ResponseEntity.ok(orderService.getOrderByNumber(orderNumber));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Récupérer les commandes d'un utilisateur")
    public ResponseEntity<List<OrderResponse>> getOrdersByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
    }

    @GetMapping
    @Operation(summary = "Récupérer toutes les commandes")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Récupérer les commandes par statut")
    public ResponseEntity<List<OrderResponse>> getOrdersByStatus(@PathVariable String status) {
        return ResponseEntity.ok(orderService.getOrdersByStatus(status));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Mettre à jour le statut d'une commande")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, status));
    }

    @PutMapping("/{id}/payment-status")
    @Operation(summary = "Mettre à jour le statut de paiement")
    public ResponseEntity<OrderResponse> updatePaymentStatus(
            @PathVariable Long id,
            @RequestParam String paymentStatus) {
        return ResponseEntity.ok(orderService.updatePaymentStatus(id, paymentStatus));
    }

    @PutMapping("/{id}/tracking")
    @Operation(summary = "Mettre à jour le numéro de suivi")
    public ResponseEntity<OrderResponse> updateTrackingNumber(
            @PathVariable Long id,
            @RequestParam String trackingNumber) {
        return ResponseEntity.ok(orderService.updateTrackingNumber(id, trackingNumber));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Annuler une commande")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancelOrder(id));
    }

    @GetMapping("/user/{userId}/stats")
    @Operation(summary = "Récupérer les statistiques d'un utilisateur")
    public ResponseEntity<UserStats> getUserStats(@PathVariable Long userId) {
        Double totalSpent = orderService.getUserTotalSpent(userId);
        Long orderCount = orderService.getUserOrderCount(userId);

        UserStats stats = new UserStats();
        stats.setUserId(userId);
        stats.setTotalSpent(totalSpent);
        stats.setOrderCount(orderCount);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "Récupérer les articles d'une commande")
    public ResponseEntity<List<?>> getOrderItems(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderItems(id));
    }

    @Data
    static class UserStats {
        private Long userId;
        private Double totalSpent;
        private Long orderCount;
    }
}