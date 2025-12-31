package com.example.paymentservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitMQListener {

    private final ObjectMapper objectMapper;
    private final PaymentService paymentService;

    @RabbitListener(queues = "order.payment.queue")
    public void handleOrderPaymentMessage(String message) {
        try {
            OrderPaymentEvent event = objectMapper.readValue(message, OrderPaymentEvent.class);
            log.info("Reçu événement de paiement de commande: {}", event);

            // Ici, vous pourriez automatiquement créer une demande de paiement
            // ou simplement enregistrer l'événement

        } catch (Exception e) {
            log.error("Erreur lors du traitement de l'événement de paiement de commande", e);
        }
    }

    @RabbitListener(queues = "payment.admin.queue")
    public void handleAdminMessage(String message) {
        try {
            log.info("Message admin reçu dans le service de paiement: {}", message);
            // Traiter les messages administratifs si nécessaire
        } catch (Exception e) {
            log.error("Erreur lors du traitement du message admin", e);
        }
    }
}

@Data
class OrderPaymentEvent {
    private Long orderId;
    private String orderNumber;
    private Long userId;
    private String userEmail;
    private Double amount;
    private String paymentMethod;
    private LocalDateTime timestamp;
}