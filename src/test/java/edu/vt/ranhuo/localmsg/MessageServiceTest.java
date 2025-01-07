package edu.vt.ranhuo.localmsg;

import edu.vt.ranhuo.localmsg.core.LocalMessage;
import edu.vt.ranhuo.localmsg.core.Message;
import edu.vt.ranhuo.localmsg.core.MessageStatus;
import edu.vt.ranhuo.localmsg.dao.MessageDao;
import edu.vt.ranhuo.localmsg.executor.TransactionExecutor;
import edu.vt.ranhuo.localmsg.producer.Producer;
import edu.vt.ranhuo.localmsg.service.DefaultMessageService;
import edu.vt.ranhuo.localmsg.support.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Connection;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageDao messageDao;
    @Mock
    private Producer producer;
    @Mock
    private TransactionExecutor transactionExecutor;
    @Mock
    private Connection connection;

    private DefaultMessageService messageService;
    private static final String TABLE_NAME = "local_msgs";

    @BeforeEach
    void setUp() {
        messageService = new DefaultMessageService(
                messageDao,
                producer,
                transactionExecutor,
                TABLE_NAME,
                2,
                Duration.ofSeconds(1)
        );
    }

    @Test
    void testSendMessageSuccess() throws Exception {
        // Arrange
        Message message = createTestMessage();

        // Act
        messageService.sendMessage(message);

        // Assert
        verify(producer, times(1)).send(message);
    }

    @Test
    void testSendMessageFailure() throws Exception {
        // Arrange
        Message message = createTestMessage();
        doThrow(new RuntimeException("Send failed"))
                .when(producer)
                .send(message);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> messageService.sendMessage(message));
    }

    @Test
    void testExecuteWithTransactionSuccess() throws Exception {
        // Arrange
        Message message = createTestMessage();
        LocalMessage savedMessage = createTestLocalMessage(message);
        savedMessage.setId(1L); // Set the ID

        // Mock transaction execution
        when(transactionExecutor.execute(any(), any())).thenAnswer(invocation -> {
            var callback = invocation.getArgument(1);
            return ((edu.vt.ranhuo.localmsg.executor.TransactionCallback<Message>)callback)
                    .doInTransaction(connection);
        });

        // Mock save method to set the ID
        doAnswer(invocation -> {
            LocalMessage localMessage = invocation.getArgument(2);
            localMessage.setId(1L); // Set the ID
            return null;
        }).when(messageDao).save(any(), eq(TABLE_NAME), any(LocalMessage.class));

        // Act
        messageService.executeWithTransaction(null, conn -> message);

        // Assert
        // Verify message was saved
        ArgumentCaptor<LocalMessage> messageCaptor = ArgumentCaptor.forClass(LocalMessage.class);
        verify(messageDao).save(any(), eq(TABLE_NAME), messageCaptor.capture());

        LocalMessage capturedMessage = messageCaptor.getValue();
        assertEquals(message.getKey(), capturedMessage.getKey());
        assertEquals(MessageStatus.INIT, capturedMessage.getStatus());
        assertEquals(0, capturedMessage.getSendTimes());
    }

    @Test
    void testHandleMessageRetrySuccess() throws Exception {
        // Arrange
        Message message = createTestMessage();
        LocalMessage localMessage = createTestLocalMessage(message);

        // Act
        messageService.handleMessageRetry(localMessage);

        // Assert
        verify(producer).send(any(Message.class));
        // Verify status update
        verify(messageDao).updateStatus(
                any(),
                eq(TABLE_NAME),
                eq(localMessage.getId()),
                eq(localMessage.getSendTimes() + 1),
                eq(MessageStatus.SUCCESS.getValue())
        );
    }

    @Test
    void testHandleMessageRetryFailure() throws Exception {
        // Arrange
        Message message = createTestMessage();
        LocalMessage localMessage = createTestLocalMessage(message);

        doThrow(new RuntimeException("Retry failed"))
                .when(producer)
                .send(any(Message.class));

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> messageService.handleMessageRetry(localMessage));

        // Verify status update
        verify(messageDao).updateStatus(
                any(),
                eq(TABLE_NAME),
                eq(localMessage.getId()),
                eq(localMessage.getSendTimes() + 1),
                eq(MessageStatus.INIT.getValue())
        );
    }

    @Test
    void testHandleMessageRetryMaxAttemptsReached() throws Exception {
        // Arrange
        Message message = createTestMessage();
        LocalMessage localMessage = createTestLocalMessage(message);
        // Set send times to max - 1, so next failure will reach max
        localMessage.setSendTimes(messageService.getMaxRetryTimes() - 1);

        doThrow(new RuntimeException("Retry failed"))
                .when(producer)
                .send(any(Message.class));

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> messageService.handleMessageRetry(localMessage));

        // Verify message is marked as failed
        verify(messageDao).updateStatus(
                any(),
                eq(TABLE_NAME),
                eq(localMessage.getId()),
                eq(messageService.getMaxRetryTimes()),
                eq(MessageStatus.FAILED.getValue())
        );
    }

    private Message createTestMessage() {
        return Message.builder()
                .topic("test-topic")
                .key("test-key")
                .content("test-content")
                .build();
    }

    private LocalMessage createTestLocalMessage(Message message) throws Exception {
        return LocalMessage.builder()
                .id(1L)
                .key(message.getKey())
                .data(JsonUtils.serialize(message))
                .sendTimes(0)
                .status(MessageStatus.INIT)
                .updateTime(System.currentTimeMillis())
                .createTime(System.currentTimeMillis())
                .build();
    }
}
