package edu.vt.ranhuo.localmsg.sharding;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractShardingProvider implements ShardingProvider {
    private static final Logger logger = LoggerFactory.getLogger(AbstractShardingProvider.class);
    private final Map<Integer, DataSource> dataSources;

    protected AbstractShardingProvider() {
        this.dataSources = initializeDataSources();
        logger.info("ShardingProvider initialized with {} datasources", dataSources.size());
    }

    @Override
    public List<DataSource> getMessageDataSources() {
        return new ArrayList<>(dataSources.values());
    }

    @Override
    public DataSource getMessageDataSource(Object shardingValue) {
        if (shardingValue == null) {
            return dataSources.get(0);
        }
        return dataSources.get(((Number) shardingValue).intValue());
    }

    protected String getConfigFileName() {
        return "datasource.properties";
    }

    protected void customizePoolConfig(HikariConfig config, int index) {
        config.setMinimumIdle(2);
        config.setMaximumPoolSize(5);
        config.setConnectionTimeout(5000);
        config.setValidationTimeout(3000);
        config.setPoolName("LocalMessagePool-" + index);
    }

    private Map<Integer, DataSource> initializeDataSources() {
        Properties props = loadProperties();
        Map<Integer, DataSource> datasourceMap = new ConcurrentHashMap<>();

        int dataSourceCount = Integer.parseInt(props.getProperty("datasource.count", "1"));

        for (int i = 0; i < dataSourceCount; i++) {
            String prefix = "datasource." + i + ".";
            HikariConfig config = new HikariConfig();

            config.setJdbcUrl(props.getProperty(prefix + "url"));
            config.setUsername(props.getProperty(prefix + "username"));
            config.setPassword(props.getProperty(prefix + "password"));

            customizePoolConfig(config, i);

            DataSource dataSource = new HikariDataSource(config);
            datasourceMap.put(i, dataSource);

            logger.info("Initialized datasource {}: {}", i, props.getProperty(prefix + "url"));
        }

        if (datasourceMap.isEmpty()) {
            throw new IllegalStateException("No datasources configured");
        }

        return datasourceMap;
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream(getConfigFileName())) {
            if (input == null) {
                throw new IllegalStateException("Could not find " + getConfigFileName());
            }
            props.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load datasource properties", e);
        }
        return props;
    }

    protected int getDataSourceCount() {
        return dataSources.size();
    }
}
