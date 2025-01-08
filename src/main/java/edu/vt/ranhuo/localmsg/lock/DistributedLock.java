package edu.vt.ranhuo.localmsg.lock;

import edu.vt.ranhuo.localmsg.core.LocalMessage;
import edu.vt.ranhuo.localmsg.core.MessageStatus;
import edu.vt.ranhuo.localmsg.dao.MessageDao;
import edu.vt.ranhuo.localmsg.executor.TransactionExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class DistributedLock implements AutoCloseable {
    private final String lockKey;
    private final String value;
    private final long expireTime;
    private final TransactionExecutor transactionExecutor;
    private final MessageDao messageDao;
    private final String tableName;
    private boolean locked = false;

    private static final Logger logger = LoggerFactory.getLogger(DistributedLock.class);

    public DistributedLock(String lockKey,
                           long expireSeconds,
                           TransactionExecutor transactionExecutor,
                           MessageDao messageDao,
                           String tableName) {
        this.lockKey = "lock:" + lockKey;
        this.value = UUID.randomUUID().toString();
        this.expireTime = expireSeconds * 1000;
        this.transactionExecutor = transactionExecutor;
        this.messageDao = messageDao;
        this.tableName = tableName;
    }

    public boolean tryLock() {
        try {
            long now = System.currentTimeMillis();

            // 尝试插入锁记录
            LocalMessage lockRecord = LocalMessage.builder()
                    .key(lockKey)
                    .data(value.getBytes())
                    .sendTimes(0)
                    .status(MessageStatus.LOCK_HOLDING)
                    .updateTime(now + expireTime)
                    .createTime(now)
                    .build();

            try {
                messageDao.save(null, tableName, lockRecord);
                locked = true;
                return true;
            } catch (Exception e) {
                // 阻塞, 尝试获取已经存在的锁
                return tryAcquireExistingLock(now);
            }
        } catch (Exception e) {
            return false;
        }
    }


    private boolean tryAcquireExistingLock(long now) throws Exception {
        // 查找并尝试获取过期的锁
        LocalMessage existingLock = messageDao.findByKey(null, tableName, lockKey);
        if (existingLock == null ||
                (existingLock.getStatus() == MessageStatus.LOCK_HOLDING &&
                        now < existingLock.getUpdateTime())) {
            return false;
        }

        // 尝试更新锁状态
        int updated = messageDao.updateLock(null, tableName, lockKey,
                value.getBytes(),
                MessageStatus.LOCK_HOLDING,
                now + expireTime);

        locked = updated > 0;
        return locked;
    }

    public void unlock() {
        if (!locked) {
            return;
        }
        try {
            int updated = messageDao.updateLock(null, tableName, lockKey,
                    value.getBytes(),
                    MessageStatus.LOCK_RELEASED,  // 使用 LOCK_RELEASED 状态
                    System.currentTimeMillis());  // 设置为当前时间，表示锁已释放

            if (updated > 0) {
                locked = false;
            }
        } catch (Exception e) {
            logger.error("Failed to release lock: {}", lockKey, e);
        }
    }

    @Override
    public void close() {
        unlock();
    }
}