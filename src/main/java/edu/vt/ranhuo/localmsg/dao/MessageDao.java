package edu.vt.ranhuo.localmsg.dao;

import edu.vt.ranhuo.localmsg.core.LocalMessage;

import java.sql.Connection;
import java.util.List;

public interface MessageDao {
    LocalMessage get(Connection conn, String table, long id) throws Exception;
    List<LocalMessage> list(Connection conn, Query query) throws Exception;
    void save(Connection conn, String table, LocalMessage message) throws Exception;
    void updateStatus(Connection conn, String table, long id, int sendTimes, int status) throws Exception;
}