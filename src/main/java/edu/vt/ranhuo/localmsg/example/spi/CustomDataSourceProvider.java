package edu.vt.ranhuo.localmsg.example.spi;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import edu.vt.ranhuo.localmsg.config.DataSourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Properties;

public class CustomDataSourceProvider implements DataSourceProvider {
    private static final Logger logger = LoggerFactory.getLogger(CustomDataSourceProvider.class);

    @Override
    public DataSource createDataSource(Properties properties) {
        logger.info("Creating custom data source with properties: {}", properties);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(properties.getProperty("jdbc.url"));
        config.setUsername(properties.getProperty("jdbc.username"));
        config.setPassword(properties.getProperty("jdbc.password"));

        // 自定义连接池配置
        config.setMinimumIdle(2);
        config.setMaximumPoolSize(5);
        config.setConnectionTimeout(5000);
        config.setValidationTimeout(3000);

        config.setPoolName("CustomLocalMessagePool");

        return new HikariDataSource(config);
    }
}