package edu.vt.ranhuo.localmsg.config;

import java.time.Duration;
import java.util.Properties;

public class DefaultConfiguration implements Configuration {
    private final Properties properties;

    public DefaultConfiguration(Properties properties) {
        this.properties = new Properties();
        this.properties.putAll(properties);
    }

    @Override
    public String getKafkaBootstrapServers() {
        return properties.getProperty("local.msg.kafka.bootstrap-servers", "localhost:9092");
    }

    @Override
    public String getMessageTableName() {
        return properties.getProperty("local.msg.table-name", "local_msgs");
    }

    @Override
    public Duration getMessageWaitDuration() {
        return Duration.ofSeconds(Long.parseLong(
                properties.getProperty("local.msg.wait-duration-seconds", "30")
        ));
    }

    @Override
    public int getMaxRetryTimes() {
        return Integer.parseInt(properties.getProperty("local.msg.max-retry-times", "3"));
    }

    @Override
    public int getBatchSize() {
        return Integer.parseInt(properties.getProperty("local.msg.batch-size", "100"));
    }

    @Override
    public Properties getDataSourceProperties() {
        Properties dsProps = new Properties();
        // 数据源配置前缀
        String prefix = "local.msg.datasource.";
        properties.stringPropertyNames().stream()
                .filter(key -> key.startsWith(prefix))
                .forEach(key -> dsProps.setProperty(
                        key.substring(prefix.length()),
                        properties.getProperty(key)
                ));
        return dsProps;
    }
}

