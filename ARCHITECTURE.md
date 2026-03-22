# 项目代码重构与架构设计文档

## 1. 目录结构设计要求

项目采用多级树形层级结构进行组织，按业务功能进行高内聚、低耦合的模块划分。

### 顶层目录结构
```text
AI_Education/
├── app/                  # 主应用入口，负责依赖注入、全局配置和主导航
├── common/               # 公共基础模块（网络、数据库、基础UI组件、工具类）
├── ai_tutor/             # AI辅导核心主模块
│   ├── multimodal_chat/  # 多模态对话子模块
│   ├── timeline_map/     # 历史时间轴子模块
│   └── learning_record/  # 学习记录子模块
├── solver/               # 智能解题主模块
│   ├── geometry_solver/  # 几何解题子模块
│   ├── algebra_solver/   # 代数解题子模块
│   └── comprehensive/    # 综合解题子模块
├── summarizer/           # 智能总结主模块
│   ├── video_summarizer/ # 视频总结子模块
│   ├── text_summarizer/  # 文本总结子模块
│   └── audio_summarizer/ # 音频总结子模块
└── review/               # 智能复习主模块
    ├── planner/          # 复习计划子模块
    ├── reinforcement/    # 知识巩固子模块
    └── error_book/       # 错题本子模块
```

### 子模块内部标准结构（以 `video_summarizer` 为例）
```text
video_summarizer/
├── models/               # 数据模型 (Entities, DTOs, States)
├── services/             # 业务服务 (Repositories, Data Sources, APIs)
├── utils/                # 专属工具类 (Formatters, Helpers)
└── presentation/         # 页面与视图组件 (Screens, ViewModels, Components)
```

## 2. 模块划分标准

- **高内聚、低耦合**：每个主模块（如 `summarizer`）独立负责一个大的业务领域。
- **职责单一**：主模块下的子模块（如 `video_summarizer`）只关注具体维度的功能。
- **依赖关系**：
  - `app` 依赖所有业务模块。
  - 业务模块之间尽量解耦，通过回调或路由传递数据。
  - 所有业务模块均依赖 `common` 模块获取基础能力。

## 3. 组件层级规范

组件按复用范围和职责划分为三个层级：
1. **基础组件 (Base Components)**：位于 `common/ui/components`，如自定义按钮、对话框、进度条，与具体业务无关。
2. **业务组件 (Business Components)**：位于各子模块的 `presentation/components`，如 `video_summarizer` 中的 `SummaryOptionCard`，具有特定的业务逻辑。
3. **页面组件 (Page Components)**：位于各子模块的 `presentation/screens`，如 `VideoDownloadScreen`，负责组合业务组件并与 ViewModel 交互。

## 4. 重构实施计划

当前我们已经开始了重构的第一阶段：
1. **梳理与重新规划**：输出了本架构文档。
2. **包结构迁移**：正在将 `summarizer` 等模块按照新的子模块目录标准（`video_summarizer`, `text_summarizer` 等）进行内部包结构的重构。
3. **依赖更新**：同步更新 `app` 和各模块中的 `import` 路径，确保编译通过。
4. **测试与验证**：确保核心功能（如视频总结、AI辅导）在重构后依然可以正常运行，性能不退化。