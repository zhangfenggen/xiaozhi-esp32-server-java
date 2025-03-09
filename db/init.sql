-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS xiaozhi;

-- 创建本地用户并设置密码（使用mysql_native_password插件）
CREATE USER 'xiaozhi'@'localhost' IDENTIFIED WITH mysql_native_password BY '123456';

-- 创建远程用户并设置密码（使用mysql_native_password插件）
CREATE USER 'xiaozhi'@'%' IDENTIFIED WITH mysql_native_password BY '123456';

-- 仅授予本地用户对 xiaozhi 数据库的所有权限
GRANT ALL PRIVILEGES ON xiaozhi.* TO 'xiaozhi'@'localhost';

-- 仅授予远程用户对 xiaozhi 数据库的所有权限
GRANT ALL PRIVILEGES ON xiaozhi.* TO 'xiaozhi'@'%';

-- 刷新权限以使更改生效
FLUSH PRIVILEGES;

-- 查看用户权限
SHOW GRANTS FOR 'xiaozhi'@'localhost';
SHOW GRANTS FOR 'xiaozhi'@'%';


CREATE DATABASE IF NOT EXISTS `xiaozhi` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- xiaozhi.sys_user definition

CREATE TABLE `xiaozhi`.`sys_user` (
  `userId` int unsigned NOT NULL AUTO_INCREMENT,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `tel` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `state` enum('1','0') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '1',
  `loginIp` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `isAdmin` enum('1','0') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `loginTime` datetime DEFAULT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP,
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`userId`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO xiaozhi.sys_user (username,password,tel,email,state,loginIp,isAdmin,loginTime,name,createTime,updateTime) VALUES
	 ('admin','11cd9c061d614dcf37ec60c44c11d2ad',NULL,NULL,'1',NULL,'1',NULL,'小智','2025-03-09 18:32:29','2025-03-09 18:32:35');

-- xiaozhi.sys_device definition

CREATE TABLE `xiaozhi`.`sys_device` (
  `deviceId` varchar(255) NOT NULL COMMENT '设备ID，主键',
  `deviceName` varchar(100) NOT NULL COMMENT '设备名称',
  `ip` varchar(45) DEFAULT NULL COMMENT 'IP地址',
  `wifiName` varchar(100) DEFAULT NULL COMMENT 'WiFi名称',
  `wifiPassword` varchar(100) DEFAULT NULL COMMENT 'WiFi密码',
  `state` enum('1','0') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '1' COMMENT '设备状态：1-在线，0-离线',
  `userId` int NOT NULL COMMENT '创建人',
  `createTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `lastLogin` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后登录时间',
  PRIMARY KEY (`deviceId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备信息表';


-- xiaozhi.sys_message definition

CREATE TABLE `xiaozhi`.`sys_message` (
  `messageId` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID，主键，自增',
  `deviceId` varchar(30) NOT NULL COMMENT '设备ID，关联sys_device表',
  `sender` enum('user','ai') NOT NULL COMMENT '消息发送方：user-用户，ai-人工智能',
  `message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '消息内容',
  `audioPath` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '语言文件路径',
  `createTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消息发送时间',
  PRIMARY KEY (`messageId`),
  KEY `deviceId` (`deviceId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人与AI对话消息表';

-- xiaozhi.sys_code definition

CREATE TABLE `xiaozhi`.`sys_code` (
  `codeId` int unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` varchar(100) NOT NULL COMMENT '验证码',
  `deviceId` varchar(30) NOT NULL COMMENT '设备ID',
  `sessionId` varchar(100) NOT NULL COMMENT 'sessionID',
  `audioPath` text COMMENT '语音文件路径',
  `createTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`codeId`),
  KEY `deviceId` (`deviceId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='验证码表';

