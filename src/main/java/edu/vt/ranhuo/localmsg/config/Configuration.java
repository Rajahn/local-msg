package edu.vt.ranhuo.localmsg.config;

import java.time.Duration;
import java.util.Properties;

public class Configuration {
    private final Properties properties;

    public Configuration(Properties properties) {
        this.properties = properties;
    }

    public String getKafkaBootstrapServers() {
        return properties.getProperty("local.msg.kafka.bootstrap-servers", "localhost:9092");
    }

    public String getMessageTableName() {
        return properties.getProperty("local.msg.table-name", "local_msgs");
    }

    public Duration getMessageWaitDuration() {
        return Duration.ofSeconds(Long.parseLong(
                properties.getProperty("local.msg.wait-duration-seconds", "15")
        ));
    }

    public int getMaxRetryTimes() {
        return Integer.parseInt(properties.getProperty("local.msg.max-retry-times", "5"));
    }

    public int getBatchSize() {
        return Integer.parseInt(properties.getProperty("local.msg.batch-size", "100"));
    }

    public Properties getDataSourceProperties() {
        Properties dsProps = new Properties();
        dsProps.setProperty("jdbc.url", properties.getProperty("local.msg.datasource.url"));
        dsProps.setProperty("jdbc.username", properties.getProperty("local.msg.datasource.username"));
        dsProps.setProperty("jdbc.password", properties.getProperty("local.msg.datasource.password"));
        return dsProps;
    }
}