package edu.vt.ranhuo.localmsg.executor;

import java.sql.Connection;

public interface TransactionExecutor {
    /**
     * 在事务中执行操作
     * @param conn 如果传入null，则创建新连接
     * @param action 要执行的操作
     * @return 操作结果
     */
    <T> T execute(Connection conn, TransactionCallback<T> action) throws Exception;
}
