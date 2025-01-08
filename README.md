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

## 分布式锁与补偿任务
### 表字段复用
考虑到已经有一个local_msg 表用于存储消息，直接复用该表来实现分布式锁
字段复用对应关系：<br>
key: 锁的唯一标识，使用 "lock:" 前缀区分锁与消息 <br>
data: 存储锁持有者的唯一标识（UUID）<br>
send_times: 作为锁的版本号使用 <br>
status: 新增锁的状态值（LOCK_HOLDING=3, LOCK_RELEASED=4） <br>
update_time: 作为锁的过期时间使用 <br>
create_time: 记录锁的创建时间
### 补偿任务设计
每分钟调度一次补偿任务
使用分布式锁确保互斥执行
持续处理待补偿消息直到出现以下情况：
- 没有更多待处理消息
- 消息处理出现失败

## 示例
### 自定义业务分库规则
```
public class CustomShardingProvider extends AbstractShardingProvider {
    @Override
    public Object getShardingValue(Message message) {
        // 实现分片逻辑
    }

    @Override
    protected String getConfigFileName() {
        // 自定义配置文件名
        return "custom-datasource.properties";
    }

    @Override
    protected void customizePoolConfig(HikariConfig config, int index) {
        // 自定义连接池配置
    }
}
```
### 多数据源配置
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
