package com.example.paymentservice.service;

import com.example.paymentservice.dto.PaymentRequest.CreditCardInfo;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
public class PaymentGatewayService {

    @Value("${payment.gateway.simulate:true}")
    private boolean simulateGateway;

    @Value("${payment.gateway.success.rate:0.95}")
    private double successRate;

    @Value("${payment.gateway.api.key}")
    private String apiKey;

    @Value("${payment.gateway.secret.key}")
    private String secretKey;

    public PaymentGatewayResponse processCardPayment(CreditCardInfo cardInfo, Double amount, String reference) {
        log.info("Traitement du paiement par carte: {} pour le montant: {}", reference, amount);

        if (simulateGateway) {
            // Simulation de la passerelle de paiement
            try {
                // Simuler un délai de traitement
                Thread.sleep(ThreadLocalRandom.current().nextInt(500, 1500));

                // Valider la carte
                if (!isValidCard(cardInfo)) {
                    log.warn("Carte invalide: {}", reference);
                    return PaymentGatewayResponse.failure("Carte invalide ou expirée");
                }

                // Déterminer si le paiement réussit selon le taux de succès
                boolean success = ThreadLocalRandom.current().nextDouble() < successRate;

                if (success) {
                    String transactionId = "CARD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                    log.info("Paiement par carte réussi: {}, transaction: {}", reference, transactionId);

                    return PaymentGatewayResponse.success(
                            transactionId,
                            "Paiement par carte traité avec succès. Montant: " + amount + " EUR"
                    );
                } else {
                    log.warn("Paiement par carte échoué: {}", reference);
                    return PaymentGatewayResponse.failure("Paiement refusé par la banque");
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return PaymentGatewayResponse.failure("Traitement du paiement interrompu");
            } catch (Exception e) {
                log.error("Erreur lors du traitement du paiement par carte", e);
                return PaymentGatewayResponse.failure("Erreur technique: " + e.getMessage());
            }
        } else {
            // Intégration avec une vraie passerelle de paiement
            // À implémenter selon le fournisseur choisi (Stripe, PayPal, etc.)
            throw new UnsupportedOperationException("Passerelle de paiement réelle non implémentée");
        }
    }

    public PaymentGatewayResponse processPaypalPayment(String paypalEmail, Double amount, String reference) {
        log.info("Traitement du paiement PayPal: {} pour le montant: {}", reference, amount);

        if (simulateGateway) {
            try {
                Thread.sleep(ThreadLocalRandom.current().nextInt(1000, 2000));

                if (paypalEmail == null || !paypalEmail.contains("@")) {
                    return PaymentGatewayResponse.failure("Email PayPal invalide");
                }

                boolean success = ThreadLocalRandom.current().nextDouble() < successRate;

                if (success) {
                    String transactionId = "PP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                    log.info("Paiement PayPal réussi: {}, transaction: {}", reference, transactionId);

                    return PaymentGatewayResponse.success(
                            transactionId,
                            "Paiement PayPal traité avec succès. Montant: " + amount + " EUR"
                    );
                } else {
                    log.warn("Paiement PayPal échoué: {}", reference);
                    return PaymentGatewayResponse.failure("Échec du paiement PayPal");
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return PaymentGatewayResponse.failure("Traitement PayPal interrompu");
            } catch (Exception e) {
                log.error("Erreur lors du traitement PayPal", e);
                return PaymentGatewayResponse.failure("Erreur technique PayPal: " + e.getMessage());
            }
        } else {
            throw new UnsupportedOperationException("Intégration PayPal réelle non implémentée");
        }
    }

    public PaymentGatewayResponse processBankTransfer(Double amount, String reference) {
        log.info("Traitement du virement bancaire: {} pour le montant: {}", reference, amount);

        // Les virements bancaires sont toujours considérés comme réussis en simulation
        // Dans un environnement réel, cela nécessiterait une confirmation manuelle
        String transactionId = "BANK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.info("Virement bancaire initié: {}, transaction: {}", reference, transactionId);

        return PaymentGatewayResponse.success(
                transactionId,
                "Virement bancaire initié. Veuillez effectuer le transfert. " +
                        "Montant: " + amount + " EUR. Référence: " + reference
        );
    }

    public PaymentGatewayResponse processRefund(String transactionId, Double amount, String reference) {
        log.info("Traitement du remboursement: {} pour la transaction: {}", reference, transactionId);

        if (simulateGateway) {
            try {
                Thread.sleep(ThreadLocalRandom.current().nextInt(800, 1200));

                String refundId = "REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                log.info("Remboursement traité: {}, remboursement ID: {}", reference, refundId);

                return PaymentGatewayResponse.success(
                        refundId,
                        "Remboursement traité avec succès. Montant: " + amount + " EUR"
                );

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return PaymentGatewayResponse.failure("Traitement du remboursement interrompu");
            } catch (Exception e) {
                log.error("Erreur lors du remboursement", e);
                return PaymentGatewayResponse.failure("Erreur technique de remboursement: " + e.getMessage());
            }
        } else {
            throw new UnsupportedOperationException("Remboursement réel non implémenté");
        }
    }

    private boolean isValidCard(CreditCardInfo cardInfo) {
        // Validation simple de la carte
        try {
            // Vérifier la date d'expiration
            int month = Integer.parseInt(cardInfo.getExpiryMonth());
            int year = Integer.parseInt(cardInfo.getExpiryYear());

            if (month < 1 || month > 12) {
                return false;
            }

            // Vérifier si la carte est expirée
            int currentYear = java.time.Year.now().getValue();
            int currentMonth = java.time.MonthDay.now().getMonthValue();

            if (year < currentYear) {
                return false;
            }
            if (year == currentYear && month < currentMonth) {
                return false;
            }

            // Vérifier le numéro de carte avec l'algorithme de Luhn
            return isValidLuhn(cardInfo.getCardNumber());

        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidLuhn(String cardNumber) {
        int sum = 0;
        boolean alternate = false;

        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));

            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit % 10) + 1;
                }
            }

            sum += digit;
            alternate = !alternate;
        }

        return (sum % 10 == 0);
    }
}

@Data
class PaymentGatewayResponse {
    private boolean success;
    private String transactionId;
    private String response;

    public static PaymentGatewayResponse success(String transactionId, String response) {
        PaymentGatewayResponse r = new PaymentGatewayResponse();
        r.setSuccess(true);
        r.setTransactionId(transactionId);
        r.setResponse(response);
        return r;
    }

    public static PaymentGatewayResponse failure(String response) {
        PaymentGatewayResponse r = new PaymentGatewayResponse();
        r.setSuccess(false);
        r.setTransactionId(null);
        r.setResponse(response);
        return r;
    }
}