package com.example.orderservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchanges
    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange("order.exchange");
    }

    @Bean
    public DirectExchange paymentStatusExchange() {
        return new DirectExchange("payment.status.exchange");
    }

    // Queues
    @Bean
    public Queue orderPaymentQueue() {
        return new Queue("order.payment.queue", true);
    }

    @Bean
    public Queue orderStatusQueue() {
        return new Queue("order.status.queue", true);
    }

    @Bean
    public Queue paymentStatusQueue() {
        return new Queue("payment.status.queue", true);
    }

    @Bean
    public Queue orderAdminQueue() {
        return new Queue("order.admin.queue", true);
    }

    // Bindings
    @Bean
    public Binding orderPaymentBinding() {
        return BindingBuilder.bind(orderPaymentQueue())
                .to(orderExchange())
                .with("order.payment");
    }

    @Bean
    public Binding orderStatusBinding() {
        return BindingBuilder.bind(orderStatusQueue())
                .to(orderExchange())
                .with("order.status");
    }

    @Bean
    public Binding orderClearCartBinding() {
        return BindingBuilder.bind(new Queue("cart.clear.queue", true))
                .to(orderExchange())
                .with("order.clear.cart");
    }

    @Bean
    public Binding paymentStatusBinding() {
        return BindingBuilder.bind(paymentStatusQueue())
                .to(paymentStatusExchange())
                .with("payment.status");
    }

    // Message Converter
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

    @Bean
    Queue cartClearQueue() {
        return new Queue("cart.clear.queue", true); // true = durable
    }

}