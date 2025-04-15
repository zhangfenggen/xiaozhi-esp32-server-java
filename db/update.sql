ALTER TABLE `xiaozhi`.`sys_code` 
ADD COLUMN `email` varchar(100) DEFAULT NULL COMMENT '邮箱' AFTER `deviceId`;

ALTER TABLE xiaozhi.sys_code MODIFY COLUMN deviceId varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '设备ID';
ALTER TABLE xiaozhi.sys_code MODIFY COLUMN email varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '邮箱';
ALTER TABLE xiaozhi.sys_code MODIFY COLUMN sessionId varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'sessionID';

ALTER TABLE `xiaozhi`.`sys_user` 
ADD COLUMN `avatar` varchar(100) DEFAULT NULL COMMENT '头像' AFTER `state`;