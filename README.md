支持分布式场景的本地消息表组件，确保消息发送的可靠性和最终一致性
设计思路来自 https://github.com/meoying/local-msg-go

## 设计原理

本地消息表是一种实现分布式事务的方案，其基本思路是：
在本地事务中，同时完成业务操作和消息持久化
通过补偿任务，确保消息最终被发送到消息中间件

## 关键特性
支持分库场景
消息可靠投递
异步补偿机制
可配置的重试策略
基于SPI的扩展机制

## 多数据源配置
```
datasource.count=2

datasource.0.url=jdbc:mysql://localhost:3306/local_msg_db_0
datasource.0.username=root
datasource.0.password=root

datasource.1.url=jdbc:mysql://localhost:3306/local_msg_db_1
datasource.1.username=root
datasource.1.password=root
```

```
public class Example {
    public void businessOperation() {
        try (LocalMessageInitializer messageTable = new LocalMessageInitializer.Builder()
                .build()) {
            messageTable.start();

            // 创建消息
            Message message = Message.builder()
                    .topic("order-topic")
                    .key("ORDER_" + orderId)
                    .content(orderJson)
                    .build();

            // 执行业务和消息处理
            messageTable.executeBusinessWithMessage(
                    conn -> {
                        // 执行业务逻辑
                        orderService.createOrder(order);
                        return null;
                    },
                    message
            );
        }
    }
}

```
