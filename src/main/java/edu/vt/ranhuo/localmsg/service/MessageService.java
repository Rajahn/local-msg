package edu.vt.ranhuo.localmsg.service;

import edu.vt.ranhuo.localmsg.core.Message;
import edu.vt.ranhuo.localmsg.executor.TransactionCallback;

import java.sql.Connection;

public interface MessageService {
    /**
     * 在事务中执行业务逻辑并发送消息
     * @param connection 数据库连接
     * @param business 业务逻辑
     * @throws Exception 执行失败时抛出异常
     */
    void executeWithTransaction(Connection connection, TransactionCallback<Message> business) throws Exception;

    /**
     * 直接发送消息（不开启新事务）
     * @param message 要发送的消息
     * @throws Exception 发送失败时抛出异常
     */
    void sendMessage(Message message) throws Exception;

    /**
     * 保存消息到本地表
     *
     * @param message 要保存的消息
     * @throws Exception 保存失败时抛出异常
     */
    void saveMessage(Message message) throws Exception;
}
