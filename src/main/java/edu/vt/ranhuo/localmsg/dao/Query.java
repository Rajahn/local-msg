package edu.vt.ranhuo.localmsg.dao;

import edu.vt.ranhuo.localmsg.core.MessageStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Query {
    private String table;
    private int offset;
    private int limit;
    private MessageStatus status;
    private String key;
    private long startTime;
    private long endTime;
}