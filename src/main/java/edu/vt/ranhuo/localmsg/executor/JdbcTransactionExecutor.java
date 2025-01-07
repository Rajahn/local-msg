package edu.vt.ranhuo.localmsg.executor;

import javax.sql.DataSource;
import java.sql.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JdbcTransactionExecutor implements TransactionExecutor {
    private static final Logger logger = LoggerFactory.getLogger(JdbcTransactionExecutor.class);

    private final DataSource dataSource;

    public JdbcTransactionExecutor(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public <T> T execute(Connection conn, TransactionCallback<T> action) throws Exception {
        boolean isNewConnection = (conn == null);
        Connection actualConn = conn;

        if (isNewConnection) {
            actualConn = dataSource.getConnection();
        }

        boolean originalAutoCommit = actualConn.getAutoCommit();
        try {
            // 设置手动提交模式
            if (originalAutoCommit) {
                actualConn.setAutoCommit(false);
            }

            // 执行业务逻辑
            T result = action.doInTransaction(actualConn);

            // 提交事务
            if (isNewConnection) {
                actualConn.commit();
            }

            return result;
        } catch (Exception e) {
            // 发生异常时回滚事务
            if (isNewConnection) {
                try {
                    actualConn.rollback();
                } catch (Exception ex) {
                    logger.error("Failed to rollback transaction", ex);
                }
            }
            throw e;
        } finally {
            // 恢复原始自动提交设置
            if (originalAutoCommit) {
                try {
                    actualConn.setAutoCommit(true);
                } catch (Exception ex) {
                    logger.error("Failed to restore auto-commit setting", ex);
                }
            }

            if (isNewConnection && actualConn != null) {
                try {
                    actualConn.close();
                } catch (Exception ex) {
                    logger.error("Failed to close connection", ex);
                }
            }
        }
    }
}
