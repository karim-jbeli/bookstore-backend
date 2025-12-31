package com.example.paymentservice.config;

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
    public DirectExchange paymentExchange() {
        return new DirectExchange("payment.exchange");
    }

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange("order.exchange");
    }

    // Queues
    @Bean
    public Queue paymentStatusQueue() {
        return new Queue("payment.status.queue", true);
    }

    @Bean
    public Queue paymentRefundQueue() {
        return new Queue("payment.refund.queue", true);
    }

    @Bean
    public Queue orderPaymentQueue() {
        return new Queue("order.payment.queue", true);
    }

    @Bean
    public Queue paymentAdminQueue() {
        return new Queue("payment.admin.queue", true);
    }

    // Bindings
    @Bean
    public Binding paymentStatusBinding() {
        return BindingBuilder.bind(paymentStatusQueue())
                .to(paymentExchange())
                .with("payment.status");
    }

    @Bean
    public Binding paymentRefundBinding() {
        return BindingBuilder.bind(paymentRefundQueue())
                .to(paymentExchange())
                .with("payment.refund");
    }

    @Bean
    public Binding orderPaymentBinding() {
        return BindingBuilder.bind(orderPaymentQueue())
                .to(orderExchange())
                .with("order.payment");
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
}