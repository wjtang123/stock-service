-- 先执行这个脚本初始化数据库
-- mysql -u root -p < init.sql

CREATE DATABASE IF NOT EXISTS demo_stock DEFAULT CHARSET utf8mb4;
CREATE DATABASE IF NOT EXISTS demo_order DEFAULT CHARSET utf8mb4;

-- ============ demo_stock ============
USE demo_stock;

CREATE TABLE IF NOT EXISTS stock (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    product_id   VARCHAR(50)  NOT NULL UNIQUE,
    product_name VARCHAR(100) NOT NULL,
    quantity     INT          NOT NULL DEFAULT 0,
    version      INT          NOT NULL DEFAULT 0  COMMENT '乐观锁',
    updated_at   DATETIME     DEFAULT NOW() ON UPDATE NOW()
);

INSERT INTO stock(product_id, product_name, quantity) VALUES
    ('P001', 'iPhone 15',   100),
    ('P002', '小米14',      200),
    ('P003', 'MacBook Pro',  50)
ON DUPLICATE KEY UPDATE quantity = quantity;

-- 本地消息表：Outbox 模式的核心
-- 扣库存 + 写这张表 在同一个事务里
-- 定时任务扫 PENDING 的记录发到 MQ
CREATE TABLE IF NOT EXISTS outbox_message (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id    VARCHAR(64)  NOT NULL UNIQUE COMMENT '全局唯一ID，消费端幂等用',
    exchange      VARCHAR(100) NOT NULL,
    routing_key   VARCHAR(100) NOT NULL,
    payload       TEXT         NOT NULL COMMENT 'JSON消息体',
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SENT/FAILED',
    retry_count   INT          NOT NULL DEFAULT 0,
    max_retry     INT          NOT NULL DEFAULT 5,
    next_retry_at DATETIME     DEFAULT NOW(),
    created_at    DATETIME     DEFAULT NOW(),
    sent_at       DATETIME     NULL
);

-- ============ demo_order ============
USE demo_order;

CREATE TABLE IF NOT EXISTS orders (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no     VARCHAR(64)  NOT NULL UNIQUE,
    user_id      VARCHAR(50)  NOT NULL,
    product_id   VARCHAR(50)  NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    quantity     INT          NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'CREATED',
    source_msg_id VARCHAR(64) NOT NULL COMMENT '来源消息ID，幂等用',
    created_at   DATETIME     DEFAULT NOW()
);

-- 幂等控制表：记录已消费的消息ID，防止重复消费
CREATE TABLE IF NOT EXISTS consumed_message (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id  VARCHAR(64) NOT NULL UNIQUE,
    consumed_at DATETIME    DEFAULT NOW()
);
