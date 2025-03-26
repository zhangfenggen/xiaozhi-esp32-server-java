# 小智ESP32服务器部署文档

## 系统要求
- Java JDK 8
- MySQL数据库
- Maven（用于构建项目）
- Node.js和npm或yarn（用于前端构建）
- FFmpeg（用于音频处理，**必须安装**）

## 部署步骤

### 1. 安装FFmpeg（必需）
项目依赖FFmpeg进行音频处理，必须在系统中安装FFmpeg。

**Windows安装：**
1. 从官方网站下载FFmpeg: https://ffmpeg.org/download.html
2. 解压下载的文件
3. 将FFmpeg的bin目录添加到系统环境变量PATH中
4. 验证安装：打开命令行，输入 `ffmpeg -version`

**Linux安装：**
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install ffmpeg

# CentOS/RHEL
sudo yum install epel-release
sudo yum install ffmpeg ffmpeg-devel

# 验证安装
ffmpeg -version
```

**macOS安装：**

*因项目采用 VOSK 做本地 TTS 服务，而 VOSK 在 macOS 上有 bug ，因此后端项目暂时无法在 macOS 上运行。待修复后，再更新本节内容。*


### 2. 数据库配置
1. 创建名为`xiaozhi`的MySQL数据库
2. 使用提供的初始化脚本创建数据库结构：
   ```bash
   mysql -u root -p xiaozhi < db/init.sql
   ```
3. 默认数据库配置（可在application.properties中修改）：
   - 数据库URL: jdbc:mysql://localhost:3306/xiaozhi
   - 用户名: xiaozhi
   - 密码: 123456

### 3. 下载Vosk语音识别模型（重要）
项目使用Vosk进行语音识别，需要手动下载模型文件：

1. 从Vosk官方网站下载中文模型：https://alphacephei.com/vosk/models
2. 下载`vosk-model-cn-0.22`模型（或其他中文模型版本）
3. 解压下载的模型文件
4. 将解压后的模型文件夹放置在项目的`src/main/resources/`目录下
5. 确保模型文件夹路径为：`src/main/resources/vosk-model-cn-0.22`

### 4. 后端部署
1. 克隆项目代码到本地
2. 使用Maven构建项目：
   ```bash
   mvn clean package -DskipTests
   ```
3. 运行生成的jar文件：
   ```bash
   java -jar target/xiaozhi.server-1.0.jar
   ```

### 5. 前端部署
1. 进入web目录：
   ```bash
   cd web
   ```
2. 安装依赖：
   ```bash
   # npm
   npm install
   # yarn
   yarn install
   ```
3. 构建前端项目：
   ```bash
   # npm
   npm run dev
   # yarn
   yarn run dev
   ```
4. 前端构建完成后，前端项目将自动运行在默认的开发服务器上

### 6. 访问系统
- 使用浏览器访问 `http://localhost:8084`，即可访问系统，默认用户名为`admin`，密码为`123456`
- 后端服务启动后，控制台会输出 WebSocket 连接信息，用于设备通信

## 注意事项
1. **FFmpeg安装**：必须正确安装FFmpeg，否则音频处理功能将无法使用
2. **Vosk模型文件**：必须手动下载并放置在正确位置，否则语音识别功能无法使用
3. 确保MySQL服务已启动并且配置正确
4. 如需修改端口或其他配置，请编辑`src/main/resources/application.properties`文件
5. 项目默认使用的音频文件存储路径为`audio/`

## 文件目录说明
- `src/main/java/com/xiaozhi`：后端Java代码
- `src/main/resources`：配置文件和资源文件
- `web`：前端Vue.js代码
- `db`：数据库初始化脚本

## 常见问题
1. **语音识别失败**：检查Vosk模型是否正确下载并放置在`src/main/resources/vosk-model-cn-0.22`目录下
2. **音频处理失败**：检查FFmpeg是否正确安装，可通过命令行运行`ffmpeg -version`验证
3. **数据库连接失败**：检查MySQL服务是否启动，以及用户名密码是否正确
4. **端口冲突**：如果8091端口被占用，可以在application.properties中修改server.port配置，并修改前端代码中的API地址

## 技术支持
如果在部署过程中遇到问题，请参考项目的README.md文件或联系项目维护者。