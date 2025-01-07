package edu.vt.ranhuo.localmsg.sharding.rules;


import java.util.Collections;
import java.util.List;

/**
 * 单表路由规则(不分表)
 */
public class SingleTableRule implements TableRule {
    @Override
    public String getActualTable(String logicTable, Object shardingValue) {
        return logicTable;
    }

    @Override
    public List<String> getAllActualTables(String logicTable) {
        return Collections.singletonList(logicTable);
    }
}
