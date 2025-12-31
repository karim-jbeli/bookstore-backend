package com.example.orderservice.service;

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
    private final OrderService orderService;

    @RabbitListener(queues = "payment.status.queue")
    public void handlePaymentStatusMessage(String message) {
        try {
            PaymentStatusEvent event = objectMapper.readValue(message, PaymentStatusEvent.class);
            log.info("Reçu événement de statut de paiement: {}", event);

            // Mettre à jour le statut de paiement de la commande
            orderService.updatePaymentStatus(event.getOrderId(), event.getStatus());

            if ("SUCCEEDED".equals(event.getStatus())) {
                orderService.updatePaymentReference(event.getOrderId(), event.getPaymentReference());
            }

        } catch (Exception e) {
            log.error("Erreur lors du traitement de l'événement de statut de paiement", e);
        }
    }

    @RabbitListener(queues = "order.admin.queue")
    public void handleAdminMessage(String message) {
        try {
            log.info("Message admin reçu: {}", message);
            // Traiter les messages administratifs si nécessaire
        } catch (Exception e) {
            log.error("Erreur lors du traitement du message admin", e);
        }
    }
}

@Data
class PaymentStatusEvent {
    private Long orderId;
    private String orderNumber;
    private String status;
    private String paymentReference;
    private LocalDateTime timestamp;
}