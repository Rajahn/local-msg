package edu.vt.ranhuo.localmsg.sharding.rules;

/**
 * 分库分表规则
 */
public interface ShardingRule {
    /**
     * 获取库路由规则
     */
    DataSourceRule getDatabaseRule();

    /**
     * 获取表路由规则
     */
    TableRule getTableRule();
}