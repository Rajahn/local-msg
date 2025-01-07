package edu.vt.ranhuo.localmsg.sharding.rules;

public class SimpleShardingRule implements ShardingRule {
    private final DataSourceRule dataSourceRule;
    private final TableRule tableRule;

    public SimpleShardingRule(DataSourceRule dataSourceRule, TableRule tableRule) {
        this.dataSourceRule = dataSourceRule;
        this.tableRule = tableRule;
    }

    @Override
    public DataSourceRule getDatabaseRule() {
        return dataSourceRule;
    }

    @Override
    public TableRule getTableRule() {
        return tableRule;
    }
}