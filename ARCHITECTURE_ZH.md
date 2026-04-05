# 项目代码重构与架构设计文档

[English](ARCHITECTURE.md) | [简体中文](ARCHITECTURE_ZH.md)

本文档定义了 东大智搭 (Dongda Zhida) 项目的整体架构模式、目录结构规范、组件层级设计以及多模块化拆分标准。

## 1. 架构模式

本项目全面采用 **Clean Architecture** 结合 **MVVM** 设计模式，并基于 **MVI** 的思想通过 StateFlow 实现单向数据流 (Unidirectional Data Flow)。

- **UI 层 (Presentation)**: 使用 Jetpack Compose 构建声明式 UI。页面 (Screen) 仅负责状态的观测和事件的分发，不包含任何业务逻辑。
- **视图模型层 (ViewModel)**: 接收 UI 层的意图 (Intent/Event)，通过调用 Repository 执行业务逻辑，并最终暴露出一个不可变的统一状态流 (`UiState`) 供 UI 消费。
- **领域与数据层 (Services/Models)**: 
  - Repository 模式：对外屏蔽数据来源（本地 Room 数据库或远程网络 API）。
  - 数据模型分离：严格区分网络 DTO、数据库 Entity 与 UI 层所需的 State Models。

## 2. 目录结构设计要求

项目采用多模块化 (Multi-module) 进行组织，按业务功能进行高内聚、低耦合的模块划分。

### 顶层目录结构
```text
AI_Education/
├── app/                  # 主应用入口，负责 Hilt 依赖注入配置、全局配置和主导航路由
├── common/               # 公共基础模块（网络层、Room数据库、基础UI组件、全局工具类、底层Manager）
├── ai_tutor/             # AI辅导核心主模块
│   ├── multimodal_chat/  # 多模态对话子模块
│   └── timeline_map/     # 历史时间轴子模块
├── solver/               # 智能解题主模块
│   ├── geometry_solver/  # 几何解题子模块 (含动态图元渲染引擎)
│   ├── algebra_solver/   # 代数解题子模块
│   └── comprehensive/    # 综合解题子模块 (包含拍照解题、自动分类与错题本联动)
├── summarizer/           # 智能总结主模块
│   ├── video_summarizer/ # 视频总结子模块
│   ├── text_summarizer/  # 文本总结子模块 (多格式解析与 PDF 导出)
│   ├── audio_summarizer/ # 音频/语音总结子模块 (离线转写)
│   └── dialogue_summarizer/ # 对话历史总结子模块
└── review/               # 智能复习主模块
    ├── planner/          # 复习计划子模块 (艾宾浩斯记忆曲线)
    ├── reinforcement/    # 知识巩固子模块
    └── error_book/       # 错题本子模块
```

### 子模块内部标准结构（以 `video_summarizer` 为例）
为了保持代码组织的高度一致性，每一个子模块内部都必须严格遵循以下包结构：
```text
video_summarizer/
├── models/               # 数据模型定义
│   ├── entities/         # Room 数据库实体类
│   ├── dtos/             # 网络请求响应数据传输对象
│   └── states/           # UI 状态类 (UiState)
├── services/             # 业务服务与数据来源
│   ├── api/              # Retrofit 接口定义
│   ├── repository/       # 数据仓库实现类
│   └── usecases/         # 复杂业务逻辑的用例 (可选)
├── utils/                # 专属工具类 (Formatters, Helpers)
└── presentation/         # 表现层
    ├── components/       # 专属业务组件 (如 SummaryOptionCard)
    ├── screens/          # 页面级组件 (如 VideoDownloadScreen)
    └── viewmodels/       # 页面对应的 ViewModel
```

## 3. 模块划分与依赖标准

- **高内聚、低耦合**：每个主模块（如 `summarizer`）独立负责一个大的业务领域。
- **职责单一**：主模块下的子模块（如 `video_summarizer`）只关注具体维度的功能。
- **依赖关系规则**：
  - `app` 模块是唯一可以依赖所有其他业务模块的宿主模块。
  - **横向隔离 (FeatureApi 路由机制)**：平级的业务模块 (`ai_tutor`, `solver`, `summarizer`, `review`) 之间**严禁互相依赖**。各业务模块需实现定义在 `common` 模块中的 `FeatureApi` 接口（如 `AiTutorFeatureApi`, `SolverFeatureApi`），由 Hilt 在 `app` 模块统一注入，最后在 `MainScreen` 中调用 `registerGraph` 注册路由。这实现了跨模块通信和页面跳转的彻底解耦。
  - **底层沉淀**：所有的业务模块均单向依赖 `common` 模块以获取基础能力（如统一的 LLM 网络层、DispatcherProvider、全局组件与工具类）。

## 4. 组件层级规范与 Compose 最佳实践

Compose UI 组件按复用范围和职责划分为三个层级：
1. **基础组件 (Base Components)**：位于 `common/presentation/components`。提供与业务无关的基础 UI（如升级版的 `SafeMarkdownText`、标准化的 `ApiKeyDialog`、全局加载指示器）。
2. **业务组件 (Business Components)**：位于各子模块的 `presentation/components`。具有特定的业务逻辑但在模块内部多页面复用。
3. **页面组件 (Page Components)**：位于各子模块的 `presentation/screens`。负责组合业务组件并唯一负责与 ViewModel 交互。

### Compose 架构与最佳实践
1. **统一顶栏规范**：必须使用 `CenterAlignedTopAppBar`（配合 `TopAppBarDefaults.centerAlignedTopAppBarColors`），通过 `title = { Text(...) }` 设置居中标题，通过 `navigationIcon` 提供标准返回按钮，与系统返回键行为对齐并解耦。
2. **生命周期感知的状态收集**：禁止直接使用 `.collectAsState()`，所有 UI 层对 ViewModel `StateFlow` 的观测必须通过 `lifecycle-runtime-compose` 包下的 `.collectAsStateWithLifecycle()` 进行收集，以避免应用在后台时仍然消耗资源。
3. **单次 UI 事件分发 (UiEvent)**：对于 Snackbar 提示、Toast、页面跳转等非持续性事件，禁止放在 `StateFlow` 中引发重复触发，必须使用 `Channel<UiEvent>` 发送，由 UI 层的 `LaunchedEffect` 进行收集与消费。
4. **动效规范**：各模块必须通过 `AnimatedVisibility` (如 `fadeIn+expandVertically` / `fadeOut+shrinkVertically`) 以及 NavHost 页面过渡 (slide/fade) 补充核心交互路径的动效反馈。
5. **高度测量适配**：在 `Column` 应用 `verticalScroll` 时，内部的 `MarkdownText` 可能会因为获得无限大高度约束而导致不可见或截断。应将 `verticalScroll` 直接应用在 `MarkdownText` 的 `modifier` 上，并配合父容器的 `Box` 或 `weight(1f)` 以确保高度测量正确。

## 5. 状态流与异常处理规范

1. **StateFlow 替代 LiveData/MutableState**：所有的 ViewModel 必须使用 `MutableStateFlow` 暴露单一的 UI 状态。
   ```kotlin
   private val _uiState = MutableStateFlow(MyUiState())
   val uiState: StateFlow<MyUiState> = _uiState.asStateFlow()
   ```
2. **安全协程调度**：必须通过注入的 `DispatcherProvider` 获取协程上下文，严禁硬编码 `Dispatchers.IO` 或 `Dispatchers.Main`，以保障 ViewModel 单元测试的完全可控。
3. **统一异常捕获**：网络请求、文件解析、模型推理等易错操作，必须在 ViewModel 层进行 `try-catch` 捕获，并将错误信息转化为 `uiState.error`，交由 UI 层的统一 ErrorCard 组件渲染。

## 6. 测试规范

- **单元测试**：
  - 核心逻辑（Repository 业务聚合、ViewModel 状态扭转、工具类解析）必须覆盖单元测试。
  - `common` 模块基础建设的代码覆盖率要求在 90% 以上。
  - 必须统一采用 `UnconfinedTestDispatcher` 解决协程挂起不一致问题，并在测试 Mockito 匹配时注意使用 `anyOrNull()` 而不是 `any()` 来适配 Kotlin 的可空类型。
- **UI 测试 / Compose 测试**：针对关键用户路径（如输入框防抖、导航跳转）可选择性添加。

## 7. 安全与离线策略规范

1. **API Key 安全存储**：
   - 严禁在代码中硬编码明文 API Key。
   - 采用 Android NDK (C/C++) 层存储并使用 XOR 等简单加密方式混淆 Key，通过 JNI 接口 (`NativeLib`) 提供给上层。由于跨平台配置繁琐，现阶段在 `local.properties` 配置并通过 `BuildConfig` 和 `GlobalConfigRepository` 获取，并作为优雅降级的安全回退方案。
2. **全局离线降级策略**：
   - 必须通过全局单例的 `NetworkMonitor` (基于 `ConnectivityManager.NetworkCallback` + `StateFlow`) 实时监听网络连接状态。
   - 所有涉及云端大模型推断或网络下载的交互，必须在 ViewModel 意图处理的起始位置判断网络状态，并在无网时阻断请求，优雅降级为本地数据读取或给出清晰的无网提示。 

## 8. 多模态与底层 JNI 内存规范

在涉及大图片处理（如拍照解题）、离线模型推理（如 Sherpa-ONNX 语音转写）时，必须严格管理 Native 与 JVM 内存：
1. **Bitmap 生命周期与内存回收**：经过裁剪或旋转处理后生成的临时 Bitmap（如 `AiTutorViewModel`、`SolverViewModel` 中生成的 base64 预览图），在转换或上传完成后，必须主动调用 `bitmap.recycle()` 回收 Native 内存，避免多模态输入时发生 OOM。
2. **音视频流式解析防止崩溃**：
   - 使用 C++ 的 `WaveReader` 解析大音频文件时，**严禁一次性将整个文件传递给底层**，否则会导致 `Channel unrecoverably broken` 或 `SIGABRT`。
   - 必须在 Kotlin 层通过 `RandomAccessFile` 分块解析 WAV (跳过 44 字节头部处理 PCM)，每次仅分配小块 (如 30秒/3MB 的 FloatArray) 并循环交由 `acceptWaveform` 进行流式解码。
3. **多线程并发限制**：在部分 JNI 推理操作（如 Sherpa-ONNX 的部分离线模型）中，必须通过配置限制 `numThreads=1` 防止多线程竞争引发的底层崩溃。

## 9. 全局与配置规范

1. **国际化 (i18n)**：项目中**严禁出现硬编码的中文字符串**。所有向用户展示的文本必须提取至 `strings.xml` 中，并通过 `stringResource(R.string.xxx)` (Compose) 或 `Context.getString()` 进行引用，并在 `locales_config.xml` 中配置支持语言 (如默认 `values` 与 `values-en`)，实现应用级动态语言切换。
2. **JSON 解析器**：统一使用 `Gson` 作为序列化与反序列化工具，禁用 `JSONObject` 以确保对象映射的类型安全与代码可读性。
3. **镜像源与网络下载**：为提升国内环境下的下载稳定性，依赖下载（如 `ModelDownloader.kt`，`sherpa_setup.gradle`）均已全面替换为国内镜像（如 `kkgithub.com`, `ghproxy`, `hf-mirror.com`），新增模型依赖时需遵循此规范。 
