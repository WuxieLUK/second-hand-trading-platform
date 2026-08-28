-- =============================================================
-- AI 对话会话管理表
-- 支持多会话管理和长对话记忆持久化
-- 在 shop 数据库中执行
-- =============================================================

-- AI 对话会话表
CREATE TABLE IF NOT EXISTS `ai_chat_session` (
    `id`           VARCHAR(64)  NOT NULL COMMENT '会话ID（UUID）',
    `user_id`      INT          NOT NULL COMMENT '用户ID',
    `title`        VARCHAR(100) NOT NULL DEFAULT '新对话' COMMENT '会话标题',
    `message_count` INT         NOT NULL DEFAULT 0 COMMENT '消息总数',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后活动时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 对话会话';

-- AI 对话消息表
CREATE TABLE IF NOT EXISTS `ai_chat_message` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `session_id`   VARCHAR(64)  NOT NULL COMMENT '会话ID',
    `role`         VARCHAR(10)  NOT NULL COMMENT '角色：user / ai',
    `content`      TEXT         NOT NULL COMMENT '消息内容',
    `sequence`     INT          NOT NULL DEFAULT 0 COMMENT '消息序号',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_sequence` (`session_id`, `sequence`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 对话消息';
