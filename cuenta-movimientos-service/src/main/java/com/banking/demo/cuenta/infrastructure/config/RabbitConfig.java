package com.banking.demo.cuenta.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class RabbitConfig {

    @Value("${banking.rabbitmq.exchange:banking.exchange}")
    private String exchangeName;

    @Value("${banking.rabbitmq.queue:cuenta.client-events}")
    private String queueName;

    @Value("${banking.rabbitmq.routing-key:client.#}")
    private String routingKey;

    @Bean
    public TopicExchange bankingExchange() {
        log.info("Registrando TopicExchange [{}] para eventos de mensajeria", exchangeName);
        return new TopicExchange(exchangeName);
    }

    @Bean
    public Queue clientEventsQueue() {
        log.info("Registrando cola durable [{}] para consumo de eventos de cliente", queueName);
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    public Binding clientEventsBinding(Queue clientEventsQueue, TopicExchange bankingExchange) {
        log.info("Creando binding de cola [{}] con exchange [{}] bajo routing-key [{}]",
                queueName, exchangeName, routingKey);
        return BindingBuilder.bind(clientEventsQueue).to(bankingExchange).with(routingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        return factory;
    }
}
