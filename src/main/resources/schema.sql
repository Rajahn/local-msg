-- schema.sql

CREATE TABLE local_msgs (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            `key` VARCHAR(191),
                            data TEXT,
                            send_times INT NOT NULL DEFAULT 0,
                            status TINYINT NOT NULL DEFAULT 0,
                            update_time BIGINT NOT NULL,
                            create_time BIGINT NOT NULL,
                            INDEX idx_key (`key`),
                            INDEX idx_status_update_time (status, update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;