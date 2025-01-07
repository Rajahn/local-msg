package edu.vt.ranhuo.localmsg.config;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * 数据源提供者接口，支持SPI扩展
 */
public interface DataSourceProvider {
    DataSource createDataSource(Properties properties);
}

