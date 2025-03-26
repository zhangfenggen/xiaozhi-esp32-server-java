-- 添加新字段到 sys_device 表
ALTER TABLE `xiaozhi`.`sys_device` 
ADD COLUMN `chipModelName` varchar(100) DEFAULT NULL COMMENT '芯片型号' AFTER `wifiName`,
ADD COLUMN `version` varchar(50) DEFAULT NULL COMMENT '固件版本' AFTER `chipModelName`;