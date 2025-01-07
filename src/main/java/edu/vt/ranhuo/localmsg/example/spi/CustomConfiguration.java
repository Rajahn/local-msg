package edu.vt.ranhuo.localmsg.example.spi;

import edu.vt.ranhuo.localmsg.config.Configuration;

import java.time.Duration;
import java.util.Properties;

public class CustomConfiguration implements Configuration {
    private final Properties properties;

    public CustomConfiguration() {
        properties = new Properties();
        properties.setProperty("jdbc.url", "jdbc:mysql://localhost:3306/local_msg_test?useSSL=false&serverTimezone=UTC");
        properties.setProperty("jdbc.username", "root");
        properties.setProperty("jdbc.password", "root");
    }

    @Override
    public String getKafkaBootstrapServers() {
        return "localhost:9092";
    }

    @Override
    public String getMessageTableName() {
        return "local_msgs";  // 自定义表名
    }

    @Override
    public Duration getMessageWaitDuration() {
        return Duration.ofSeconds(15);
    }

    @Override
    public int getMaxRetryTimes() {
        return 5;
    }

    @Override
    public int getBatchSize() {
        return 5;
    }

    @Override
    public Properties getDataSourceProperties() {
        return properties;
    }
}