ALTER TABLE `xiaozhi`.`sys_device` 
CHANGE COLUMN `configId` `modelId` int NULL COMMENT '模型ID',
ADD COLUMN `sttId` int DEFAULT NULL COMMENT 'STT服务ID' AFTER `modelId`,
ADD COLUMN `ttsId` int DEFAULT NULL COMMENT 'TTS服务ID' AFTER `sttId`;