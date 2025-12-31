package com.example.paymentservice.controller;


import com.example.paymentservice.dto.PaymentRequest;
import com.example.paymentservice.dto.PaymentResponse;
import com.example.paymentservice.dto.PaymentStatusUpdate;
import com.example.paymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Endpoints pour la gestion des paiements")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Traiter un paiement")
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody PaymentRequest paymentRequest) {
        return ResponseEntity.ok(paymentService.processPayment(paymentRequest));
    }

    @PostMapping("/{id}/refund")
    @Operation(summary = "Rembourser un paiement")
    public ResponseEntity<PaymentResponse> refundPayment(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.refundPayment(id));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Annuler un paiement")
    public ResponseEntity<PaymentResponse> cancelPayment(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.cancelPayment(id));
    }

    @PutMapping("/status")
    @Operation(summary = "Mettre à jour le statut d'un paiement")
    public ResponseEntity<Void> updatePaymentStatus(@Valid @RequestBody PaymentStatusUpdate statusUpdate) {
        paymentService.updatePaymentStatus(statusUpdate);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer un paiement par ID")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    @GetMapping("/reference/{reference}")
    @Operation(summary = "Récupérer un paiement par référence")
    public ResponseEntity<PaymentResponse> getPaymentByReference(@PathVariable String reference) {
        return ResponseEntity.ok(paymentService.getPaymentByReference(reference));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Récupérer les paiements d'une commande")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.getPaymentsByOrderId(orderId));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Récupérer les paiements d'un utilisateur")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(paymentService.getPaymentsByUserId(userId));
    }

    @GetMapping
    @Operation(summary = "Récupérer tous les paiements")
    public ResponseEntity<List<PaymentResponse>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Récupérer les paiements par statut")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(paymentService.getPaymentsByStatus(status));
    }

    @GetMapping("/stats")
    @Operation(summary = "Récupérer les statistiques des paiements")
    public ResponseEntity<Map<String, Object>> getPaymentStatistics() {
        return ResponseEntity.ok(paymentService.getPaymentStatistics());
    }

    @GetMapping("/health")
    @Operation(summary = "Vérifier la santé du service")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "payment-service",
                "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }
}