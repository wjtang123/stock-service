package com.demo.stock.mapper;

import com.demo.stock.entity.OutboxMessage;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OutboxMessageMapper {

    @Insert("INSERT INTO outbox_message(message_id, exchange, routing_key, payload, status, retry_count, max_retry, next_retry_at) " +
            "VALUES(#{messageId}, #{exchange}, #{routingKey}, #{payload}, #{status}, 0, #{maxRetry}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OutboxMessage msg);

    /** 查询到期的 PENDING 消息（next_retry_at <= NOW()），每次最多取20条 */
    @Select("SELECT * FROM outbox_message WHERE status = 'PENDING' AND next_retry_at <= NOW() " +
            "ORDER BY created_at LIMIT 20")
    List<OutboxMessage> findPendingMessages();

    /** 标记发送成功 */
    @Update("UPDATE outbox_message SET status='SENT', sent_at=NOW() WHERE id=#{id}")
    int markSent(@Param("id") Long id);

    /**
     * 发送失败：重试次数+1，计算下次重试时间（指数退避），超过最大次数标记FAILED
     * 指数退避：第1次失败等10s，第2次等20s，第3次等40s...
     */
    @Update("UPDATE outbox_message SET " +
            "  retry_count = retry_count + 1, " +
            "  next_retry_at = DATE_ADD(NOW(), INTERVAL POWER(2, retry_count) * 10 SECOND), " +
            "  status = CASE WHEN retry_count + 1 >= max_retry THEN 'FAILED' ELSE 'PENDING' END " +
            "WHERE id = #{id}")
    int markRetry(@Param("id") Long id);
}
