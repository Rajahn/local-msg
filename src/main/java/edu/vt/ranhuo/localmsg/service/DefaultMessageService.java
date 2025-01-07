package edu.vt.ranhuo.localmsg.service;

import edu.vt.ranhuo.localmsg.core.LocalMessage;
import edu.vt.ranhuo.localmsg.core.Message;
import edu.vt.ranhuo.localmsg.core.MessageStatus;
import edu.vt.ranhuo.localmsg.dao.MessageDao;
import edu.vt.ranhuo.localmsg.executor.TransactionCallback;
import edu.vt.ranhuo.localmsg.executor.TransactionExecutor;
import edu.vt.ranhuo.localmsg.producer.Producer;
import edu.vt.ranhuo.localmsg.support.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.time.Duration;

public class DefaultMessageService implements MessageService {
    private static final Logger logger = LoggerFactory.getLogger(DefaultMessageService.class);

    private final MessageDao messageDao;
    private final Producer producer;
    private final TransactionExecutor transactionExecutor;
    private final String tableName;
    private final int maxRetryTimes;
    private final Duration waitDuration;

    public DefaultMessageService(
            MessageDao messageDao,
            Producer producer,
            TransactionExecutor transactionExecutor,
            String tableName,
            int maxRetryTimes,
            Duration waitDuration) {
        this.messageDao = messageDao;
        this.producer = producer;
        this.transactionExecutor = transactionExecutor;
        this.tableName = tableName;
        this.maxRetryTimes = maxRetryTimes;
        this.waitDuration = waitDuration;
    }

    public void executeWithTransaction(Connection connection, TransactionCallback<Message> business) throws Exception {
        Message message = business.doInTransaction(connection);
        saveMessage(connection, message);
    }

    @Override
    public void sendMessage(Message message) throws Exception {
        try {
            producer.send(message);
            logger.info("Message sent successfully: topic={}, key={}",
                    message.getTopic(), message.getKey());
        } catch (Exception e) {
            logger.error("Failed to send message: topic={}, key={}",
                    message.getTopic(), message.getKey(), e);
            throw e;
        }
    }


    @Override
    public void saveMessage(Connection conn, Message message) throws Exception {
        LocalMessage localMessage = createLocalMessage(message);
        messageDao.save(conn, tableName, localMessage);

        // 尝试立即发送消息
        try {
            sendMessage(message);
            // 更新状态为发送成功
            messageDao.updateStatus(conn, tableName, localMessage.getId(),
                    1, MessageStatus.SUCCESS.getValue());
        } catch (Exception e) {
            logger.warn("Failed to send message immediately, will retry later: topic={}, key={}",
                    message.getTopic(), message.getKey());
            // 更新发送次数
            messageDao.updateStatus(conn, tableName, localMessage.getId(),
                    1, MessageStatus.INIT.getValue());
        }
    }

    public void handleMessageRetry(LocalMessage localMessage) throws Exception {
        Message message = JsonUtils.deserialize(localMessage.getData());
        try {
            sendMessage(message);
            // 使用新事务更新状态
            transactionExecutor.execute(null, conn -> {
                messageDao.updateStatus(conn, tableName, localMessage.getId(),
                        localMessage.getSendTimes() + 1, MessageStatus.SUCCESS.getValue());
                return null;
            });
        } catch (Exception e) {
            int newSendTimes = localMessage.getSendTimes() + 1;
            final int status;
            if (newSendTimes >= maxRetryTimes) {
                status = MessageStatus.FAILED.getValue();
                logger.error("Message retry failed and max retry times reached: id={}, topic={}, key={}",
                        localMessage.getId(), message.getTopic(), message.getKey());
            } else {
                status = MessageStatus.INIT.getValue();
                logger.warn("Message retry failed, will retry later: id={}, topic={}, key={}",
                        localMessage.getId(), message.getTopic(), message.getKey());
            }

            // 使用新事务更新状态
            transactionExecutor.execute(null, conn -> {
                messageDao.updateStatus(conn, tableName, localMessage.getId(),
                        newSendTimes, status);
                return null;
            });

            throw e;
        }
    }

    private LocalMessage createLocalMessage(Message message) throws Exception {
        long now = System.currentTimeMillis();
        return LocalMessage.builder()
                .key(message.getKey())
                .data(JsonUtils.serialize(message))
                .sendTimes(0)
                .status(MessageStatus.INIT)
                .updateTime(now)
                .createTime(now)
                .build();
    }

    // Getters for configuration
    public int getMaxRetryTimes() {
        return maxRetryTimes;
    }

    public Duration getWaitDuration() {
        return waitDuration;
    }

    public String getTableName() {
        return tableName;
    }

    public TransactionExecutor getTransactionExecutor() {
        return transactionExecutor;
    }
}
