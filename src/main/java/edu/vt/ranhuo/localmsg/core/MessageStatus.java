package edu.vt.ranhuo.localmsg.core;


public enum MessageStatus {
    INIT(0),
    SUCCESS(1),
    FAILED(2);

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
