package edu.vt.ranhuo.localmsg.core;


public enum MessageStatus {
    INIT(0),
    SUCCESS(1),
    FAILED(2),
    LOCK_HOLDING(3), //复用消息表status字段 实现分布式锁
    LOCK_RELEASED(4);

    private final int value;

    MessageStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static MessageStatus fromValue(int value) {
        for (MessageStatus status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid MessageStatus value: " + value);
    }
}
