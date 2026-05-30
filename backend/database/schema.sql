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

CREATE TABLE IF NOT EXISTS conversations (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '对话 ID',
  user_id BIGINT NOT NULL COMMENT '所属用户',
  title VARCHAR(100) NOT NULL COMMENT '对话标题',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_conversations_user_updated_at (user_id, updated_at),
  CONSTRAINT fk_conversations_user
    FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话表';

CREATE TABLE IF NOT EXISTS chat_messages (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息 ID',
  user_id BIGINT NOT NULL COMMENT '所属用户',
  conversation_id BIGINT NULL COMMENT '所属对话',
  role VARCHAR(20) NOT NULL COMMENT 'user 或 assistant',
  content TEXT NOT NULL COMMENT '消息内容',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (id),
  KEY idx_chat_messages_user_id (user_id),
  KEY idx_chat_messages_conversation_created_at (conversation_id, created_at),
  KEY idx_chat_messages_user_role_created_at (user_id, role, created_at),
  CONSTRAINT fk_chat_messages_user
    FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT fk_chat_messages_conversation
    FOREIGN KEY (conversation_id) REFERENCES conversations (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT chk_chat_messages_role
    CHECK (role IN ('user', 'assistant'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天消息表';

CREATE TABLE IF NOT EXISTS interview_sessions (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '面试会话 ID',
  user_id BIGINT NOT NULL COMMENT '所属用户',
  position VARCHAR(200) NOT NULL COMMENT '面试岗位',
  status VARCHAR(30) NOT NULL DEFAULT 'IN_PROGRESS'
    COMMENT '面试状态：IN_PROGRESS / TERMINATED / EVALUATING / COMPLETED',
  total_questions INT DEFAULT 0 COMMENT '生成的题目总数',
  answered_count INT DEFAULT 0 COMMENT '已回答的题目数',
  overall_score DECIMAL(4,1) DEFAULT NULL COMMENT '总体评分(0-100)',
  overall_comment TEXT DEFAULT NULL COMMENT '总体评价',
  study_advice TEXT DEFAULT NULL COMMENT '学习建议',
  started_at DATETIME DEFAULT NULL COMMENT '面试开始时间',
  ended_at DATETIME DEFAULT NULL COMMENT '面试结束时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_interview_sessions_user_id (user_id),
  KEY idx_interview_sessions_status (status),
  CONSTRAINT fk_interview_sessions_user
    FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模拟面试会话表';

CREATE TABLE IF NOT EXISTS interview_questions (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '题目 ID',
  session_id BIGINT NOT NULL COMMENT '所属面试会话',
  question_number INT NOT NULL COMMENT '题目序号(从1开始)',
  question_text TEXT NOT NULL COMMENT '题目内容',
  category VARCHAR(50) DEFAULT NULL COMMENT '题目分类',
  difficulty VARCHAR(20) DEFAULT NULL COMMENT '题目难度：基础/进阶/综合',
  user_answer TEXT DEFAULT NULL COMMENT '用户回答',
  score DECIMAL(4,1) DEFAULT NULL COMMENT 'AI评分(0-100)',
  comment TEXT DEFAULT NULL COMMENT 'AI对该题的评价',
  reference_answer TEXT DEFAULT NULL COMMENT 'AI提供的参考答案要点',
  answered_at DATETIME DEFAULT NULL COMMENT '用户回答时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_interview_questions_session_id (session_id),
  CONSTRAINT fk_interview_questions_session
    FOREIGN KEY (session_id) REFERENCES interview_sessions (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试题目与回答表';

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
