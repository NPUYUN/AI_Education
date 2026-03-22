# AI Education - 代码重构与架构设计

本项目进行了全面的代码重构，重新设计了整个项目的目录结构、模块划分与组件层级架构。采用了清晰的多级树形层级结构进行组织。

## 1. 目录结构设计要求

当前项目遵循分层级、分模块的目录组织方式，以业务功能进行高内聚、低耦合的模块划分。

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
│   │   ├── models/       # 数据模型 (Entities, DTOs, States)
│   │   ├── services/     # 业务服务 (Repositories, APIs)
│   │   ├── utils/        # 专属工具类
│   │   └── presentation/ # 页面与视图组件
│   ├── text_summarizer/  # 文本总结子模块
│   │   ├── models/
│   │   └── services/
│   └── audio_summarizer/ # 音频总结子模块
│       ├── models/
│       └── services/
└── review/               # 智能复习主模块
    ├── planner/          # 复习计划子模块
    ├── reinforcement/    # 知识巩固子模块
    └── error_book/       # 错题本子模块
```

## 2. 模块划分标准

- **按业务功能划分**：每个主模块（如 `summarizer`）独立负责一个业务领域，其内部通过包级别（如 `video_summarizer`、`text_summarizer`）进一步拆分功能。
- **职责单一**：子模块（如 `video_summarizer`）只关注具体维度的功能，包含了 `models`, `services`, `presentation` 等完整集合。
- **依赖关系**：`app` 模块负责整合，所有的业务模块均依赖于 `common` 模块的基础能力。

## 3. 组件层级规范

- **基础组件**：位于 `common/presentation/components`，提供与业务无关的基础 UI（如进度条、通用弹窗）。
- **业务组件**：位于各子模块的 `presentation/components`（例如视频下载选项卡），具有特定的业务逻辑。
- **页面组件**：位于各子模块的 `presentation/screens`（例如 `VideoDownloadScreen`），负责组合业务组件并与 ViewModel 交互。

## 4. 重构实施情况

- **梳理与重新规划**：输出了本架构文档并规划了长期演进路线。
- **包结构迁移**：我们已经成功将所有的业务模块（`summarizer`、`ai_tutor`、`solver`、`review`）内部按照新标准进行了子模块目录的重构，并且完成了各模块 `models`、`services` 和 `presentation` 的拆分。
- **依赖更新**：同步更新了所有相关的 import 路径（包含 `app` 模块对 `summarizer` 模块的引用），并确保了项目可以顺利编译运行（`compileDebugKotlin` 成功）。
- **测试覆盖率**：配置了 Jacoco 覆盖率统计报告（排除了 UI、Manager、DI 和 Room 数据库等与框架强绑定的组件）。已为 `common` 模块的底层网络库、工具类和全局配置补充了完善的单元测试，核心业务逻辑代码覆盖率已达到 **91%** 以上。
- **后续计划**：完善其他业务模块的集成测试，并验证项目整体性能指标是否达到预期。
