package edu.vt.ranhuo.localmsg.sharding.rules;

import java.util.List;

/**
 * 表路由规则
 */
public interface TableRule {
    /**
     * 获取实际物理表名
     * @param logicTable 逻辑表名
     * @param shardingValue 分片值
     * @return 物理表名
     */
    String getActualTable(String logicTable, Object shardingValue);

    /**
     * 获取当前逻辑表下的所有物理表
     * @param logicTable 逻辑表名
     * @return 物理表列表
     */
    List<String> getAllActualTables(String logicTable);
}
