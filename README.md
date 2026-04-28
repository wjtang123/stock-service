**启动步骤**

**第一步：启动基础设施**
```bash
# RabbitMQ（如果没装Docker，去官网下安装包也行）
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```
Nacos 用你现有的。

**第二步：初始化数据库**
```shell
mysql -u root -p < init.sql
```

**第三步：改两个 yml 里的 MySQL 密码，然后用 IDEA 分别打开两个项目，依次启动 `StockApplication` 和 `OrderApplication`。**

**第四步：测试**
```shell
# 扣库存（触发整个流程）
curl -X POST "http://localhost:8081/stock/deduct?userId=U001&productId=P001&quantity=2"

# 等5秒（定时任务发消息），查订单
curl http://localhost:8082/orders

# 查消息表状态（PENDING→SENT说明发送成功）
curl http://localhost:8081/stock/outbox/pending

# 查库存是否减少
curl http://localhost:8081/stock/P001
```

**调试关键点**
调试时控制台会打印完整链路，看这几行确认每个阶段是否正常：
```bash
[StockService] 库存扣减成功，消息已写入本地表: P001 x2 msgId=abc123
[Outbox] ✓ 发送成功 msgId=abc123
[Consumer] 收到消息: {"messageId":"abc123",...}
[OrderService] ✓ 订单创建成功: ORD-XXXXXXXX msgId=abc123
[Consumer] ✓ ACK 消息 deliveryTag=1
```
**验证幂等**：用同一个 msgId 重复发消息，订单只会创建一次：
```bash
# 在 RabbitMQ 管理界面 (localhost:15672) → Queues → stock.deducted.queue
# → Publish message，手动发一条已消费过的消息，看日志打印"消息已消费，跳过"
```
**验证可靠性**：在 StockService.deductStock() 写消息表之后、事务提交前，把 RabbitMQ 停掉。重启 RabbitMQ 后，定时任务会自动把未发送的消息重新发出去。

