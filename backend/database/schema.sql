CREATE DATABASE IF NOT EXISTS interview_assistant
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE interview_assistant;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户唯一 ID',
  username VARCHAR(50) NOT NULL COMMENT '登录用户名',
  password VARCHAR(255) NOT NULL COMMENT 'BCrypt 加密后的密码',
  email VARCHAR(100) NULL COMMENT '邮箱',
  avatar_url VARCHAR(255) NULL COMMENT '头像链接',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS chat_messages (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息 ID',
  user_id BIGINT NOT NULL COMMENT '所属用户',
  role VARCHAR(20) NOT NULL COMMENT 'user 或 assistant',
  content TEXT NOT NULL COMMENT '消息内容',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (id),
  KEY idx_chat_messages_user_id (user_id),
  CONSTRAINT fk_chat_messages_user
    FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT chk_chat_messages_role
    CHECK (role IN ('user', 'assistant'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天消息表';

CREATE TABLE IF NOT EXISTS resumes (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '简历 ID',
  user_id BIGINT NOT NULL COMMENT '所属用户',
  name VARCHAR(50) NOT NULL COMMENT '姓名',
  email VARCHAR(100) NULL COMMENT '邮箱',
  phone VARCHAR(20) NULL COMMENT '电话',
  education TEXT NULL COMMENT '教育经历 JSON 数组',
  skills TEXT NULL COMMENT '技能列表 JSON 数组',
  experience TEXT NULL COMMENT '工作/实习经历 JSON 数组',
  projects TEXT NULL COMMENT '项目经历 JSON 数组',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_resumes_user_id (user_id),
  CONSTRAINT fk_resumes_user
    FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历表';
