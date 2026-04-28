package com.demo.stock.service;

import com.demo.stock.config.RabbitConfig;
import com.demo.stock.entity.OutboxMessage;
import com.demo.stock.entity.Stock;
import com.demo.stock.entity.StockDeductedEvent;
import com.demo.stock.mapper.OutboxMessageMapper;
import com.demo.stock.mapper.StockMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class StockService {

    @Autowired private StockMapper       stockMapper;
    @Autowired private OutboxMessageMapper outboxMapper;

    private final ObjectMapper json = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    /**
     * 扣减库存（核心方法）
     *
     * 关键点：扣库存 + 写本地消息表 在同一个 @Transactional 事务里！
     *
     * 这保证了两件事要么同时成功，要么同时失败：
     *   ✓ 扣了库存 → 消息一定会被记录 → 最终一定会发到MQ → 订单一定会被创建
     *   ✓ 扣库存失败回滚 → 消息也不会写入 → 不会产生孤立订单
     *
     * 不使用直接发MQ的原因：
     *   如果先扣库存再发MQ，MQ发送那一刻网络断了：
     *   → 库存已扣，但订单没创建，数据不一致！
     *
     *   Outbox 模式把"发MQ"变成"写本地表"，和扣库存在同一事务，
     *   本地事务比网络调用可靠得多。
     */
    @Transactional(rollbackFor = Exception.class)
    public String deductStock(String userId, String productId, int quantity) throws Exception {

        // 1. 查询库存（带版本号，用于乐观锁）
        Stock stock = stockMapper.findByProductId(productId);
        if (stock == null) {
            throw new RuntimeException("商品不存在: " + productId);
        }
        if (stock.getQuantity() < quantity) {
            throw new RuntimeException("库存不足! 当前库存=" + stock.getQuantity()
                    + ", 需要=" + quantity);
        }

        // 2. 乐观锁扣减库存（并发时只有一个请求能成功，其余返回0）
        int affected = stockMapper.deductWithVersion(productId, quantity, stock.getVersion());
        if (affected == 0) {
            // 版本号不匹配，说明被并发修改，抛异常让调用方重试
            throw new RuntimeException("库存更新冲突（乐观锁），请重试");
        }

        // 3. 构建消息事件
        String messageId = UUID.randomUUID().toString().replace("-", "");
        StockDeductedEvent event = new StockDeductedEvent(
                messageId, userId, productId, stock.getProductName(), quantity);

        // 4. 写入本地消息表（和步骤2在同一事务！）
        OutboxMessage outbox = new OutboxMessage();
        outbox.setMessageId(messageId);
        outbox.setExchange(RabbitConfig.STOCK_EXCHANGE);
        outbox.setRoutingKey(RabbitConfig.STOCK_DEDUCTED_KEY);
        outbox.setPayload(json.writeValueAsString(event));
        outbox.setStatus(OutboxMessage.PENDING);
        outbox.setMaxRetry(5);
        outboxMapper.insert(outbox);

        System.out.println("[StockService] 库存扣减成功，消息已写入本地表: "
                + productId + " x" + quantity + " msgId=" + messageId);

        // 事务提交后，定时任务会扫描消息表并发送到MQ
        return messageId;
    }
}
