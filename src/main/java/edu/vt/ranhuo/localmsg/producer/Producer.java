package edu.vt.ranhuo.localmsg.producer;

import edu.vt.ranhuo.localmsg.core.Message;

public interface Producer {
    /**
     * 发送消息到消息队列
     * @param message 要发送的消息
     * @throws Exception 发送失败时抛出异常
     */
    void send(Message message) throws Exception;

    void close();
}
