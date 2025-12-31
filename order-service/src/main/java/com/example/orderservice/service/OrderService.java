package com.example.orderservice.service;



import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.orderservice.client.BookServiceClient;
import com.example.orderservice.client.CartServiceClient;
import com.example.orderservice.dto.*;
import com.example.orderservice.model.Order;
import com.example.orderservice.model.ShippingAddress;
import com.example.orderservice.model.OrderItem;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.OrderItemRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartServiceClient cartServiceClient;
    private final BookServiceClient bookServiceClient;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrderResponse createOrder(OrderRequest orderRequest) {
        log.info("Creating order for user: {}", orderRequest.getUserId());

        try {
            // 1. Récupérer le panier de l'utilisateur
            CartInfo cartInfo = cartServiceClient.getCart(orderRequest.getUserId());

            if (cartInfo == null || cartInfo.getItems() == null || cartInfo.getItems().isEmpty()) {
                throw new RuntimeException("Le panier est vide");
            }

            // 2. Vérifier la disponibilité des livres et calculer le total
            double totalAmount = 0.0;

            for (CartInfo.CartItemInfo cartItem : cartInfo.getItems()) {
                BookInfo bookInfo = bookServiceClient.getBookById(cartItem.getBookId());

                if (bookInfo == null) {
                    throw new RuntimeException("Livre non trouvé: " + cartItem.getBookId());
                }

                if (bookInfo.getStock() < cartItem.getQuantity()) {
                    throw new RuntimeException("Stock insuffisant pour: " + cartItem.getTitle() +
                            ". Disponible: " + bookInfo.getStock() +
                            ", Demandé: " + cartItem.getQuantity());
                }

                totalAmount += cartItem.getPrice() * cartItem.getQuantity();
            }

            // 3. Créer la commande
            Order order = new Order();
            order.setUserId(orderRequest.getUserId());
            order.setUserEmail(orderRequest.getUserEmail());
            order.setUserName(orderRequest.getUserName());
            order.setTotalAmount(totalAmount);
            order.setShippingCost(calculateShippingCost(orderRequest.getShippingAddress()));
            order.setTaxAmount(calculateTax(totalAmount));

            // Adresse de livraison
            // Adresse de livraison
            ShippingAddress shippingAddress = new ShippingAddress();
            shippingAddress.setStreet(orderRequest.getShippingAddress().getStreet());
            shippingAddress.setCity(orderRequest.getShippingAddress().getCity());
            shippingAddress.setPostalCode(orderRequest.getShippingAddress().getPostalCode());
            shippingAddress.setCountry(orderRequest.getShippingAddress().getCountry());
            shippingAddress.setPhone(orderRequest.getShippingAddress().getPhone());
            order.setShippingAddress(shippingAddress);


            order.setPaymentMethod(orderRequest.getPaymentMethod());
            order.setNotes(orderRequest.getNotes());
            order.setStatus(Order.OrderStatus.PENDING);
            order.setPaymentStatus(Order.PaymentStatus.PENDING);

            // 4. Ajouter les articles et mettre à jour le stock
            for (CartInfo.CartItemInfo cartItem : cartInfo.getItems()) {
                BookInfo bookInfo = bookServiceClient.getBookById(cartItem.getBookId());

                OrderItem orderItem = new OrderItem();
                orderItem.setBookId(cartItem.getBookId());
                orderItem.setTitle(cartItem.getTitle());
                orderItem.setAuthor(cartItem.getAuthor());
                orderItem.setPrice(cartItem.getPrice());
                orderItem.setQuantity(cartItem.getQuantity());
                orderItem.setIsbn(bookInfo.getIsbn());

                order.addItem(orderItem);

                // Mettre à jour le stock
                int newStock = bookInfo.getStock() - cartItem.getQuantity();
                bookServiceClient.updateStock(cartItem.getBookId(), newStock);
            }

            // 5. Sauvegarder la commande
            Order savedOrder = orderRepository.save(order);

            // 6. Publier un événement pour le paiement
            publishPaymentEvent(savedOrder);

            // 7. Publier un événement pour vider le panier
            publishClearCartEvent(orderRequest.getUserId());

            log.info("Commande créée avec succès: {}", savedOrder.getOrderNumber());

            return mapToOrderResponse(savedOrder);

        } catch (Exception e) {
            log.error("Erreur lors de la création de la commande", e);
            throw new RuntimeException("Erreur lors de la création de la commande: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Commande non trouvée"));

        return mapToOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Commande non trouvée"));

        return mapToOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByStatus(String status) {
        try {
            Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
            return orderRepository.findByStatus(orderStatus).stream()
                    .map(this::mapToOrderResponse)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Statut invalide: " + status);
        }
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Commande non trouvée"));

        try {
            Order.OrderStatus newStatus = Order.OrderStatus.valueOf(status.toUpperCase());
            order.setStatus(newStatus);

            // Mettre à jour les timestamps
            switch (newStatus) {
                case SHIPPED:
                    order.setShippedAt(LocalDateTime.now());
                    break;
                case DELIVERED:
                    order.setDeliveredAt(LocalDateTime.now());
                    break;
                case CANCELLED:
                    restoreStock(order);
                    break;
            }

            Order updatedOrder = orderRepository.save(order);

            // Publier un événement de mise à jour
            publishOrderStatusEvent(updatedOrder);

            return mapToOrderResponse(updatedOrder);

        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Statut invalide: " + status);
        }
    }

    @Transactional
    public OrderResponse updatePaymentStatus(Long orderId, String paymentStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Commande non trouvée"));

        try {
            Order.PaymentStatus newPaymentStatus = Order.PaymentStatus.valueOf(paymentStatus.toUpperCase());
            order.setPaymentStatus(newPaymentStatus);

            if (newPaymentStatus == Order.PaymentStatus.PAID) {
                order.setPaidAt(LocalDateTime.now());
                order.setStatus(Order.OrderStatus.CONFIRMED);
            }

            Order updatedOrder = orderRepository.save(order);

            return mapToOrderResponse(updatedOrder);

        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Statut de paiement invalide: " + paymentStatus);
        }
    }

    @Transactional
    public OrderResponse updatePaymentReference(Long orderId, String paymentReference) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Commande non trouvée"));

        order.setPaymentReference(paymentReference);
        Order updatedOrder = orderRepository.save(order);

        return mapToOrderResponse(updatedOrder);
    }

    @Transactional
    public OrderResponse updateTrackingNumber(Long orderId, String trackingNumber) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Commande non trouvée"));

        order.setTrackingNumber(trackingNumber);
        Order updatedOrder = orderRepository.save(order);

        return mapToOrderResponse(updatedOrder);
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Commande non trouvée"));

        if (order.getStatus() == Order.OrderStatus.SHIPPED ||
                order.getStatus() == Order.OrderStatus.DELIVERED) {
            throw new RuntimeException("Impossible d'annuler une commande déjà expédiée ou livrée");
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setPaymentStatus(Order.PaymentStatus.CANCELLED);

        // Restituer le stock
        restoreStock(order);

        Order updatedOrder = orderRepository.save(order);

        return mapToOrderResponse(updatedOrder);
    }

    @Transactional(readOnly = true)
    public Double getUserTotalSpent(Long userId) {
        Double total = orderRepository.getTotalSpentByUser(userId);
        return total != null ? total : 0.0;
    }

    @Transactional(readOnly = true)
    public Long getUserOrderCount(Long userId) {
        return orderRepository.countByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<OrderItemResponse> getOrderItems(Long orderId) {
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        return items.stream()
                .map(this::mapToOrderItemResponse)
                .collect(Collectors.toList());
    }

    private Double calculateShippingCost(OrderRequest.ShippingAddressDto address) {
        // Logique simplifiée pour le calcul des frais de port
        // Dans une application réelle, utiliser un service de calcul de frais de port
        return 4.99; // Frais fixes pour la démonstration
    }

    private Double calculateTax(Double amount) {
        // Taux de TVA à 20% pour la France
        return amount * 0.20;
    }

    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            try {
                BookInfo bookInfo = bookServiceClient.getBookById(item.getBookId());
                if (bookInfo != null) {
                    int newStock = bookInfo.getStock() + item.getQuantity();
                    bookServiceClient.updateStock(item.getBookId(), newStock);
                }
            } catch (Exception e) {
                log.error("Erreur lors de la restauration du stock pour le livre: {}", item.getBookId(), e);
            }
        }
    }

    private void publishPaymentEvent(Order order) {
        try {
            PaymentEvent event = new PaymentEvent();
            event.setOrderId(order.getId());
            event.setOrderNumber(order.getOrderNumber());
            event.setUserId(order.getUserId());
            event.setUserEmail(order.getUserEmail());
            event.setAmount(order.getFinalAmount());
            event.setPaymentMethod(order.getPaymentMethod());
            event.setTimestamp(LocalDateTime.now());

            String message = objectMapper.writeValueAsString(event);
            rabbitTemplate.convertAndSend("order.exchange", "order.payment", message);

            log.info("Événement de paiement publié pour la commande: {}", order.getOrderNumber());

        } catch (Exception e) {
            log.error("Erreur lors de la publication de l'événement de paiement", e);
        }
    }

    private void publishClearCartEvent(Long userId) {
        try {
            ClearCartEvent event = new ClearCartEvent();
            event.setUserId(userId);
            event.setTimestamp(LocalDateTime.now());

            String message = objectMapper.writeValueAsString(event);
            rabbitTemplate.convertAndSend("order.exchange", "order.clear.cart", message);

            log.info("Événement de vidage de panier publié pour l'utilisateur: {}", userId);

        } catch (Exception e) {
            log.error("Erreur lors de la publication de l'événement de vidage de panier", e);
        }
    }

    private void publishOrderStatusEvent(Order order) {
        try {
            OrderStatusEvent event = new OrderStatusEvent();
            event.setOrderId(order.getId());
            event.setOrderNumber(order.getOrderNumber());
            event.setUserId(order.getUserId());
            event.setStatus(order.getStatus().name());
            event.setTimestamp(LocalDateTime.now());

            String message = objectMapper.writeValueAsString(event);
            rabbitTemplate.convertAndSend("order.exchange", "order.status", message);

            log.info("Événement de statut de commande publié: {}", order.getOrderNumber());

        } catch (Exception e) {
            log.error("Erreur lors de la publication de l'événement de statut", e);
        }
    }

    private OrderResponse mapToOrderResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setUserId(order.getUserId());
        response.setUserEmail(order.getUserEmail());
        response.setUserName(order.getUserName());
        response.setTotalAmount(order.getTotalAmount());
        response.setTaxAmount(order.getTaxAmount());
        response.setShippingCost(order.getShippingCost());
        response.setFinalAmount(order.getFinalAmount());
        response.setStatus(order.getStatus());
        response.setPaymentStatus(order.getPaymentStatus());
        response.setPaymentMethod(order.getPaymentMethod());
        response.setPaymentReference(order.getPaymentReference());
        response.setNotes(order.getNotes());
        response.setTrackingNumber(order.getTrackingNumber());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());
        response.setPaidAt(order.getPaidAt());
        response.setShippedAt(order.getShippedAt());
        response.setDeliveredAt(order.getDeliveredAt());

        // Adresse de livraison
        OrderResponse.ShippingAddressDto addressDto = new OrderResponse.ShippingAddressDto();
        addressDto.setStreet(order.getShippingAddress().getStreet());
        addressDto.setCity(order.getShippingAddress().getCity());
        addressDto.setPostalCode(order.getShippingAddress().getPostalCode());
        addressDto.setCountry(order.getShippingAddress().getCountry());
        addressDto.setPhone(order.getShippingAddress().getPhone());
        response.setShippingAddress(addressDto);

        // Articles
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(this::mapToOrderItemResponse)
                .collect(Collectors.toList());
        response.setItems(itemResponses);

        return response;
    }

    private OrderItemResponse mapToOrderItemResponse(OrderItem item) {
        OrderItemResponse response = new OrderItemResponse();
        response.setId(item.getId());
        response.setBookId(item.getBookId());
        response.setTitle(item.getTitle());
        response.setAuthor(item.getAuthor());
        response.setIsbn(item.getIsbn());
        response.setPrice(item.getPrice());
        response.setQuantity(item.getQuantity());
        response.setTotalPrice(item.getTotalPrice());
        return response;
    }
}

// Classes d'événements
@Data
class PaymentEvent {
    private Long orderId;
    private String orderNumber;
    private Long userId;
    private String userEmail;
    private Double amount;
    private String paymentMethod;
    private LocalDateTime timestamp;
}

@Data
class ClearCartEvent {
    private Long userId;
    private LocalDateTime timestamp;
}

@Data
class OrderStatusEvent {
    private Long orderId;
    private String orderNumber;
    private Long userId;
    private String status;
    private LocalDateTime timestamp;
}