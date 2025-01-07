package edu.vt.ranhuo.localmsg.config;

import java.time.Duration;
import java.util.Properties;

public interface Configuration {
    String getKafkaBootstrapServers();
    String getMessageTableName();
    Duration getMessageWaitDuration();
    int getMaxRetryTimes();
    int getBatchSize();
    Properties getDataSourceProperties();
}