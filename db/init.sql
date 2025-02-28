-- xiaozhi.sys_message definition

CREATE TABLE `sys_message` (
  `messageId` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID，主键，自增',
  `deviceId` int NOT NULL COMMENT '设备ID，关联sys_device表',
  `sender` enum('user','ai') NOT NULL COMMENT '消息发送方：user-用户，ai-人工智能',
  `message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '消息内容',
  `filePath` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '语言文件路径',
  `createTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消息发送时间',
  PRIMARY KEY (`messageId`),
  KEY `deviceId` (`deviceId`),
  CONSTRAINT `sys_message_ibfk_1` FOREIGN KEY (`deviceId`) REFERENCES `sys_device` (`deviceId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人与AI对话消息表';

-- xiaozhi.sys_user definition

CREATE TABLE `sys_user` (
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
  `principalId` int DEFAULT NULL,
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP,
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`userId`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- xiaozhi.sys_device definition

CREATE TABLE `sys_device` (
  `deviceId` int NOT NULL AUTO_INCREMENT COMMENT '设备ID，主键，自增',
  `deviceName` varchar(100) NOT NULL COMMENT '设备名称',
  `ip` varchar(45) NOT NULL COMMENT 'IP地址',
  `wifiName` varchar(100) DEFAULT NULL COMMENT 'WiFi名称',
  `wifiPassword` varchar(100) DEFAULT NULL COMMENT 'WiFi密码',
  `mac` varchar(17) NOT NULL COMMENT 'MAC地址',
  `state` enum('1','0') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '1' COMMENT '设备状态：1-在线，0-离线',
  `principalId` int NOT NULL COMMENT '创建人',
  `createTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`deviceId`),
  UNIQUE KEY `unique_mac` (`mac`) COMMENT 'MAC地址唯一约束'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备信息表';