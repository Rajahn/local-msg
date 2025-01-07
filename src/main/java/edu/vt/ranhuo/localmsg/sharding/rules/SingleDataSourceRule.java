package edu.vt.ranhuo.localmsg.sharding.rules;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.List;

/**
 * 单库路由规则(不分库)
 */
public class SingleDataSourceRule implements DataSourceRule {
    private final DataSource dataSource;

    public SingleDataSourceRule(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public DataSource getDataSource(Object shardingValue) {
        return dataSource;
    }

    @Override
    public List<DataSource> getAllDataSources() {
        return Collections.singletonList(dataSource);
    }
}