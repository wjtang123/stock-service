package com.demo.stock.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // Exchange / Queue / RoutingKey 名称常量
    public static final String STOCK_EXCHANGE       = "stock.exchange";
    public static final String STOCK_DEDUCTED_KEY   = "stock.deducted";
    public static final String STOCK_DEDUCTED_QUEUE = "stock.deducted.queue";
    public static final String DEAD_EXCHANGE        = "stock.dead.exchange";
    public static final String DEAD_QUEUE           = "stock.dead.queue";
    public static final String DEAD_ROUTING_KEY     = "stock.dead";

    /* ---- Exchange ---- */
    @Bean public DirectExchange stockExchange() {
        return new DirectExchange(STOCK_EXCHANGE, true, false);
    }
    @Bean public DirectExchange deadExchange() {
        return new DirectExchange(DEAD_EXCHANGE, true, false);
    }

    /* ---- Queue ---- */
    @Bean
    public Queue stockDeductedQueue() {
        // 消费失败超过重试次数 → 自动转入死信队列，不丢消息
        return QueueBuilder.durable(STOCK_DEDUCTED_QUEUE)
                .withArgument("x-dead-letter-exchange", DEAD_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DEAD_ROUTING_KEY)
                .build();
    }
    @Bean public Queue deadQueue() {
        return QueueBuilder.durable(DEAD_QUEUE).build();
    }

    /* ---- Binding ---- */
    @Bean
    public Binding stockBinding(Queue stockDeductedQueue, DirectExchange stockExchange) {
        return BindingBuilder.bind(stockDeductedQueue).to(stockExchange).with(STOCK_DEDUCTED_KEY);
    }
    @Bean
    public Binding deadBinding(Queue deadQueue, DirectExchange deadExchange) {
        return BindingBuilder.bind(deadQueue).to(deadExchange).with(DEAD_ROUTING_KEY);
    }

    /* ---- 消息序列化（Java对象 ↔ JSON） ---- */
    @Bean public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        template.setMandatory(true);

        // 消息到达 Exchange 后回调（ack=true 表示成功）
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (correlationData == null) return;
            if (ack) {
                System.out.println("[MQ-Confirm] ✓ 消息已到达Exchange, id=" + correlationData.getId());
            } else {
                System.err.println("[MQ-Confirm] ✗ 消息未到达Exchange! id="
                        + correlationData.getId() + " cause=" + cause);
            }
        });

        // 消息路由失败（Exchange找不到Queue）回调
        template.setReturnCallback((msg, code, text, exchange, routingKey) ->
                System.err.println("[MQ-Return] 路由失败! exchange=" + exchange
                        + " routingKey=" + routingKey + " reply=" + text));

        return template;
    }
}
