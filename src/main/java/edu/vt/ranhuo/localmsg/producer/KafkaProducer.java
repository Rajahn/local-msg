package edu.vt.ranhuo.localmsg.producer;

import edu.vt.ranhuo.localmsg.core.Message;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaProducer implements Producer {
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducer.class);

    private final org.apache.kafka.clients.producer.KafkaProducer<String, String> kafkaProducer;

    public KafkaProducer(org.apache.kafka.clients.producer.KafkaProducer<String, String> kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }

    @Override
    public void send(Message message) throws Exception {
        ProducerRecord<String, String> record;
        if (message.getPartition() >= 0) {
            record = new ProducerRecord<>(
                    message.getTopic(),
                    message.getPartition(),
                    message.getKey(),
                    message.getContent()
            );
        } else {
            record = new ProducerRecord<>(
                    message.getTopic(),
                    message.getKey(),
                    message.getContent()
            );
        }

        try {
            kafkaProducer.send(record).get();
            logger.debug("Message sent successfully: topic={}, key={}",
                    message.getTopic(), message.getKey());
        } catch (Exception e) {
            logger.error("Failed to send message: topic={}, key={}",
                    message.getTopic(), message.getKey(), e);
            throw new RuntimeException("Failed to send message", e);
        }
    }

    public void close() {
        if (kafkaProducer != null) {
            kafkaProducer.close();
        }
    }

    // Factory method for creating KafkaProducer with default settings
    public static KafkaProducer create(String bootstrapServers) {
        java.util.Properties props = new java.util.Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        // 设置acks=all以确保消息可靠性
        props.put("acks", "all");
        // 设置重试次数
        props.put("retries", 3);
        // 启用幂等性
        props.put("enable.idempotence", true);

        return new KafkaProducer(
                new org.apache.kafka.clients.producer.KafkaProducer<>(props)
        );
    }
}