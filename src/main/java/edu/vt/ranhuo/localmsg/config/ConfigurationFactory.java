package edu.vt.ranhuo.localmsg.config;

import java.util.Iterator;
import java.util.Properties;
import java.util.ServiceLoader;

/**
 * 配置工厂，支持SPI机制
 */
public class ConfigurationFactory {
    private ConfigurationFactory() {}

    public static Configuration createConfiguration(Properties properties) {
        // 通过SPI加载自定义配置实现
        ServiceLoader<Configuration> loader = ServiceLoader.load(Configuration.class);
        Iterator<Configuration> it = loader.iterator();

        if (it.hasNext()) {
            return it.next();
        }
        return new DefaultConfiguration(properties);
    }

    public static Configuration createDefaultConfiguration() {
        Properties props = new Properties();
        // 默认配置
        props.setProperty("local.msg.kafka.bootstrap-servers", "localhost:9092");
        props.setProperty("local.msg.table-name", "local_msgs");
        props.setProperty("local.msg.wait-duration-seconds", "30");
        props.setProperty("local.msg.max-retry-times", "3");
        props.setProperty("local.msg.batch-size", "100");

        // 数据源默认配置
        props.setProperty("local.msg.datasource.jdbc.url",
                "jdbc:mysql://localhost:3306/local_msg_test?useSSL=false&serverTimezone=UTC");
        props.setProperty("local.msg.datasource.jdbc.username", "root");
        props.setProperty("local.msg.datasource.jdbc.password", "root");
        props.setProperty("local.msg.datasource.jdbc.minimumIdle", "5");
        props.setProperty("local.msg.datasource.jdbc.maximumPoolSize", "10");

        return new DefaultConfiguration(props);
    }
}