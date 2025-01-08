package edu.vt.ranhuo.localmsg.sharding;

import edu.vt.ranhuo.localmsg.core.Message;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.List;

public class DefaultShardingProvider implements ShardingProvider {
    private final DataSource defaultDataSource;

    public DefaultShardingProvider(DataSource dataSource) {
        this.defaultDataSource = dataSource;
    }

    @Override
    public Object getShardingValue(Message message) {
        return null;  // 单库模式不需要分片值
    }


    @Override
    public List<DataSource> getMessageDataSources() {
        return Collections.singletonList(defaultDataSource);
    }

    @Override
    public DataSource getMessageDataSource(Object shardingValue) {
        return defaultDataSource;
    }
}