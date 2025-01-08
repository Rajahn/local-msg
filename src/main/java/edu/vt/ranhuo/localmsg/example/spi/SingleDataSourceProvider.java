package edu.vt.ranhuo.localmsg.example.spi;

import edu.vt.ranhuo.localmsg.core.Message;
import edu.vt.ranhuo.localmsg.sharding.AbstractShardingProvider;

public class SingleDataSourceProvider extends AbstractShardingProvider {
    @Override
    public Object getShardingValue(Message message) {
        return 0; // 不分库, 始终返回第一个数据源
    }

    @Override
    protected String getConfigFileName() {
        return "single-datasource.properties";
    }
}