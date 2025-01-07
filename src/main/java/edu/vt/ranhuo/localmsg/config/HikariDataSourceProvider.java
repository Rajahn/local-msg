package edu.vt.ranhuo.localmsg.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import java.util.Properties;

public class HikariDataSourceProvider implements DataSourceProvider {
    @Override
    public DataSource createDataSource(Properties properties) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(properties.getProperty("jdbc.url"));
        config.setUsername(properties.getProperty("jdbc.username"));
        config.setPassword(properties.getProperty("jdbc.password"));
        config.setMinimumIdle(Integer.parseInt(properties.getProperty("jdbc.minimumIdle", "5")));
        config.setMaximumPoolSize(Integer.parseInt(properties.getProperty("jdbc.maximumPoolSize", "10")));
        return new HikariDataSource(config);
    }
}