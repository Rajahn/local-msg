package edu.vt.ranhuo.localmsg.example.spi;

import edu.vt.ranhuo.localmsg.core.Message;
import edu.vt.ranhuo.localmsg.sharding.AbstractShardingProvider;

public class CustomShardingProvider extends AbstractShardingProvider {
    @Override
    public Object getShardingValue(Message message) {
        if (getDataSourceCount() == 1) {
            return 0;
        }
        return Math.abs(message.getKey().hashCode() % getDataSourceCount());
    }
}