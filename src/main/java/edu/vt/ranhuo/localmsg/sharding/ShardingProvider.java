package edu.vt.ranhuo.localmsg.sharding;

import edu.vt.ranhuo.localmsg.config.Configuration;
import edu.vt.ranhuo.localmsg.sharding.rules.ShardingRule;

/**
 * 分库规则提供者接口，支持SPI扩展
 */
public interface ShardingProvider {
    /**
     * 根据配置创建分库规则
     * @param configuration 配置信息
     * @return 分库规则
     */
    ShardingRule createShardingRule(Configuration configuration);
}