-- 末端驿站包裹管理系统 DDL

CREATE DATABASE IF NOT EXISTS package_manager
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE package_manager;

-- 每日序列号表（用于生成取件码流水号）
CREATE TABLE IF NOT EXISTS daily_sequence (
    date_key    DATE        NOT NULL PRIMARY KEY COMMENT '日期',
    seq_no      INT         NOT NULL DEFAULT 0 COMMENT '当日序列号(1-9999)',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日序列号表';

-- 包裹表
CREATE TABLE IF NOT EXISTS package (
    id           int(11)         AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    biz_id       VARCHAR(32)    NOT NULL UNIQUE COMMENT '业务ID',
    waybill_no   VARCHAR(64)    NOT NULL COMMENT '运单号',
    phone        VARCHAR(20)    NOT NULL COMMENT '收件人手机号',
    courier      VARCHAR(32)    NOT NULL COMMENT '快递公司编码',
    shelf        VARCHAR(4)     NOT NULL COMMENT '货架位置(格式:大写字母-两位数字,如A-13)',
    status       TINYINT        NOT NULL DEFAULT 0 COMMENT '0待取件 1已取件',
    checkin_time DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    pickup_time  DATETIME       NULL COMMENT '取件时间',
    pickup_code  VARCHAR(10)    NULL COMMENT '取件码(货架-流水号,如A-13-0001)',
    user_id      BIGINT         NULL COMMENT '入库操作人ID',
    phone_suffix VARCHAR(4)     GENERATED ALWAYS AS (RIGHT(phone, 4)) STORED COMMENT '手机号后四位',
    create_time  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_phone (phone),
    INDEX idx_status (status),
    INDEX idx_waybill_no (waybill_no),
    INDEX idx_checkin_time (checkin_time),
    INDEX idx_pickup_code (pickup_code),
    INDEX idx_phone_suffix (phone_suffix),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='包裹表';

-- 系统用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT         AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(32)    NOT NULL UNIQUE COMMENT '用户名',
    password    VARCHAR(128)   NOT NULL COMMENT 'BCrypt加密密码',
    create_time DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 初始化管理员: admin / admin123
INSERT INTO sys_user (username, password) VALUES ('admin', '$2a$10$IH2zGFqSObWXNUX7yvdkdey92XBCc3AbxkX.FpYZKjaXilNmWqIhW');

-- ============================================================
-- 迁移脚本（已有数据库增量执行）
-- ============================================================
-- ALTER TABLE package MODIFY COLUMN shelf VARCHAR(4) NOT NULL COMMENT '货架位置(格式:大写字母-两位数字,如A-13)';
-- ALTER TABLE package ADD COLUMN pickup_code VARCHAR(10) NULL COMMENT '取件码' AFTER pickup_time;
-- ALTER TABLE package ADD COLUMN user_id BIGINT NULL COMMENT '入库操作人ID' AFTER pickup_code;
-- ALTER TABLE package ADD COLUMN phone_suffix VARCHAR(4) GENERATED ALWAYS AS (RIGHT(phone, 4)) STORED COMMENT '手机号后四位' AFTER user_id;
-- ALTER TABLE package ADD INDEX idx_pickup_code (pickup_code);
-- ALTER TABLE package ADD INDEX idx_phone_suffix (phone_suffix);
-- ALTER TABLE package ADD INDEX idx_user_id (user_id);
