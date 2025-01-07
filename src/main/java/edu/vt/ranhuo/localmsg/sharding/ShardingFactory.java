package edu.vt.ranhuo.localmsg.sharding;

import edu.vt.ranhuo.localmsg.config.Configuration;
import edu.vt.ranhuo.localmsg.sharding.rules.ShardingRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * 分库规则工厂
 */
public class ShardingFactory {
    private static final Logger logger = LoggerFactory.getLogger(ShardingFactory.class);

    private ShardingFactory() {}

    public static ShardingRule createShardingRule(Configuration configuration) {
        ServiceLoader<ShardingProvider> loader = ServiceLoader.load(ShardingProvider.class);
        Iterator<ShardingProvider> it = loader.iterator();

        if (it.hasNext()) {
            ShardingProvider provider = it.next();
            logger.info("Using custom ShardingProvider: {}", provider.getClass().getName());
            return provider.createShardingRule(configuration);
        }

        logger.info("No custom ShardingProvider found, using default single datasource implementation");
        return new DefaultShardingProvider().createShardingRule(configuration);
    }
}
