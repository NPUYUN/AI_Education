# 项目代码重构与架构设计文档

本文档定义了 AI Education (智学助手) 项目的整体架构模式、目录结构规范、组件层级设计以及多模块化拆分标准。

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
│   ├── text_summarizer/  # 文本总结子模块 (多格式解析)
│   ├── audio_summarizer/ # 音频/语音总结子模块 (离线转写)
│   ├── dialogue_summarizer/ # 对话历史总结子模块
│   └── knowledge_cards/  # 知识卡片管理子模块
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
  - **横向隔离**：平级的业务模块 (`ai_tutor`, `solver`, `summarizer`, `review`) 之间**严禁互相依赖**，必须通过 `app` 模块中的全局路由传递参数解耦。
  - **底层沉淀**：所有的业务模块均单向依赖 `common` 模块以获取基础能力（如统一的 LLM 网络层、DispatcherProvider、Vosk/Sherpa 管理器）。

## 4. 组件层级规范

Compose UI 组件按复用范围和职责划分为三个层级：
1. **基础组件 (Base Components)**：位于 `common/presentation/components`。提供与业务无关的基础 UI（如全局使用的 `TopBar`、标准化的错误卡片、加载动画指示器）。
2. **业务组件 (Business Components)**：位于各子模块的 `presentation/components`。例如 `summarizer` 模块中的文件选择卡片，具有特定的业务逻辑但可在该模块内部多页面复用。
3. **页面组件 (Page Components)**：位于各子模块的 `presentation/screens`。负责组合业务组件、拦截系统级事件（如返回键），并唯一负责与 ViewModel 的交互。

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
- **UI 测试 / Compose 测试**：针对关键用户路径（如输入框防抖、导航跳转）可选择性添加。 
