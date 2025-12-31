package com.example.paymentservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.paymentservice.dto.PaymentRequest;
import com.example.paymentservice.dto.PaymentResponse;
import com.example.paymentservice.dto.PaymentStatusUpdate;
import com.example.paymentservice.model.Payment;
import com.example.paymentservice.repository.PaymentRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentGatewayService paymentGatewayService;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public PaymentResponse processPayment(PaymentRequest paymentRequest) {
        log.info("Traitement du paiement pour la commande: {}", paymentRequest.getOrderNumber());

        try {
            // 1. Créer l'enregistrement de paiement
            Payment payment = new Payment();
            payment.setOrderId(paymentRequest.getOrderId());
            payment.setOrderNumber(paymentRequest.getOrderNumber());
            payment.setUserId(paymentRequest.getUserId());
            payment.setUserEmail(paymentRequest.getUserEmail());
            payment.setAmount(paymentRequest.getAmount());
            payment.setPaymentMethod(paymentRequest.getPaymentMethod());
            payment.setStatus(Payment.PaymentStatus.PROCESSING);
            payment.setDescription(paymentRequest.getDescription());
            payment.setCurrency("EUR");

            Payment savedPayment = paymentRepository.save(payment);

            // 2. Traiter le paiement via la passerelle
            PaymentGatewayResponse gatewayResponse;

            switch (paymentRequest.getPaymentMethod()) {
                case CREDIT_CARD:
                case DEBIT_CARD:
                    if (paymentRequest.getCreditCard() == null) {
                        throw new RuntimeException("Informations de carte de crédit requises");
                    }
                    gatewayResponse = paymentGatewayService.processCardPayment(
                            paymentRequest.getCreditCard(),
                            paymentRequest.getAmount(),
                            savedPayment.getPaymentReference()
                    );
                    break;

                case PAYPAL:
                    if (paymentRequest.getPaypalEmail() == null || paymentRequest.getPaypalEmail().isEmpty()) {
                        throw new RuntimeException("Email PayPal requis");
                    }
                    gatewayResponse = paymentGatewayService.processPaypalPayment(
                            paymentRequest.getPaypalEmail(),
                            paymentRequest.getAmount(),
                            savedPayment.getPaymentReference()
                    );
                    break;

                case BANK_TRANSFER:
                    gatewayResponse = paymentGatewayService.processBankTransfer(
                            paymentRequest.getAmount(),
                            savedPayment.getPaymentReference()
                    );
                    break;

                default:
                    throw new RuntimeException("Méthode de paiement non supportée");
            }

            // 3. Mettre à jour le statut du paiement
            if (gatewayResponse.isSuccess()) {
                payment.setStatus(Payment.PaymentStatus.SUCCEEDED);
                payment.setPaidAt(LocalDateTime.now());
                payment.setGatewayTransactionId(gatewayResponse.getTransactionId());
                payment.setGatewayResponse(gatewayResponse.getResponse());

                // Pour les cartes, enregistrer les derniers chiffres et la marque
                if (paymentRequest.getPaymentMethod() == Payment.PaymentMethod.CREDIT_CARD ||
                        paymentRequest.getPaymentMethod() == Payment.PaymentMethod.DEBIT_CARD) {
                    String cardNumber = paymentRequest.getCreditCard().getCardNumber();
                    payment.setCardLastFour(cardNumber.substring(cardNumber.length() - 4));
                    payment.setCardBrand(detectCardBrand(cardNumber));
                }
            } else {
                payment.setStatus(Payment.PaymentStatus.FAILED);
                payment.setGatewayResponse(gatewayResponse.getResponse());
            }

            Payment updatedPayment = paymentRepository.save(payment);

            // 4. Publier l'événement de statut de paiement
            publishPaymentStatusEvent(updatedPayment);

            log.info("Paiement traité avec succès: {}", updatedPayment.getPaymentReference());

            return mapToPaymentResponse(updatedPayment);

        } catch (Exception e) {
            log.error("Erreur lors du traitement du paiement", e);
            throw new RuntimeException("Erreur lors du traitement du paiement: " + e.getMessage());
        }
    }

    @Transactional
    public PaymentResponse refundPayment(Long paymentId) {
        log.info("Traitement du remboursement pour le paiement: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Paiement non trouvé"));

        if (payment.getStatus() != Payment.PaymentStatus.SUCCEEDED) {
            throw new RuntimeException("Seuls les paiements réussis peuvent être remboursés");
        }

        if (payment.getGatewayTransactionId() == null || payment.getGatewayTransactionId().isEmpty()) {
            throw new RuntimeException("Transaction ID manquant pour le remboursement");
        }

        try {
            // Traiter le remboursement via la passerelle
            PaymentGatewayResponse refundResponse = paymentGatewayService.processRefund(
                    payment.getGatewayTransactionId(),
                    payment.getAmount(),
                    payment.getPaymentReference() + "-REFUND"
            );

            if (refundResponse.isSuccess()) {
                payment.setStatus(Payment.PaymentStatus.REFUNDED);
                payment.setGatewayResponse(
                        payment.getGatewayResponse() + " | Remboursement: " + refundResponse.getResponse()
                );

                Payment updatedPayment = paymentRepository.save(payment);

                // Publier l'événement de remboursement
                publishRefundEvent(updatedPayment);

                log.info("Remboursement traité avec succès: {}", payment.getPaymentReference());

                return mapToPaymentResponse(updatedPayment);
            } else {
                throw new RuntimeException("Échec du remboursement: " + refundResponse.getResponse());
            }

        } catch (Exception e) {
            log.error("Erreur lors du remboursement", e);
            throw new RuntimeException("Erreur lors du remboursement: " + e.getMessage());
        }
    }

    @Transactional
    public PaymentResponse cancelPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Paiement non trouvé"));

        if (payment.getStatus() != Payment.PaymentStatus.PENDING &&
                payment.getStatus() != Payment.PaymentStatus.PROCESSING) {
            throw new RuntimeException("Seuls les paiements en attente ou en traitement peuvent être annulés");
        }

        payment.setStatus(Payment.PaymentStatus.CANCELLED);
        Payment updatedPayment = paymentRepository.save(payment);

        // Publier l'événement d'annulation
        publishPaymentStatusEvent(updatedPayment);

        return mapToPaymentResponse(updatedPayment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Paiement non trouvé"));

        return mapToPaymentResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByReference(String paymentReference) {
        Payment payment = paymentRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> new RuntimeException("Paiement non trouvé"));

        return mapToPaymentResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId).stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByUserId(Long userId) {
        return paymentRepository.findByUserId(userId).stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByStatus(String status) {
        try {
            Payment.PaymentStatus paymentStatus = Payment.PaymentStatus.valueOf(status.toUpperCase());
            return paymentRepository.findByStatus(paymentStatus).stream()
                    .map(this::mapToPaymentResponse)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Statut invalide: " + status);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPaymentStatistics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysAgo = now.minusDays(30);

        Double totalRevenue = paymentRepository.getTotalRevenueBetweenDates(thirtyDaysAgo, now);
        Long successfulPayments = paymentRepository.countSuccessfulPayments();
        List<Object[]> paymentsByMethod = paymentRepository.countPaymentsByMethod();

        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalRevenueLast30Days", totalRevenue != null ? totalRevenue : 0.0);
        stats.put("successfulPaymentsCount", successfulPayments != null ? successfulPayments : 0);

        Map<String, Long> methodStats = new java.util.HashMap<>();
        for (Object[] result : paymentsByMethod) {
            String method = ((Payment.PaymentMethod) result[0]).name();
            Long count = (Long) result[1];
            methodStats.put(method, count);
        }
        stats.put("paymentsByMethod", methodStats);

        return stats;
    }

    @Transactional
    public void updatePaymentStatus(PaymentStatusUpdate statusUpdate) {
        Payment payment = paymentRepository.findById(statusUpdate.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Paiement non trouvé"));

        try {
            Payment.PaymentStatus newStatus = Payment.PaymentStatus.valueOf(statusUpdate.getStatus().toUpperCase());
            payment.setStatus(newStatus);

            if (statusUpdate.getGatewayTransactionId() != null) {
                payment.setGatewayTransactionId(statusUpdate.getGatewayTransactionId());
            }

            if (newStatus == Payment.PaymentStatus.SUCCEEDED) {
                payment.setPaidAt(LocalDateTime.now());
            }

            paymentRepository.save(payment);

        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Statut invalide: " + statusUpdate.getStatus());
        }
    }

    private String detectCardBrand(String cardNumber) {
        if (cardNumber.startsWith("4")) {
            return "VISA";
        } else if (cardNumber.startsWith("5")) {
            return "MASTERCARD";
        } else if (cardNumber.startsWith("3")) {
            return "AMEX";
        } else if (cardNumber.startsWith("6")) {
            return "DISCOVER";
        } else {
            return "UNKNOWN";
        }
    }

    private void publishPaymentStatusEvent(Payment payment) {
        try {
            PaymentStatusEvent event = new PaymentStatusEvent();
            event.setPaymentId(payment.getId());
            event.setPaymentReference(payment.getPaymentReference());
            event.setOrderId(payment.getOrderId());
            event.setOrderNumber(payment.getOrderNumber());
            event.setUserId(payment.getUserId());
            event.setAmount(payment.getAmount());
            event.setStatus(payment.getStatus().name());
            event.setTransactionId(payment.getGatewayTransactionId());
            event.setTimestamp(LocalDateTime.now());

            String message = objectMapper.writeValueAsString(event);
            rabbitTemplate.convertAndSend("payment.exchange", "payment.status", message);

            log.info("Événement de statut de paiement publié: {}", payment.getPaymentReference());

        } catch (Exception e) {
            log.error("Erreur lors de la publication de l'événement de statut de paiement", e);
        }
    }

    private void publishRefundEvent(Payment payment) {
        try {
            RefundEvent event = new RefundEvent();
            event.setPaymentId(payment.getId());
            event.setPaymentReference(payment.getPaymentReference());
            event.setOrderId(payment.getOrderId());
            event.setAmount(payment.getAmount());
            event.setRefundReference(payment.getPaymentReference() + "-REFUND");
            event.setTimestamp(LocalDateTime.now());

            String message = objectMapper.writeValueAsString(event);
            rabbitTemplate.convertAndSend("payment.exchange", "payment.refund", message);

            log.info("Événement de remboursement publié: {}", payment.getPaymentReference());

        } catch (Exception e) {
            log.error("Erreur lors de la publication de l'événement de remboursement", e);
        }
    }

    private PaymentResponse mapToPaymentResponse(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setId(payment.getId());
        response.setPaymentReference(payment.getPaymentReference());
        response.setOrderId(payment.getOrderId());
        response.setOrderNumber(payment.getOrderNumber());
        response.setUserId(payment.getUserId());
        response.setUserEmail(payment.getUserEmail());
        response.setAmount(payment.getAmount());
        response.setStatus(payment.getStatus());
        response.setPaymentMethod(payment.getPaymentMethod());
        response.setCardLastFour(payment.getCardLastFour());
        response.setCardBrand(payment.getCardBrand());
        response.setCurrency(payment.getCurrency());
        response.setDescription(payment.getDescription());
        response.setGatewayTransactionId(payment.getGatewayTransactionId());
        response.setCreatedAt(payment.getCreatedAt());
        response.setUpdatedAt(payment.getUpdatedAt());
        response.setPaidAt(payment.getPaidAt());
        return response;
    }
}

@Data
class PaymentStatusEvent {
    private Long paymentId;
    private String paymentReference;
    private Long orderId;
    private String orderNumber;
    private Long userId;
    private Double amount;
    private String status;
    private String transactionId;
    private LocalDateTime timestamp;
}

@Data
class RefundEvent {
    private Long paymentId;
    private String paymentReference;
    private Long orderId;
    private Double amount;
    private String refundReference;
    private LocalDateTime timestamp;
}