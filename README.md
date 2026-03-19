# 软件创新大赛项目 - AI Education (智能教育助手)

## 项目概览 (Project Overview)
本项目是一个集成了智能辅导、历史时间轴可视化、几何解题和视频总结功能的多模块 Android 教育应用。项目采用现代 Android 开发技术栈，注重模块化设计与代码复用，旨在通过人工智能技术提升学生的学习效率和体验。

## 核心技术栈 (Tech Stack)
- **开发语言**: Kotlin 1.9+
- **UI 框架**: Jetpack Compose (Material3)
- **架构模式**: MVVM, Clean Architecture (Data/Domain/Presentation 隔离)
- **异步与响应式编程**: Kotlin Coroutines, StateFlow / SharedFlow
- **网络通信**: Retrofit, OkHttp
- **本地存储**: Room (SQLite), DataStore (替代 SharedPreferences)
- **多媒体与设备**: CameraX (图像采集), ExoPlayer / Media3 (视频播放)
- **语音识别与处理**: Vosk-Small-CN (离线轻量级语音识别), Sherpa-onnx (高性能离线语音转写)
- **视频处理**: FFmpegKit (音视频提取), YoutubeDL-Android (视频解析下载)
- **AI 大模型接入**: 阿里云百炼平台 (Qwen-Turbo 文本对话, Qwen-VL 多模态视觉)
- **构建系统**: Gradle (Kotlin DSL), Version Catalog 依赖管理

---

## 项目架构与模块说明 (Project Architecture)

本项目采用多模块 (Multi-Module) 架构，按功能特性进行垂直拆分，并抽取公共基础设施，以实现高内聚、低耦合。

```text
AI_Education/
├── app/                  # 主应用壳工程 (负责应用入口、全局配置和最终打包)
├── common/               # 公共基础模块 (UI组件、网络/数据库配置、工具类、统一资源)
├── ai_tutor/             # 智能辅导模块 (核心对话、多模态输入、Agent决策)
├── timeline_map/         # 历史时间轴地图模块 (OSM地图展示、事件演变可视化)
├── video_summarizer/     # 视频总结模块 (视频下载、音频提取、离线ASR转写、大模型总结)
└── geometry_solver/      # 几何解题模块 (占位，待开发)
```

---

## 模块详细开发进度 (Detailed Development Progress)

### 1. 智能辅导模块 (`:ai_tutor`)
该模块是应用的核心交互入口，提供基于 AI 的多模态（文本、语音、图像）辅导服务。
- **状态**: 🟢 开发中 (主要功能已完成并集成)
- **已实现功能**:
    - [x] **多模态对话界面**: 基于 Compose 实现的流畅聊天界面 (`ChatScreen`)，已全面适配深色模式。
    - [x] **大模型对接**: 集成 `QwenRepository` 对接阿里云百炼 Qwen-Turbo (文本) 和 Qwen-VL (视觉理解) 模型。
    - [x] **Agent 意图识别**: 实现 `AgentDecisionHub`，初步支持区分通用聊天与特定指令。
    - [x] **本地数据持久化**: 使用 Room 数据库 (`ChatDao`, `UserDao`) 保存聊天会话 (`ChatSessionEntity`) 和消息记录 (`MessageEntity`)。
    - [x] **图像处理与识别**: 集成自定义相机 (`CameraScreen`)，支持拍照预览，图片自动 Base64 编码上传至 Qwen-VL。
    - [x] **主界面导航框架**: `MainScreen` 实现底部导航栏，整合各个子功能模块的入口。
- **待开发功能**:
    - [ ] **知识库增强 (RAG)**: 引入本地或云端向量数据库，支持基于特定学科教材的检索增强生成。
    - [ ] **高级 Tool Calling**: 扩展 Agent 能力，支持调用计算器、维基百科搜索等外部工具。
    - [ ] **聊天记录导出**: 支持将辅导记录导出为 PDF 或长图格式。

### 2. 历史时间轴模块 (`:timeline_map`)
该模块通过地图与时间轴的可视化结合，动态展示历史事件的地理空间演变。
- **状态**: 🟡 迭代中 (基础框架与UI已完成，已接入主导航)
- **已实现功能**:
    - [x] **可视化界面**: `TimelineMapScreen` 实现底部时间轴滑块与顶部地图的联动展示，移除冗余返回按钮，优化导航体验。
    - [x] **地图集成**: 基于 `osmdroid` (OpenStreetMap) 实现地图背景展示与坐标标记 (`GeoPoint`)，支持深色模式下的地图反色显示。
    - [x] **语音交互**: 集成语音输入，支持通过语音描述自动生成或查询历史时间轴。
    - [x] **架构整合**: 与主应用的 `NavController` 完美对接，支持沉浸式体验与返回栈管理。
- **待开发功能**:
    - [ ] **真实数据接入**: 完善 `TimelineRepository`，通过 Qwen API 根据用户查询动态生成结构化的历史事件数据。
    - [ ] **富媒体内容**: 在地图标记点击时弹出的气泡中展示相关历史图片或维基百科摘要。
    - [ ] **离线地图支持**: 优化 `osmdroid` 缓存策略，支持预下载特定朝代/区域的离线地图包。

### 3. 视频总结模块 (`:video_summarizer`)
提供网络长视频或本地视频的内容智能摘要与知识点提取，帮助学生快速获取视频核心内容。
- **状态**: 🟢 开发中 (核心链路已打通并集成)
- **已实现功能**:
    - [x] **视频下载与解析**:
        - 集成 `youtubedl-android` 支持多种视频平台链接的解析与下载。
        - 提供 `VideoDownloadScreen` 展示下载进度与状态，移除冗余返回按钮。
    - [x] **本地视频处理**:
        - 支持通过系统文件选择器导入本地视频文件。
        - 使用 `FFmpegKit` 将视频文件高效提取为音频文件 (WAV格式)。
    - [x] **高性能离线转写 (ASR)**:
        - 集成新一代端到端语音识别框架 `Sherpa-onnx`。
        - 部署 `paraformer-zh` 模型，实现极低延迟、高准确率的全离线中文语音转文本。
    - [x] **AI 智能摘要**:
        - `BailianSummaryRepository` 将长文本转写结果发送至 Qwen 模型，生成结构化的视频内容摘要。
    - [x] **模块集成**: 已成功接入 `MainScreen` 的导航流程。
- **待开发功能**:
    - [ ] **知识点卡片化**: 将纯文本摘要进一步解析为结构化的思维导图或图文记忆卡片。
    - [ ] **转写时间戳对齐**: 结合播放器 (ExoPlayer)，实现点击摘要文字跳转到对应视频播放进度的功能。

### 4. 公共基础模块 (`:common`)
作为项目的核心基础设施层，为上层业务模块提供标准化的组件与工具，确保风格统一与代码复用。
- **状态**: 🟢 稳定
- **已实现功能**:
    - [x] **统一硬件管理器**:
        - `VoskVoiceManager` & `VoskModelManager`: 封装 Vosk 引擎，提供统一的离线语音唤醒与识别接口。
    - [x] **通用 UI 组件**:
        - `CameraScreen`: 基于 CameraX 的全屏自定义相机，支持前后摄切换，已完成深色模式适配。
        - `ImagePreviewScreen`: 图片预览与重拍/确认界面，已完成深色模式适配。
        - `GlobalApiSettingsDialog`: 统一的 API Key 和模型设置对话框，支持多模块共享配置。
    - [x] **网络与存储基础设施**:
        - `RetrofitClient` & `AuthInterceptor`: 统一的网络请求配置、日志拦截与 Token 管理。
        - `PreferencesManager`: 基于 Kotlin Flow 的协程安全键值对存储。
        - `RoomDatabaseBuilder`: 统一的数据库构建器与迁移管理。
- **待开发功能**:
    - [ ] **全局错误处理机制**: 建立统一的网络异常捕获与 UI (Snackbar/Dialog) 提示分发中心。

### 5. 几何解题模块 (`:geometry_solver`)
专注于平面/立体几何题目的智能识别与辅助求解。
- **状态**: ⚪ 待开发 (仅占位，已适配基础主题配置)
- **规划功能**:
    - 图形识别 (OCR + OpenCV)。
    - 几何画板与手绘修正。
    - 基于大模型的几何定理推导与分步解题展示。

### 6. 主应用壳工程 (`:app`)
负责应用的生命周期管理、各 Feature 模块的最终组装与打包配置。
- **状态**: 🟢 稳定
- **已实现功能**:
    - [x] **构建配置优化**: 解决多模块依赖冲突，统一编译 SDK 版本 (35) 与 Java 版本 (17)。
    - [x] **动态库打包**: 配置 `jniLibs` 打包规则，确保 FFmpeg、Vosk、Sherpa-onnx 等 C++ 动态链接库正确打包进 APK。

---

## 快速开始 (Getting Started)

### 环境要求
- Android Studio Koala | 2024.1.1 或更高版本 (推荐)
- JDK 17
- Android SDK 35 (Min SDK 24)

### 运行步骤
1. 克隆项目到本地。
2. 使用 Android Studio 打开项目根目录。
3. 等待 Gradle 同步完成 (可能需要下载较大的依赖包，如 ffmpeg-kit 和模型文件)。
4. 运行 `gradlew sherpaDownload` 任务或手动下载 Sherpa-onnx 语音模型：
   - 确保 `video_summarizer/src/main/assets/sherpa-onnx-paraformer-zh-2023-09-14` 目录下存在模型文件。
5. 选择 `app` 运行配置，连接物理设备或模拟器（强烈建议使用**物理设备**以测试相机、麦克风和高性能本地模型推理），点击 Run。

### API Key 配置
本项目依赖阿里云百炼大模型平台。首次运行应用后：
1. 点击主界面或相关模块右上角的设置图标，打开 **全局 API 设置** (`GlobalApiSettingsDialog`)。
2. 填入您的阿里云百炼 API Key。
3. (可选) 修改基础 URL 和切换所需的 Qwen 模型。

### 构建清理
如果您在构建过程中遇到缓存冲突或构建产物锁定问题（例如 `Unable to delete directory`），可以使用项目根目录提供的深度清理脚本：
- **Windows**: 双击运行 `clean_project.bat` 或在终端执行 `.\clean_project.bat`。该脚本会停止后台 Gradle/Kotlin 守护进程，并彻底清除所有 `.gradle`、`build` 和 `.cxx` 缓存目录。

---

## 许可证 (License)
本项目仅供软件创新大赛学习与交流使用。引用的第三方开源库（如 Vosk, Sherpa-onnx, FFmpeg 等）遵循其各自的开源协议。