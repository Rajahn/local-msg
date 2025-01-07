package edu.vt.ranhuo.localmsg.executor;


import java.sql.Connection;

@FunctionalInterface
public interface TransactionCallback<T> {
    T doInTransaction(Connection conn) throws Exception;
}
