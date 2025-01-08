// ShardingProvider.java
package edu.vt.ranhuo.localmsg.sharding;

import edu.vt.ranhuo.localmsg.core.Message;
import javax.sql.DataSource;
import java.util.List;

/**
 * 分片规则提供者接口，由用户实现
 */
public interface ShardingProvider {
    /**
     * 根据消息内容获取分片值
     * @param message 消息内容
     * @return 分片值
     */
    Object getShardingValue(Message message);

    /**
     * 获取所有本地消息表数据源
     *
     * @return 数据源列表
     */
    List<DataSource> getMessageDataSources();

    /**
     * 根据分片值获取对应的消息表数据源
     *
     * @param shardingValue 分片值
     * @return 数据源
     */
    DataSource getMessageDataSource(Object shardingValue);
}