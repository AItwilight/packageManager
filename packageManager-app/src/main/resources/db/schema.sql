-- 末端驿站包裹管理系统 DDL

CREATE DATABASE IF NOT EXISTS package_manager
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE package_manager;

-- 包裹表
CREATE TABLE IF NOT EXISTS package (
    id           BIGINT         AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    biz_id       VARCHAR(32)    NOT NULL UNIQUE COMMENT '业务ID',
    waybill_no   VARCHAR(64)    NOT NULL COMMENT '运单号',
    phone        VARCHAR(20)    NOT NULL COMMENT '收件人手机号',
    courier      VARCHAR(32)    NOT NULL COMMENT '快递公司编码',
    shelf        VARCHAR(64)    NOT NULL COMMENT '货架位置',
    status       TINYINT        NOT NULL DEFAULT 0 COMMENT '0待取件 1已取件',
    checkin_time DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    pickup_time  DATETIME       NULL COMMENT '取件时间',
    create_time  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_phone (phone),
    INDEX idx_status (status),
    INDEX idx_waybill_no (waybill_no),
    INDEX idx_checkin_time (checkin_time)
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
