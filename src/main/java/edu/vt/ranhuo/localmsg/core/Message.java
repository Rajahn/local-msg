package edu.vt.ranhuo.localmsg.core;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Message {
    private int partition;
    private String key;
    private String topic;
    private String content;
}
