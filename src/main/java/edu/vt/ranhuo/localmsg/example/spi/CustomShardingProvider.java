package edu.vt.ranhuo.localmsg.example.spi;

import edu.vt.ranhuo.localmsg.config.Configuration;
import edu.vt.ranhuo.localmsg.config.DataSourceFactory;
import edu.vt.ranhuo.localmsg.sharding.*;
import edu.vt.ranhuo.localmsg.sharding.rules.ShardingRule;
import edu.vt.ranhuo.localmsg.sharding.rules.SimpleShardingRule;
import edu.vt.ranhuo.localmsg.sharding.rules.SingleDataSourceRule;
import edu.vt.ranhuo.localmsg.sharding.rules.SingleTableRule;

import javax.sql.DataSource;

public class CustomShardingProvider implements ShardingProvider {
    @Override
    public ShardingRule createShardingRule(Configuration configuration) {

        DataSource dataSource = DataSourceFactory.createDataSource(configuration.getDataSourceProperties());

        return new SimpleShardingRule(
                new SingleDataSourceRule(dataSource),
                new SingleTableRule()
        );
    }
}
