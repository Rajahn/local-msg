package edu.vt.ranhuo.localmsg.sharding;

import edu.vt.ranhuo.localmsg.config.Configuration;
import edu.vt.ranhuo.localmsg.config.DataSourceFactory;
import edu.vt.ranhuo.localmsg.sharding.rules.ShardingRule;
import edu.vt.ranhuo.localmsg.sharding.rules.SimpleShardingRule;
import edu.vt.ranhuo.localmsg.sharding.rules.SingleDataSourceRule;
import edu.vt.ranhuo.localmsg.sharding.rules.SingleTableRule;

/**
 * 默认的分库规则提供者，使用单库模式
 */
public class DefaultShardingProvider implements ShardingProvider {
    @Override
    public ShardingRule createShardingRule(Configuration configuration) {
        return new SimpleShardingRule(
                new SingleDataSourceRule(
                        DataSourceFactory.createDataSource(configuration.getDataSourceProperties())
                ),
                new SingleTableRule()
        );
    }
}