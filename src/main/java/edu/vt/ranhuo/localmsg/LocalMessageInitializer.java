package edu.vt.ranhuo.localmsg;

import edu.vt.ranhuo.localmsg.config.Configuration;
import edu.vt.ranhuo.localmsg.config.ConfigurationFactory;
import edu.vt.ranhuo.localmsg.core.Message;
import edu.vt.ranhuo.localmsg.dao.JdbcMessageDao;
import edu.vt.ranhuo.localmsg.executor.JdbcTransactionExecutor;
import edu.vt.ranhuo.localmsg.producer.KafkaProducer;
import edu.vt.ranhuo.localmsg.producer.Producer;
import edu.vt.ranhuo.localmsg.service.DefaultMessageService;
import edu.vt.ranhuo.localmsg.sharding.*;
import edu.vt.ranhuo.localmsg.sharding.rules.ShardingRule;
import edu.vt.ranhuo.localmsg.task.AsyncCompensationTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.Closeable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LocalMessageInitializer implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(LocalMessageInitializer.class);

    private final Configuration configuration;
    private final ShardingRule shardingRule;
    private final Producer producer;
    private final Map<DataSource, DefaultMessageService> messageServices;
    private final Map<DataSource, AsyncCompensationTask> compensationTasks;
    private final AtomicBoolean started = new AtomicBoolean(false);

    private LocalMessageInitializer(Builder builder) {
        // 通过Builder获取配置，如果没有配置则通过SPI加载
        this.configuration = builder.getConfiguration();

        // 通过配置创建或获取分片规则
        this.shardingRule = builder.getShardingRule(configuration);

        // 使用配置创建生产者
        this.producer = KafkaProducer.create(configuration.getKafkaBootstrapServers());

        this.messageServices = new ConcurrentHashMap<>();
        this.compensationTasks = new ConcurrentHashMap<>();

        // 初始化服务
        initializeServices();

        logger.info("LocalMessageInitializer created with configuration: {}, shardingRule: {}",
                configuration.getClass().getName(), shardingRule.getClass().getName());
    }

    private void initializeServices() {
        for (DataSource dataSource : shardingRule.getDatabaseRule().getAllDataSources()) {
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

    public void executeWithTransaction(Object shardingValue, Connection connection,
                                       MessageProvider messageProvider) throws Exception {
        checkStarted();
        Objects.requireNonNull(connection, "Connection cannot be null");

        // 获取目标数据源并验证
        DataSource targetDataSource = shardingRule.getDatabaseRule().getDataSource(shardingValue);
        validateConnection(connection, targetDataSource);

        DefaultMessageService messageService = messageServices.get(targetDataSource);
        if (messageService == null) {
            throw new IllegalStateException("No message service found for datasource");
        }

        Message message = messageProvider.provide();
        messageService.saveMessage(connection, message);
    }

    private void validateConnection(Connection userConn, DataSource targetDataSource) throws SQLException {
        Objects.requireNonNull(userConn, "User connection cannot be null");
        Objects.requireNonNull(targetDataSource, "Target datasource cannot be null");

        if (!isSameDatabase(userConn, targetDataSource)) {
            throw new IllegalStateException("Connection does not match the target datasource");
        }
    }

    private boolean isSameDatabase(Connection conn, DataSource dataSource) throws SQLException {
        try (Connection targetConn = dataSource.getConnection()) {
            String userUrl = conn.getMetaData().getURL();
            String targetUrl = targetConn.getMetaData().getURL();
            return Objects.equals(userUrl, targetUrl);
        }
    }

    @FunctionalInterface
    public interface MessageProvider {
        Message provide() throws Exception;
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


    public static class Builder {
        private Configuration configuration;
        private ShardingRule shardingRule;
        private Properties properties;

        public Builder() {
            this.properties = new Properties();
        }

        /**
         * 可选：设置配置属性
         */
        public Builder properties(Properties properties) {
            this.properties = properties;
            return this;
        }

        /**
         * 可选：直接设置配置对象
         */
        public Builder configuration(Configuration configuration) {
            this.configuration = configuration;
            return this;
        }

        /**
         * 可选：直接设置分片规则
         */
        public Builder shardingRule(ShardingRule shardingRule) {
            this.shardingRule = shardingRule;
            return this;
        }

        /**
         * 获取配置，优先级：
         * 1. 手动设置的配置
         * 2. SPI加载的配置
         * 3. 默认配置
         */
        private Configuration getConfiguration() {
            if (configuration != null) {
                return configuration;
            }

            // 尝试通过SPI加载配置
            ServiceLoader<Configuration> loader = ServiceLoader.load(Configuration.class);
            Iterator<Configuration> it = loader.iterator();
            if (it.hasNext()) {
                Configuration spiConfig = it.next();
                logger.info("Using SPI configuration: {}", spiConfig.getClass().getName());
                return spiConfig;
            }

            // 使用默认配置
            logger.info("Using default configuration");
            return ConfigurationFactory.createConfiguration(properties);
        }

        /**
         * 获取分片规则，优先级：
         * 1. 手动设置的规则
         * 2. SPI加载的规则
         */
        private ShardingRule getShardingRule(Configuration config) {
            if (shardingRule != null) {
                return shardingRule;
            }
            return ShardingFactory.createShardingRule(config);
        }

        /**
         * 构建实例，验证必要的配置
         */
        public LocalMessageInitializer build() {
            validateProperties();
            return new LocalMessageInitializer(this);
        }

        private void validateProperties() {
            // 验证kafkaBootstrapServers
            if (!properties.containsKey("local.msg.kafka.bootstrap-servers")) {
                logger.warn("Kafka bootstrap servers not configured, using default: localhost:9092");
                properties.setProperty("local.msg.kafka.bootstrap-servers", "localhost:9092");
            }
        }
    }
}