# 软件创新大赛项目 - AI Education (智能教育助手)

## 项目概览 (Project Overview)
本项目是一个集成了智能辅导、历史时间轴可视化、全能解题和智能总结功能的多模块 Android 教育应用。项目采用现代 Android 开发技术栈，注重模块化设计与代码复用，旨在通过人工智能技术提升学生的学习效率和体验。

## 核心技术栈 (Tech Stack)
- **开发语言**: Kotlin 1.9+
- **UI 框架**: Jetpack Compose (Material3)
- **架构模式**: MVVM, Clean Architecture (Data/Domain/Presentation 隔离)
- **异步与响应式编程**: Kotlin Coroutines, StateFlow / SharedFlow, Hilt (依赖注入)
- **网络通信**: Retrofit, OkHttp
- **本地存储**: Room (SQLite), DataStore (替代 SharedPreferences)
- **多媒体与设备**: CameraX (图像采集), ExoPlayer / Media3 (视频播放)
- **语音识别与处理**: Vosk-Small-CN (离线轻量级语音识别), Sherpa-onnx (高性能离线语音转写)
- **视频处理**: FFmpegKit (音视频提取), YoutubeDL-Android (视频解析下载)
- **AI 大模型接入**: 兼容 OpenAI 格式接口，支持阿里云百炼平台 (Qwen-Turbo 文本对话, Qwen-VL 多模态视觉)
- **构建系统**: Gradle (Kotlin DSL), Version Catalog 依赖管理

---

## 项目架构与模块说明 (Project Architecture)

本项目采用多模块 (Multi-Module) 架构，按“AI辅导、解题、总结、复习”四大核心功能特性进行垂直拆分，并抽取公共基础设施，以实现高内聚、低耦合。

```text
AI_Education/
├── app/                  # 主应用壳工程 (负责应用入口、全局配置、底部导航和最终打包)
├── common/               # 公共基础模块 (UI组件、统一LLM网络架构、Hilt注入、工具类)
├── ai_tutor/             # AI辅导模块 (核心对话、多模态输入、Agent决策，内置时间轴地图)
├── solver/               # 解题模块 (几何/通用智能解题)
├── summarizer/           # 总结模块 (视频总结、文本总结、音频提取、离线ASR转写)
└── review/               # 复习模块 (复习专属页面与相关功能)
```

---

## 模块详细开发进度 (Detailed Development Progress)

### 1. AI辅导模块 (`:ai_tutor`)
该模块是应用的核心交互入口，提供基于 AI 的多模态（文本、语音、图像）辅导服务，并内置了历史时间轴地图功能。
- **状态**: 🟢 开发中 (主要功能已完成并集成)
- **已实现功能**:
    - [x] **多模态对话界面**: 基于 Compose 实现的流畅聊天界面 (`ChatScreen`)，支持深色模式。
    - [x] **大模型对接**: 基于 `common` 模块统一的 LLM 架构，对接文本与视觉大模型。
    - [x] **Agent 意图识别**: 初步支持区分通用聊天与特定指令。
    - [x] **图像处理与识别**: 集成自定义相机，支持拍照预览与 Base64 编码上传。
    - [x] **内置时间轴地图**: 
        - 当用户输入类似“生成XXX的时间轴地图”时，自动触发并跳转到纯净版时间轴地图界面 (`TimelineMapScreen`)。
        - 基于 `osmdroid` 实现地图背景展示与事件坐标标记。
- **待开发功能**:
    - [ ] **知识库增强 (RAG)**: 引入本地或云端向量数据库，支持基于特定学科教材的检索增强生成。
    - [ ] **高级 Tool Calling**: 扩展 Agent 能力，支持调用计算器、维基百科搜索等外部工具。

### 2. 解题模块 (`:solver`)
专注于平面/立体几何等题目的智能识别与辅助求解。
- **状态**: ⚪ 待开发 (基础框架搭建完成)
- **规划功能**:
    - 图形识别 (OCR + OpenCV)。
    - 几何画板与手绘修正。
    - 基于大模型的几何定理推导与分步解题展示。

### 3. 总结模块 (`:summarizer`)
提供网络长视频、本地视频或文本的内容智能摘要与知识点提取。
- **状态**: 🟢 开发中 (核心链路已打通并集成)
- **已实现功能**:
    - [x] **视频下载与解析**: 集成 `youtubedl-android` 支持多种视频平台链接解析。
    - [x] **本地视频处理**: 支持导入本地视频并使用 `FFmpegKit` 提取音频。
    - [x] **高性能离线转写 (ASR)**: 集成 `Sherpa-onnx` 与 `paraformer-zh` 模型，实现极低延迟的全离线中文语音转文本。
    - [x] **AI 智能摘要**: 基于转写结果调用大模型生成结构化视频内容摘要。
- **待开发功能**:
    - [ ] **知识点卡片化**: 将纯文本摘要进一步解析为结构化的思维导图或图文记忆卡片。
    - [ ] **转写时间戳对齐**: 结合播放器实现点击摘要跳转到对应视频进度。
    - [ ] **文本总结**: 支持直接输入长文本或文档进行智能总结。

### 4. 复习模块 (`:review`)
为用户提供复习功能与资料整合的独立空间。
- **状态**: ⚪ 待开发 (基础框架与空页面搭建完成)
- **规划功能**:
    - 错题本整理与回顾。
    - 基于遗忘曲线的复习提醒。

### 5. 公共基础模块 (`:common`)
作为项目的核心基础设施层，提供标准化的组件与工具，近期已完成深度重构。
- **状态**: 🟢 稳定
- **已实现功能**:
    - [x] **统一大模型 (LLM) 架构**: 将各模块重复的网络请求与数据模型下沉，提供统一的 OpenAI 兼容接口 (`OpenAiService`, `OpenAiModels`)。
    - [x] **统一协程调度**: 引入基于 Hilt 的 `DispatcherProvider`，提升模块化与可测试性。
    - [x] **安全配置与偏好管理**: 
        - API Key 安全存储 (`local.properties` + `BuildConfig`) 与 `GlobalConfigRepository` 降级读取机制。
        - 魔法字符串集中管理 (`AppConstants`)。
    - [x] **统一硬件与UI组件**: `VoskVoiceManager`、`CameraScreen`、`GlobalApiSettingsDialog` 等。

### 6. 主应用壳工程 (`:app`)
负责应用的生命周期管理、底部四大模块导航 (`MainScreen`) 的组装。
- **状态**: 🟢 稳定
- **已实现功能**:
    - [x] **全局导航**: 完善了 `AI辅导`、`解题`、`总结`、`复习` 四大主模块的底部导航逻辑。
    - [x] **双图标修复**: 解决了应用安装后出现两个启动图标的问题，并配置了全新的教育AI主题应用图标。

---

## 快速开始 (Getting Started)

### 环境要求
- Android Studio Koala | 2024.1.1 或更高版本 (推荐)
- JDK 17
- Android SDK 35 (Min SDK 24)

### 运行步骤
1. 克隆项目到本地。
2. 在项目根目录创建 `local.properties` 文件（如果不存在），并添加您的默认 API Key（可选）：
   ```properties
   DEFAULT_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
   ```
3. 使用 Android Studio 打开项目根目录，等待 Gradle 同步完成。
4. 运行 `gradlew sherpaDownload` 任务或手动下载 Sherpa-onnx 语音模型：
   - 确保 `summarizer/src/main/assets/sherpa-onnx-paraformer-zh-2023-09-14` 目录下存在模型文件。
5. 选择 `app` 运行配置，连接物理设备，点击 Run。

### API Key 配置
1. 点击主界面或相关模块右上角的设置图标，打开 **全局 API 设置**。
2. 填入您的 API Key（如未在 `local.properties` 中配置）。
3. (可选) 修改基础 URL 和切换所需的模型名称。

### 构建清理
如果您在构建过程中遇到缓存冲突，可双击运行根目录的 `clean_project.bat` 深度清理脚本。

---

## 许可证 (License)
本项目仅供软件创新大赛学习与交流使用。引用的第三方开源库（如 Vosk, Sherpa-onnx, FFmpeg 等）遵循其各自的开源协议。
