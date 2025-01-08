// DataSourceFactory.java
package edu.vt.ranhuo.localmsg.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import java.util.Properties;

public class DataSourceFactory {
    public static DataSource createDataSource(Properties properties) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(properties.getProperty("jdbc.url"));
        config.setUsername(properties.getProperty("jdbc.username"));
        config.setPassword(properties.getProperty("jdbc.password"));

        config.setMinimumIdle(2);
        config.setMaximumPoolSize(5);
        config.setConnectionTimeout(5000);
        config.setValidationTimeout(3000);
        config.setPoolName("LocalMessagePool");

        return new HikariDataSource(config);
    }
}