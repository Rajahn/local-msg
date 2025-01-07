package edu.vt.ranhuo.localmsg.core;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LocalMessage {
    private Long id;
    private String key;
    private byte[] data;
    private int sendTimes;
    private MessageStatus status;
    private long updateTime;
    private long createTime;
}