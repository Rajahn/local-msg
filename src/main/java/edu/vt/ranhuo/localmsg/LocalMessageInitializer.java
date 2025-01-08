package edu.vt.ranhuo.localmsg;

import edu.vt.ranhuo.localmsg.config.Configuration;
import edu.vt.ranhuo.localmsg.config.DataSourceFactory;
import edu.vt.ranhuo.localmsg.core.Message;
import edu.vt.ranhuo.localmsg.dao.JdbcMessageDao;
import edu.vt.ranhuo.localmsg.executor.JdbcTransactionExecutor;
import edu.vt.ranhuo.localmsg.executor.TransactionCallback;
import edu.vt.ranhuo.localmsg.producer.KafkaProducer;
import edu.vt.ranhuo.localmsg.producer.Producer;
import edu.vt.ranhuo.localmsg.service.DefaultMessageService;
import edu.vt.ranhuo.localmsg.sharding.DefaultShardingProvider;
import edu.vt.ranhuo.localmsg.sharding.ShardingProvider;
import edu.vt.ranhuo.localmsg.task.AsyncCompensationTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.Closeable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class LocalMessageInitializer implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(LocalMessageInitializer.class);

    private final Configuration configuration;
    private final ShardingProvider shardingProvider;
    private final Producer producer;
    private final Map<DataSource, DefaultMessageService> messageServices;
    private final Map<DataSource, AsyncCompensationTask> compensationTasks;
    private final AtomicBoolean started = new AtomicBoolean(false);

    private LocalMessageInitializer(Builder builder) {
        // 加载配置
        Properties props = loadProperties();
        this.configuration = new Configuration(props);
        this.shardingProvider = initializeShardingProvider(builder.shardingProvider);
        this.producer = KafkaProducer.create(configuration.getKafkaBootstrapServers());
        this.messageServices = new ConcurrentHashMap<>();
        this.compensationTasks = new ConcurrentHashMap<>();

        // 初始化服务
        initializeServices();

        logger.info("LocalMessageInitializer created with configuration: {}",
                configuration.getClass().getName());
    }

    private void initializeServices() {
        for (DataSource dataSource : shardingProvider.getMessageDataSources()) {
            JdbcMessageDao messageDao = new JdbcMessageDao();
            JdbcTransactionExecutor transactionExecutor = new JdbcTransactionExecutor(dataSource);

            DefaultMessageService messageService = new DefaultMessageService(
                    messageDao,
                    producer,
                    transactionExecutor,
                    configuration.getMessageTableName(),
                    configuration.getMaxRetryTimes(),
                    configuration.getMessageWaitDuration()
            );
            messageServices.put(dataSource, messageService);

            AsyncCompensationTask compensationTask = new AsyncCompensationTask(
                    messageService,
                    messageDao,
                    configuration.getBatchSize()
            );
            compensationTasks.put(dataSource, compensationTask);
        }
    }

    public void start() {
        if (!started.compareAndSet(false, true)) {
            logger.warn("LocalMessageTable is already started");
            return;
        }

        // 启动所有补偿任务
        compensationTasks.values().forEach(AsyncCompensationTask::start);
        logger.info("LocalMessageTable started with {} datasources", compensationTasks.size());
    }

    /**
     * 执行业务逻辑并发送消息
     * @param business 业务逻辑
     * @param message 消息内容
     * @throws Exception 执行失败时抛出异常
     */
    public void executeBusinessWithMessage(TransactionCallback<?> business, Message message) throws Exception {
        checkStarted();
        Objects.requireNonNull(message, "Message cannot be null");
        Objects.requireNonNull(business, "Business callback cannot be null");

        // 获取分片值和对应数据源
        Object shardingValue = shardingProvider.getShardingValue(message);
        DataSource targetDataSource = shardingProvider.getMessageDataSource(shardingValue);

        if (targetDataSource == null) {
            throw new IllegalStateException("No datasource found for sharding value: " + shardingValue);
        }

        DefaultMessageService messageService = messageServices.get(targetDataSource);
        if (messageService == null) {
            throw new IllegalStateException("No message service found for datasource");
        }

        // 执行业务和消息处理
        try {
            business.doInTransaction(null);  // 业务方负责事务
            messageService.saveMessage(message);
        } catch (Exception e) {
            logger.error("Failed to execute business with message: {}", message, e);
            throw e;
        }
    }

    @Override
    public void close() {
        if (!started.compareAndSet(true, false)) {
            return;
        }

        // 停止所有补偿任务
        compensationTasks.values().forEach(AsyncCompensationTask::stop);

        // 关闭生产者
        producer.close();

        logger.info("LocalMessageTable shutdown successfully");
    }

    private void checkStarted() {
        if (!started.get()) {
            throw new IllegalStateException("LocalMessageTable not started");
        }
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        try {
            props.load(getClass().getClassLoader()
                    .getResourceAsStream("local-message.properties"));
        } catch (Exception e) {
            logger.warn("Failed to load local-message.properties, using defaults");
        }
        return props;
    }

    private ShardingProvider initializeShardingProvider(ShardingProvider customProvider) {
        if (customProvider != null) {
            return customProvider;
        }

        // 尝试通过SPI加载
        ServiceLoader<ShardingProvider> loader = ServiceLoader.load(ShardingProvider.class);
        Iterator<ShardingProvider> it = loader.iterator();
        if (it.hasNext()) {
            return it.next();
        }

        // 使用默认的单库实现
        DataSource defaultDataSource = DataSourceFactory.createDataSource(
                configuration.getDataSourceProperties()
        );
        return new DefaultShardingProvider(defaultDataSource);
    }

    public static class Builder {
        private ShardingProvider shardingProvider;
        private Properties properties;

        public Builder() {
            this.properties = new Properties();
        }

        public LocalMessageInitializer build() {
            validateProperties();
            return new LocalMessageInitializer(this);
        }

        private void validateProperties() {
            if (!properties.containsKey("local.msg.kafka.bootstrap-servers")) {
                logger.warn("Kafka bootstrap servers not configured, using default: localhost:9092");
                properties.setProperty("local.msg.kafka.bootstrap-servers", "localhost:9092");
            }
        }
    }
}