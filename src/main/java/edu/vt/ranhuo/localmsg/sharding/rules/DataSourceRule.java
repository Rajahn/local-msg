package edu.vt.ranhuo.localmsg.sharding.rules;

import javax.sql.DataSource;
import java.util.List;

/**
 * 库路由规则
 */
public interface DataSourceRule {
    /**
     * 获取实际的数据源
     * @param shardingValue 分片值
     * @return 数据源
     */
    DataSource getDataSource(Object shardingValue);

    /**
     * 获取所有数据源
     * @return 所有实际的数据源
     */
    List<DataSource> getAllDataSources();
}

