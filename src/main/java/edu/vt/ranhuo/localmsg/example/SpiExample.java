package edu.vt.ranhuo.localmsg.example;

import edu.vt.ranhuo.localmsg.LocalMessageInitializer;
import edu.vt.ranhuo.localmsg.config.ConfigurationFactory;
import edu.vt.ranhuo.localmsg.config.DataSourceFactory;
import edu.vt.ranhuo.localmsg.core.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

public class SpiExample {
    private static final Logger logger = LoggerFactory.getLogger(SpiExample.class);

    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            logger.error("Failed to load MySQL driver", e);
            return;
        }

        try (LocalMessageInitializer messageTable = new LocalMessageInitializer.Builder().build()) {
            messageTable.start();
            logger.info("Local message table service started");

            // 获取数据源
            DataSource dataSource = DataSourceFactory.createDataSource(
                    ConfigurationFactory.createDefaultConfiguration().getDataSourceProperties()
            );

            // 模拟业务操作和消息发送
            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    createTestTableIfNotExists(conn);
                    simulateBusinessOperation(messageTable, conn);
                    conn.commit();
                    logger.info("Transaction committed successfully");
                } catch (Exception e) {
                    conn.rollback();
                    throw e;
                }
            }

            logger.info("Example completed successfully");
        } catch (Exception e) {
            logger.error("Example failed", e);
        }
    }

    private static void simulateBusinessOperation(
            LocalMessageInitializer messageTable,
            Connection connection) throws Exception {
        // 创建测试消息
        Message testMessage = Message.builder()
                .topic("test-topic")
                .key("test-key-" + System.currentTimeMillis())
                .content("Hello from SPI example!")
                .build();

        // 使用提供的连接执行事务
        messageTable.executeWithTransaction(1L, connection, () -> testMessage);

        logger.info("Test message sent successfully: {}", testMessage.getKey());
    }

    private static void createTestTableIfNotExists(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            String createTableSQL =
                    "CREATE TABLE IF NOT EXISTS test_business_table (" +
                            "    id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                            "    data VARCHAR(255)," +
                            "    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                            ")";
            stmt.execute(createTableSQL);
            logger.info("Test table created or verified");
        }
    }
}