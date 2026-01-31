# 软件创新大赛项目

## 项目概览 (Project Overview)
本项目是一个集成了智能辅导、历史可视化、几何解题和视频总结功能的多模块 Android 教育应用。项目采用现代 Android 开发技术栈，注重模块化设计与代码复用。

## 核心技术栈 (Tech Stack)
- **语言**: Kotlin
- **UI 框架**: Jetpack Compose (Material3)
- **架构模式**: MVVM, Clean Architecture (Data/Domain/Presentation)
- **异步处理**: Coroutines, Flow
- **网络**: Retrofit, OkHttp
- **数据库**: Room (SQLite)
- **多媒体**: CameraX (图像), Vosk/Sherpa-onnx (离线语音识别), FFmpegKit (音视频处理), YoutubeDL (视频下载)
- **AI 模型**: Qwen-Turbo/VL (阿里云百炼), Vosk-Small-CN/Sherpa-onnx (本地语音)
- **构建工具**: Gradle (Kotlin DSL), Version Catalog

---

## 模块详细开发进度 (Detailed Development Progress)

### 1. 智能辅导模块 (ai_tutor)
该模块是应用的核心交互入口，提供基于 AI 的多模态辅导服务。
- **状态**: 🟢 开发中 (主要功能已完成)
- **已实现功能**:
    - [x] **多模态对话**:
        - 集成 `QwenRepository` 对接 Qwen-Turbo (文本) 和 Qwen-VL (视觉) 模型。
        - 实现 `AgentDecisionHub` 进行意图识别，区分通用聊天与特定学科问题。
    - [x] **本地数据持久化**:
        - 使用 Room 数据库 (`ChatDao`, `UserDao`) 保存聊天记录 (`ChatSessionEntity`, `MessageEntity`) 和用户信息。
    - [x] **语音交互 (离线)**:
        - 通过 `common` 模块的 `VoskVoiceManager` 实现全离线中文语音识别，保护隐私并降低延迟。
        - 实时语音输入状态反馈 (Listening/Processing)。
    - [x] **图像处理**:
        - 集成自定义相机 (`CameraScreen`)，支持拍照、裁剪、预览。
        - 图片自动压缩与 Base64 编码上传。
    - [x] **用户系统**:
        - 基础的 `AuthViewModel` 和 `ProfileScreen` 框架。
- **待开发功能**:
    - [ ] **知识库增强 (RAG)**: 引入向量数据库，支持基于特定学科教材的检索增强生成。
    - [ ] **高级 Agent 能力**: 扩展 Tool Calling 能力，支持计算器、搜索等工具调用。
    - [ ] **历史记录导出**: 支持将辅导记录导出为 PDF 或 Markdown 格式。

### 2. 历史时间轴模块 (timeline_map)
该模块通过地图与时间轴的可视化结合，展示历史事件的地理空间演变。
- **状态**: 🟡 迭代中
- **已实现功能**:
    - [x] **可视化界面**:
        - `TimelineMapScreen` 实现地图背景与时间轴组件的联动展示。
    - [x] **数据逻辑**:
        - `TimelineRepository` 负责从 JSON/API 获取历史事件数据。
        - 定义了 `TimelineModels` 数据结构，支持事件的地理坐标与时间属性。
    - [x] **架构整合**:
        - 解决了与 `common` 模块的依赖冲突，统一使用项目级依赖版本。
- **待开发功能**:
    - [ ] **深度地图集成**: 接入高德/Google Maps SDK，实现自定义标记、覆盖层和路径绘制。
    - [ ] **语音交互**: 实现语音提问自动生成时间轴地图。
    - [ ] **富媒体内容**: 支持在时间轴事件中展示图片、视频等多媒体资料。
    - [ ] **筛选与搜索**: 实现按朝代、地区或关键词筛选历史事件的功能。
    - [ ] **离线地图包**: 支持地图数据的离线缓存，优化无网体验。

### 3. 公共基础模块 (common)
作为项目的核心基础设施层，为上层业务模块提供标准化的组件与工具，确保风格统一与代码复用。
- **状态**: 🟢 稳定
- **已实现功能**:
    - [x] **统一管理器 (Managers)**:
        - `VoskVoiceManager`: 封装 Vosk 引擎，提供 `startListening`, `stopListening` 等统一接口，解耦具体实现。
        - `VoskModelManager`: 负责离线模型的加载与状态检查。
    - [x] **通用 UI 组件 (Components)**:
        - `CameraScreen`: 基于 CameraX 的全屏自定义相机，支持前后摄切换与闪光灯。
        - `ChatInputArea`: 高度复用的聊天输入栏，集成文本框、语音按钮与更多功能面板。
        - `ImagePreviewScreen`: 图片预览与确认界面。
    - [x] **网络与存储 (Infrastructure)**:
        - `RetrofitClient` & `AuthInterceptor`: 统一的网络请求配置与鉴权拦截。
        - `RoomDatabaseBuilder`: 数据库构建器封装。
        - `PreferencesManager`: 基于 DataStore/SharedPreferences 的轻量级配置管理。
    - [x] **工具类 (Utils)**:
        - 加密 (`EncryptionUtils`)、日期格式化 (`DateFormatUtils`) 等通用工具。
- **待开发功能**:
    - [ ] **全局错误处理**: 建立统一的异常捕获与 Toast/Snackbar 提示机制。
    - [ ] **主题切换**: 完善深色/浅色模式的全局状态管理与持久化。
    - [ ] **统一 TTS 管理器**: 封装 TextToSpeechManager，供各模块复用。

### 4. 几何解题模块 (geometry_solver)
专注于平面/立体几何题目的智能识别与求解。
- **状态**: ⚪ 初始化 (Skeleton)
- **已实现功能**:
    - [x] **基础框架**: 包含基础的 `MainActivity` 与 UI 主题配置。
- **待开发功能**:
    - [ ] **图像采集组件**: 集成 CameraX 实现高质量图像捕获。
    - [ ] **图形识别 (OCR)**: 集成 OpenCV 或 MLKit，识别手绘或打印的几何图形与标注。
    - [ ] **几何求解器**: 实现或接入几何定理证明引擎，支持步骤推导。
    - [ ] **绘图画板**: 提供 Canvas 画板，允许用户手绘图形并进行即时修正。
    - [ ] **解题步骤展示**: 设计交互式 UI，分步展示解题思路与辅助线添加过程。

### 5. 视频总结模块 (video_summarizer)
提供长视频内容的智能摘要与知识点提取。
- **状态**: 🟢 开发中
- **已实现功能**:
    - [x] **视频下载**:
        - `VideoDownloader`: 集成 `youtubedl-android` 支持视频下载与音频提取。
        - `VideoDownloadScreen`: 提供视频链接输入与下载进度展示。
    - [x] **语音转写 (ASR)**:
        - `SherpaAsrManager`: 集成 Sherpa-onnx 实现高性能离线中文语音转写。
    - [x] **AI 摘要**:
        - `BailianSummaryRepository`: 对接阿里云百炼 API 生成视频内容摘要。
    - [x] **业务逻辑**:
        - `VideoDownloadViewModel`: 管理下载、转写、摘要生成的完整流程状态。
- **待开发功能**:
    - [ ] **视频选择与上传**: 实现系统文件选择器，支持从相册导入视频。
    - [ ] **知识点卡片**: 将摘要转化为美观的图文卡片。
    - [ ] **导出功能**: 支持将摘要导出为 PDF 或 Markdown 格式。
    - [ ] **音频提取优化**: 进一步优化 FFmpeg 集成，提升音频提取速度。

### 6. 主应用 (app)
负责应用的生命周期管理、模块路由与最终打包。
- **状态**: 🟢 稳定
- **已实现功能**:
    - [x] **构建系统**:
        - 修复了多模块间的 Gradle 依赖冲突与资源合并问题 (Resource Merging)。
        - 解决了 Windows 环境下的文件锁 (File Lock) 问题，确保构建流程顺畅。
    - [x] **模块路由**:
        - 实现了各子模块 (Feature Modules) 的聚合与导航分发。
- **待开发功能**:
    - [ ] **性能优化**: 优化冷启动时间，实施按需加载。
    - [ ] **Release 构建**: 配置签名文件与 ProGuard/R8 混淆规则。
    - [ ] **Deep Link**: 实现跨模块的深度链接跳转机制。
