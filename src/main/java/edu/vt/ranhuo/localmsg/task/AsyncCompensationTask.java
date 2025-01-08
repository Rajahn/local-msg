package edu.vt.ranhuo.localmsg.task;

import edu.vt.ranhuo.localmsg.core.LocalMessage;
import edu.vt.ranhuo.localmsg.core.MessageStatus;
import edu.vt.ranhuo.localmsg.dao.MessageDao;
import edu.vt.ranhuo.localmsg.dao.Query;
import edu.vt.ranhuo.localmsg.lock.DistributedLock;
import edu.vt.ranhuo.localmsg.service.DefaultMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncCompensationTask {
    private static final Logger logger = LoggerFactory.getLogger(AsyncCompensationTask.class);

    private final DefaultMessageService messageService;
    private final MessageDao messageDao;
    private final ScheduledExecutorService scheduler;
    private final ExecutorService taskExecutor;
    private final int batchSize;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private static final String COMPENSATE_LOCK_KEY = "compensate-task";
    private static final long LOCK_EXPIRE_SECONDS = 90;

    public AsyncCompensationTask(DefaultMessageService messageService,
                                 MessageDao messageDao,
                                 int batchSize) {
        this.messageService = messageService;
        this.messageDao = messageDao;
        this.batchSize = batchSize;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "async-compensation-scheduler");
            t.setDaemon(true);
            return t;
        });
        this.taskExecutor = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                r -> {
                    Thread t = new Thread(r, "async-compensation-worker");
                    t.setDaemon(true);
                    return t;
                }
        );
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            logger.warn("AsyncCompensationTask is already running");
            return;
        }

        // 每分钟执行一次补偿任务
        scheduler.scheduleWithFixedDelay(
                this::compensate,
                messageService.getWaitDuration().toSeconds(),
                60,
                TimeUnit.SECONDS
        );
    }

    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        scheduler.shutdown();
        taskExecutor.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            if (!taskExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                taskExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void compensate() {
        if (!running.get()) {
            return;
        }

        try (DistributedLock lock = new DistributedLock(
                COMPENSATE_LOCK_KEY,
                LOCK_EXPIRE_SECONDS,
                messageService.getTransactionExecutor(),
                messageDao,
                messageService.getTableName())) {

            // 尝试获取锁, 没有获取锁的实例直接返回, 等待下一次被调度
            if (!lock.tryLock()) {
                logger.debug("Another instance is running compensation task");
                return;
            }

            // 持续处理，直到没有消息或遇到失败
            while (running.get()) {
                List<LocalMessage> messages = findPendingMessages();
                if (messages.isEmpty()) {
                    break;
                }

                logger.info("Found {} messages to compensate", messages.size());

                boolean hasFailure = processBatch(messages);
                if (hasFailure) {
                    // 如果处理失败，退出循环，释放锁
                    logger.info("Encountered failures, releasing lock");
                    break;
                }
            }

        } catch (Exception e) {
            logger.error("Failed to execute compensation task", e);
        }
    }

    private boolean processBatch(List<LocalMessage> messages) {
        AtomicBoolean hasFailure = new AtomicBoolean(false);
        CompletableFuture<?>[] futures = messages.stream()
                .map(msg -> CompletableFuture.runAsync(() -> {
                    try {
                        messageService.handleMessageRetry(msg);
                    } catch (Exception e) {
                        hasFailure.set(true);
                        logger.error("Failed to handle message retry: id={}", msg.getId(), e);
                    }
                }, taskExecutor))
                .toArray(CompletableFuture[]::new);

        try {
            CompletableFuture.allOf(futures).join();
        } catch (Exception e) {
            hasFailure.set(true);
            logger.error("Failed to process message batch", e);
        }

        return hasFailure.get();
    }

    private List<LocalMessage> findPendingMessages() throws Exception {
        long threshold = System.currentTimeMillis() - messageService.getWaitDuration().toMillis();

        Query query = Query.builder()
                .table(messageService.getTableName())
                .status(MessageStatus.INIT)
                .endTime(threshold)
                .limit(batchSize)
                .build();

        return messageService.getTransactionExecutor().execute(null,
                conn -> messageDao.list(conn, query));
    }
}