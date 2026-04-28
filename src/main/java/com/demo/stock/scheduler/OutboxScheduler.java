package com.demo.stock.scheduler;

import com.demo.stock.entity.OutboxMessage;
import com.demo.stock.mapper.OutboxMessageMapper;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 本地消息表扫描器（Outbox Relay）
 *
 * 职责：每5秒扫一次 outbox_message 表，
 *       把 status=PENDING 且到了重试时间的消息发到 RabbitMQ。
 *
 * 为什么用定时任务而不是发完事务后立刻发？
 *   事务提交后立刻发：如果应用在提交事务和发MQ之间崩了，消息丢失。
 *   定时任务：就算崩了重启后扫表还能找到 PENDING 消息重发，更可靠。
 */
@Component
public class OutboxScheduler {

    @Autowired private OutboxMessageMapper outboxMapper;
    @Autowired private RabbitTemplate      rabbitTemplate;

    @Scheduled(fixedDelay = 5000)   // 每5秒执行一次（上次结束后5秒再执行）
    public void relayMessages() {

        List<OutboxMessage> pending = outboxMapper.findPendingMessages();
        if (pending.isEmpty()) return;

        System.out.println("[Outbox] 扫描到 " + pending.size() + " 条待发消息");

        for (OutboxMessage msg : pending) {
            try {
                // CorrelationData 携带消息ID，ConfirmCallback 里用来对应消息
                CorrelationData correlation = new CorrelationData(msg.getMessageId());

                rabbitTemplate.convertAndSend(
                        msg.getExchange(),
                        msg.getRoutingKey(),
                        msg.getPayload(),   // JSON字符串
                        correlation
                );

                // 发送成功（只是投递到Broker，不代表消费者处理成功）
                // 立刻标记 SENT，减少重复发送
                // 注意：ConfirmCallback 是异步的，这里是同步标记
                outboxMapper.markSent(msg.getId());
                System.out.println("[Outbox] ✓ 发送成功 msgId=" + msg.getMessageId());

            } catch (Exception e) {
                // 发送失败：更新重试次数和下次重试时间
                outboxMapper.markRetry(msg.getId());
                System.err.println("[Outbox] ✗ 发送失败 msgId=" + msg.getMessageId()
                        + " retryCount=" + msg.getRetryCount() + " error=" + e.getMessage());
            }
        }
    }
}
