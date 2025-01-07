package edu.vt.ranhuo.localmsg.config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Iterator;
import java.util.Properties;
import java.util.ServiceLoader;

/**
 * 数据源工厂，支持SPI机制
 */
public class DataSourceFactory {
    private static final Logger logger = LoggerFactory.getLogger(DataSourceFactory.class);

    private DataSourceFactory() {}

    public static DataSource createDataSource(Properties properties) {
        // 通过SPI加载自定义数据源提供者
        ServiceLoader<DataSourceProvider> loader = ServiceLoader.load(DataSourceProvider.class);
        Iterator<DataSourceProvider> it = loader.iterator();

        if (it.hasNext()) {
            DataSourceProvider provider = it.next();
            logger.info("Using custom DataSourceProvider: {}", provider.getClass().getName());
            return provider.createDataSource(properties);
        }

        // 没有自定义实现时使用默认HikariCP数据源
        logger.info("No custom DataSourceProvider found, using default HikariCP implementation");
        return new HikariDataSourceProvider().createDataSource(properties);
    }
}
