-- 1. 创建临时表备份 sys_device 数据
CREATE TABLE `xiaozhi`.`temp_sys_device` AS SELECT * FROM `xiaozhi`.`sys_device`;

-- 2. 创建临时表备份 sys_model 数据
CREATE TABLE `xiaozhi`.`temp_sys_model` AS SELECT * FROM `xiaozhi`.`sys_model`;

-- 3. 修改 sys_device 表结构（将 modelId 改为 configId）
ALTER TABLE `xiaozhi`.`sys_device` CHANGE COLUMN `modelId` `configId` int DEFAULT NULL COMMENT '模型ID';

-- 4. 创建新的 sys_config 表
CREATE TABLE `xiaozhi`.`sys_config` (
  `configId` int unsigned NOT NULL AUTO_INCREMENT COMMENT '配置ID，主键',
  `userId` int NOT NULL COMMENT '创建用户ID',
  `configType` varchar(30) NOT NULL DEFAULT 'llm' COMMENT '配置类型(llm, stt, tts等)',
  `provider` varchar(30) NOT NULL COMMENT '服务提供商(openai, qwen, vosk, aliyun, tencent等)',
  `configName` varchar(50) NOT NULL COMMENT '配置名称',
  `configDesc` TEXT DEFAULT NULL COMMENT '配置描述',
  `appId` varchar(100) DEFAULT NULL COMMENT 'APP ID',
  `apiKey` varchar(255) DEFAULT NULL COMMENT 'API密钥',
  `apiSecret` varchar(255) DEFAULT NULL COMMENT 'API密钥',
  `apiUrl` varchar(255) DEFAULT NULL COMMENT 'API地址',
  `isDefault` enum('1','0') DEFAULT '0' COMMENT '是否为默认配置: 1-是, 0-否',
  `state` enum('1','0') DEFAULT '1' COMMENT '状态：1-启用，0-禁用',
  `createTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`configId`),
  KEY `userId` (`userId`),
  KEY `configType` (`configType`),
  KEY `provider` (`provider`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表(模型、语音识别、语音合成等)';

-- 5. 从 sys_model 迁移数据到 sys_config
INSERT INTO `xiaozhi`.`sys_config` 
  (`configId`, `userId`, `configType`, `provider`, `configName`, `configDesc`, 
   `appId`, `apiKey`, `apiSecret`, `apiUrl`, `state`, `createTime`)
SELECT 
  `modelId`, `userId`, 'llm', `type`, `modelName`, `modelDesc`, 
  `appId`, `apiKey`, `apiSecret`, `apiUrl`, `state`, `createTime`
FROM `xiaozhi`.`temp_sys_model`;

-- 6. 设置第一条记录为默认配置
UPDATE `xiaozhi`.`sys_config` SET `isDefault` = '1' WHERE `configId` = (SELECT MIN(`configId`) FROM `xiaozhi`.`sys_config`);

-- 7. 删除临时表与旧表（确认数据迁移成功手动执行）
-- DROP TABLE `xiaozhi`.`temp_sys_device`;
-- DROP TABLE `xiaozhi`.`temp_sys_model`;
-- DROP TABLE `xiaozhi`.`sys_model`;