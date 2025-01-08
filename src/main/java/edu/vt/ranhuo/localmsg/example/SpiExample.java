package edu.vt.ranhuo.localmsg.example;

import edu.vt.ranhuo.localmsg.LocalMessageInitializer;
import edu.vt.ranhuo.localmsg.config.DataSourceFactory;
import edu.vt.ranhuo.localmsg.core.Message;
import edu.vt.ranhuo.localmsg.sharding.ShardingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpiExample {
    private static final Logger logger = LoggerFactory.getLogger(SpiExample.class);

    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            logger.error("Failed to load MySQL driver", e);
            return;
        }

        try (LocalMessageInitializer messageTable = new LocalMessageInitializer.Builder()
                .build()) {

            messageTable.start();

            // 创建测试消息
            Message testMessage = Message.builder()
                    .topic("test-topic")
                    .key("test-key-" + System.currentTimeMillis())
                    .content("Hello from SPI example!")
                    .build();

            // 执行业务和消息处理
            messageTable.executeBusinessWithMessage(
                    conn -> {
                        // 模拟业务操作
                        System.out.println("Executing business logic...");
                        return null;
                    },
                    testMessage
            );

            logger.info("Example completed successfully");
        } catch (Exception e) {
            logger.error("Example failed", e);
        }
    }
}