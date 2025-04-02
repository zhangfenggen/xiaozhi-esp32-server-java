ALTER TABLE `xiaozhi`.`sys_role` 
ADD COLUMN `ttsId` int DEFAULT NULL COMMENT 'TTS服务ID' AFTER `voiceName`;

ALTER TABLE xiaozhi.sys_device DROP COLUMN ttsId;