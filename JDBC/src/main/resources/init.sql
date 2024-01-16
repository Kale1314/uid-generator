CREATE TABLE `t_worker_node`
(
    `id`         BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'auto increment id',
    `host_name`  VARCHAR(64)         NOT NULL COMMENT 'host name' COLLATE 'utf8mb4_bin',
    `port`       VARCHAR(64)         NOT NULL COMMENT 'port' COLLATE 'utf8mb4_bin',
    `type`       INT(10)             NOT NULL COMMENT 'node type: ACTUAL or CONTAINER',
    `launch_at`  DATE                NOT NULL COMMENT 'launch date',
    `updated_at` TIMESTAMP(6)        NOT NULL COMMENT 'modified time',
    `created_at` TIMESTAMP(6)        NOT NULL COMMENT 'created time',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `host_name_port` (`host_name`, `port`) USING BTREE
)
    COMMENT ='DB WorkerID Assigner for UID Generator'
    COLLATE = 'utf8mb4_bin'
    ENGINE = InnoDB
    AUTO_INCREMENT = 1
;
