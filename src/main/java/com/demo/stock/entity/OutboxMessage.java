package com.demo.stock.entity;

import java.time.LocalDateTime;

public class OutboxMessage {
    public static final String PENDING = "PENDING";
    public static final String SENT    = "SENT";
    public static final String FAILED  = "FAILED";

    private Long          id;
    private String        messageId;
    private String        exchange;
    private String        routingKey;
    private String        payload;
    private String        status;
    private Integer       retryCount;
    private Integer       maxRetry;
    private LocalDateTime nextRetryAt;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMessageId() { return messageId; }
    public void setMessageId(String m) { this.messageId = m; }
    public String getExchange() { return exchange; }
    public void setExchange(String e) { this.exchange = e; }
    public String getRoutingKey() { return routingKey; }
    public void setRoutingKey(String r) { this.routingKey = r; }
    public String getPayload() { return payload; }
    public void setPayload(String p) { this.payload = p; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer r) { this.retryCount = r; }
    public Integer getMaxRetry() { return maxRetry; }
    public void setMaxRetry(Integer m) { this.maxRetry = m; }
    public LocalDateTime getNextRetryAt() { return nextRetryAt; }
    public void setNextRetryAt(LocalDateTime t) { this.nextRetryAt = t; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime t) { this.createdAt = t; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime t) { this.sentAt = t; }
}
