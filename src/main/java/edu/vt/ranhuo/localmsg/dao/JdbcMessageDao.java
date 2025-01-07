package edu.vt.ranhuo.localmsg.dao;

import edu.vt.ranhuo.localmsg.core.LocalMessage;
import edu.vt.ranhuo.localmsg.core.MessageStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JdbcMessageDao implements MessageDao {

    @Override
    public LocalMessage get(Connection conn, String table, long id) throws Exception {
        String sql = String.format("SELECT * FROM %s WHERE id = ?", table);
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractLocalMessage(rs);
                }
            }
        }
        return null;
    }

    @Override
    public List<LocalMessage> list(Connection conn, Query query) throws Exception {
        StringBuilder sql = new StringBuilder();
        sql.append(String.format("SELECT * FROM %s WHERE 1=1", query.getTable()));
        List<Object> params = new ArrayList<>();

        if (query.getStatus() != null) {
            sql.append(" AND status = ?");
            params.add(query.getStatus().getValue());
        }
        if (query.getKey() != null && !query.getKey().isEmpty()) {
            sql.append(" AND `key` = ?");
            params.add(query.getKey());
        }
        if (query.getStartTime() > 0) {
            sql.append(" AND update_time >= ?");
            params.add(query.getStartTime());
        }
        if (query.getEndTime() > 0) {
            sql.append(" AND update_time <= ?");
            params.add(query.getEndTime());
        }

        sql.append(" ORDER BY id DESC LIMIT ? OFFSET ?");
        params.add(query.getLimit());
        params.add(query.getOffset());

        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                List<LocalMessage> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(extractLocalMessage(rs));
                }
                return results;
            }
        }
    }

    @Override
    public void save(Connection conn, String table, LocalMessage message) throws Exception {
        String sql = String.format(
                "INSERT INTO %s (`key`, data, send_times, status, update_time, create_time) VALUES (?, ?, ?, ?, ?, ?)",
                table
        );
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, message.getKey());
            stmt.setBytes(2, message.getData());
            stmt.setInt(3, message.getSendTimes());
            stmt.setInt(4, message.getStatus().getValue());
            stmt.setLong(5, message.getUpdateTime());
            stmt.setLong(6, message.getCreateTime());

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    message.setId(rs.getLong(1));
                }
            }
        }
    }

    @Override
    public void updateStatus(Connection conn, String table, long id, int sendTimes, int status) throws Exception {
        String sql = String.format(
                "UPDATE %s SET status = ?, send_times = ?, update_time = ? WHERE id = ?",
                table
        );
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, status);
            stmt.setInt(2, sendTimes);
            stmt.setLong(3, System.currentTimeMillis());
            stmt.setLong(4, id);
            stmt.executeUpdate();
        }
    }

    private LocalMessage extractLocalMessage(ResultSet rs) throws SQLException {
        return LocalMessage.builder()
                .id(rs.getLong("id"))
                .key(rs.getString("key"))
                .data(rs.getBytes("data"))
                .sendTimes(rs.getInt("send_times"))
                .status(MessageStatus.fromValue(rs.getInt("status")))
                .updateTime(rs.getLong("update_time"))
                .createTime(rs.getLong("create_time"))
                .build();
    }
}