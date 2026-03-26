# AI Education (智学助手)

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green.svg" alt="Android">
  <img src="https://img.shields.io/badge/Language-Kotlin-blue.svg" alt="Kotlin">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-blueviolet.svg" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/Architecture-Clean%20Architecture%20%7C%20MVI-orange.svg" alt="Clean Architecture">
  <img src="https://img.shields.io/badge/LLM-Multimodal-yellow.svg" alt="LLM">
</p>

[English](README.md) | [简体中文](README_ZH.md)

一款基于大语言模型 (LLM) 与多模态技术的全能型智能教育 Android 应用。本项目采用现代化 Android 开发技术栈（Jetpack Compose, Hilt, Room, MVI 架构），通过多模块化实现了高内聚、低耦合的业务分离，提供从**知识讲解**、**智能解题**、**多端总结**到**复习巩固**的完整学习闭环。

---

## 🌟 核心功能模块

本项目划分为四大核心业务模块，深度覆盖全流程学习场景：

### 1. 🤖 AI 辅导 (AI Tutor)
* **多模态对话**: 支持文字、图片、语音的智能问答，具备完整的上下文记忆与历史对话流管理。
* **历史时间轴地图 (Timeline Map)**: 结合 OSMDroid 地图引擎与大语言模型，将抽象的历史事件具象化为全球地图上的交互式节点。支持双指缩放、全景世界地图预加载、动态标记与详情联动，帮助梳理历史事件与地理空间脉络。
* **个人中心与配置**: 全局 LLM 模型参数（API Key、Base URL、模型选择）统一管理与无缝切换。

### 2. 📝 智能解题 (Solver)
* **多模式输入**: 支持拍照解题（集成 CameraX）、相册上传以及历史记录极速回溯。
* **智能分类与解析**: 自动识别题目所属学科（几何、代数、物理、化学、生物等），并加载针对性的专属系统 Prompt。
* **动态图元渲染 (Geometry Canvas)**: 深度集成 `exp4j` 数学引擎，可根据 AI 输出的 JSON 指令动态绘制函数图像（包含切线、阴影区域）、几何图形、物理受力分析图及化学实验示意图。
* **错题本联动**: 解题成功后支持一键加入错题本，并在解题历史中进行状态同步。

### 3. 📑 智能总结 (Summarizer)
* **多格式文本总结**: 支持直接粘贴长文本，或导入并解析 PDF (PDFBox)、Word (XML解析)、HTML (Jsoup)、TXT/CSV 等多格式文件，提取核心摘要。
* **音视频总结**: 基于 Sherpa ONNX 实现高效的离线语音识别 (ASR) 转写，并结合大模型进行提炼总结。
* **对话与知识卡片**: 支持导入 Room 数据库中的历史对话进行复盘总结，并将高价值内容一键转化为知识卡片（支持本地 CRUD 管理）。

### 4. 📚 智能复习 (Review)
* **艾宾浩斯复习计划**: 支持自定义学科的复习排期，结合记忆曲线算法智能规划每日复习任务。
* **错题本与知识巩固**: 提供错题检索、分类浏览以及强化测验功能，形成完整的学习反馈回路。

---

## 🛠️ 技术栈与架构设计

### 架构规范
* **架构模式**: Clean Architecture + MVVM + 单向数据流 (MVI 思想的 StateFlow 管理)。
* **多模块化 (Multi-module)**: 以业务为边界拆分主模块 (`app`, `common`, `ai_tutor`, `solver`, `summarizer`, `review`)，模块内严格按 `models`, `services`, `presentation` 进行组件分层。
* **高可用性与离线降级**: 内置 `NetworkMonitor` 实时监听网络状态，提供全局的离线降级策略（无网环境下无缝切换至本地错题本、历史记录、离线语音转写及本地图元渲染）。

### 核心技术框架
* **UI 层**: Jetpack Compose (全面应用了 Material 3 规范、深色模式适配与系统级无障碍访问支持 Accessibility)。
* **依赖注入**: Dagger Hilt，包括自定义 Coroutine DispatcherProvider 增强可测试性。
* **本地持久化**: Room Database (包含关联表、TypeConverters) 与 DataStore/SharedPreferences。
* **网络与通信**: Retrofit, OkHttp, 统一的基于 SSE (Server-Sent Events) 的流式输出大模型接口。底层网络客户端支持针对长文本生成场景的自定义超时配置（如时间轴地图生成支持 90s 长时连接）。
* **安全存储**: 基于 Android NDK (C++) 的 API Key 安全存储机制，支持 XOR 异或加密与 BuildConfig 优雅降级。
* **地图引擎**: 集成 OSMDroid 开源地图方案，替换原生方案以提供更高的定制化能力，并配置国内高德地图瓦片镜像源（style=7 标准街道底图）以保障加载速度与稳定性。
* **多媒体与底层处理**: CameraX (拍照), Sherpa ONNX (离线语音转文字), Coil (图片加载与裁剪), PDFBox-Android (PDF解析)。

---

## 📁 目录结构

```text
AI_Education/
├── app/                  # 主应用入口，负责依赖注入、全局配置和主导航
├── common/               # 公共基础模块（网络、数据库、基础UI组件、工具类）
├── ai_tutor/             # AI辅导核心主模块
│   ├── multimodal_chat/  # 多模态对话子模块
│   └── timeline_map/     # 历史时间轴地图子模块
├── solver/               # 智能解题主模块
│   ├── geometry_solver/  # 几何解题子模块 (动态渲染引擎)
│   ├── algebra_solver/   # 代数解题子模块
│   └── comprehensive/    # 综合解题子模块 (拍照解题主流程)
├── summarizer/           # 智能总结主模块
│   ├── video_summarizer/ # 视频总结子模块
│   ├── text_summarizer/  # 文本总结子模块 (多格式文件解析)
│   ├── audio_summarizer/ # 音频/语音总结子模块 (离线 ASR)
│   ├── dialogue_summarizer/ # 对话历史总结子模块
│   └── knowledge_cards/  # 知识卡片管理子模块
└── review/               # 智能复习主模块
    ├── planner/          # 复习计划子模块 (艾宾浩斯算法)
    ├── reinforcement/    # 知识巩固子模块
    └── error_book/       # 错题本子模块
```

---

## 🚀 编译与运行指南

### 1. 环境准备
* **Android Studio**: 推荐使用 Ladybug 或更高版本。
* **JDK**: 17 及以上。
* **Android SDK**: 需配置好本地 Android SDK 环境。

### 2. 配置 API Key
项目依赖大模型 API（兼容 OpenAI 格式，默认适配 Qwen 等模型）。为保障安全，API Key **不应**硬编码在代码中。
请在项目根目录下的 `local.properties` 文件中添加您的配置（若无此文件请新建）：

```properties
API_KEY=your_actual_api_key_here
BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1/
MODEL_NAME=qwen-vl-plus
```
*(注：应用内也提供了全局设置弹窗，支持在运行时动态修改这些配置)*

### 3. 构建与清理
* 直接使用 Android Studio 点击 `Run` 运行项目。
* 如果遇到 Gradle 缓存或编译死锁问题，可以运行根目录下的清理脚本：
  * **Windows**: 双击运行 `clean_project.bat`，脚本会深度清理 `.gradle`, `.idea`, `build` 目录及各类缓存并结束相关后台进程。

---

## 🧪 测试与质量保证

* **单元测试**: 覆盖了所有核心业务 ViewModel、数据转换层和 Room 数据库操作，采用 Mockito、Turbine 以及 UnconfinedTestDispatcher 保障协程测试的稳定性与状态流 (StateFlow) 的正确性。
* **代码规范**: 采用 `ktlint` 约束，确保了极高的代码质量。可通过 `./gradlew ktlintFormat` 自动格式化。