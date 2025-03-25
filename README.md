# Xiaozhi ESP32 Server Java

## 项目简介

Xiaozhi ESP32 Server Java 是基于 [Xiaozhi ESP32](https://github.com/78/xiaozhi-esp32) 项目开发的 JAVA 版本的服务端，包含前后端管理平台。由于目前缺乏一个直观、高效的前端 Web 平台，并且当前存在的开源 Python Server 显得项目太臃肿，现采用 JAVA Server与后端服务来管理设备和相关功能，因此本项目应运而生。

该项目旨在为用户提供一个功能丰富、操作便捷的管理界面，帮助用户更好地管理设备、配置等。
Server端采用 Spring Boot 框架，前端采用 Vue.js 框架，全局响应式布局，适配各种设备以及分辨率，使用 MySQL 数据库进行数据存储。

---

## 功能模块（设计中）

### 1. **设备管理**
   - 查看已接入的所有设备列表。（已完成）
   - 设备状态实时监控（在线/离线、运行状态等）。（已完成）
   - 添加、编辑、删除设备信息。（已完成）

### 2. **音色选择**
   - 提供多种音色模板供用户选择。（已完成）
   - 预览音色效果。（已完成）
   - 为不同设备分配不同的音色配置。（已完成）

### 3. **聊天记录**
   - 查看与设备的历史聊天记录。（已完成）
   - 支持按日期、关键词搜索聊天内容。（已完成）
   - 聊天数据可视化（如聊天频率统计图表）。

### 4. **唤醒词修改**
  - 查看与修改唤醒词。

### 5. **角色切换**
  - 预设角色切换（AI老师、男/女朋友、智能家居助手）（已完成）
  - 混合模式

### 6. **持久化对话**
  - 支持持久化对话记录，方便用户查看历史对话内容。（已完成）
  - 自定义记忆对话条数。
  - 历史对话总结\摘要功能。
  - 支持手动操作对话记录。


## UI样式
<div align="center">
  <img src="docs/images/login.jpg" alt="login" width="600" style="margin: 10px;" />
  <img src="docs/images/dashboard.jpg" alt="dashboard" width="600" style="margin: 10px;" />
  <img src="docs/images/device.jpg" alt="device" width="600" style="margin: 10px;" />
  <img src="docs/images/message.jpg" alt="message" width="600" style="margin: 10px;" />
  <img src="docs/images/model.jpg" alt="model" width="600" style="margin: 10px;" />
  <img src="docs/images/role.jpg" alt="role" width="600" style="margin: 10px;" />
</div>

---

## 技术栈

- **前端框架**：Vue.js
- **样式库**：Ant Design
- **后端支持**：Java + 基于 Xiaozhi ESP32 提供的 API
- **数据库**：MySQL（用于存储用户数据、聊天记录等）
  
---

## 开发计划

- **阶段 1**：实现基础功能（设备管理、音色选择、聊天记录查看）。（已完成）
- **阶段 2**：完善用户管理、唤醒词修改、日志管理模块。
- **阶段 3**：增加数据可视化功能、增加流式对话记录。

---

## 使用方式

### 1. 本地源码运行
点击此处查看[部署文档](./docs/DEVELOPMENT.md)

成功运行后，控制台会输出 WebSocket 连接地址，根据下方固件编译文档使设备接入服务使用。

### 2. 固件编译
点这里查看[固件编译](./docs/FIRMWARE-BUILD.md)的详细过程。

烧录成功且联网成功后，通过唤醒词唤醒小智，留意server端输出的控制台信息。

---

## 贡献指南

欢迎任何形式的贡献！如果你有好的想法或发现问题，请联系反馈

<img src="./web/static/img/wechat.jpg" alt="微信" width="200" />

---
