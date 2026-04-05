# Project Architecture & Refactoring Document

[English](ARCHITECTURE.md) | [简体中文](ARCHITECTURE_ZH.md)

This document defines the overall architectural pattern, directory structure standards, component hierarchy design, and multi-module splitting criteria for the Dongda Zhida project.

## 1. Architectural Pattern

This project fully adopts the **Clean Architecture** combined with the **MVVM** design pattern, and implements Unidirectional Data Flow based on **MVI** principles using StateFlow.

- **UI Layer (Presentation)**: Uses Jetpack Compose to build declarative UIs. Pages (Screens) are only responsible for observing states and dispatching events, containing no business logic.
- **ViewModel Layer**: Receives intents/events from the UI layer, executes business logic by calling the Repository, and ultimately exposes an immutable, unified state flow (`UiState`) for the UI to consume.
- **Domain & Data Layer (Services/Models)**:
  - Repository Pattern: Shields data sources from the outside (local Room database or remote network APIs).
  - Data Model Separation: Strictly distinguishes between network DTOs, database Entities, and State Models required by the UI layer.

## 2. Directory Structure Standards

The project is organized using a Multi-module approach, dividing modules by business functionality to achieve high cohesion and low coupling.

### Top-Level Directory Structure
```text
AI_Education/
├── app/                  # Main application entry, handles Hilt DI config, global config, and main navigation routing
├── common/               # Shared core module (network layer, Room DB, base UI components, global utils, base managers)
├── ai_tutor/             # Core AI Tutor module
│   ├── multimodal_chat/  # Multimodal chat sub-module
│   └── timeline_map/     # Timeline map sub-module
├── solver/               # Smart Solver module
│   ├── geometry_solver/  # Geometry solving sub-module (includes dynamic graphics rendering engine)
│   ├── algebra_solver/   # Algebra solving sub-module
│   └── comprehensive/    # Comprehensive solving sub-module (photo solving, auto-classification, error book integration)
├── summarizer/           # Intelligent Summarizer module
│   ├── video_summarizer/ # Video summarization sub-module
│   ├── text_summarizer/  # Text summarization sub-module (multi-format parsing & PDF export)
│   ├── audio_summarizer/ # Audio/speech summarization sub-module (offline ASR)
│   └── dialogue_summarizer/ # Dialogue history summarization sub-module
└── review/               # Smart Review module
    ├── planner/          # Review planner sub-module (Ebbinghaus forgetting curve)
    ├── reinforcement/    # Knowledge reinforcement sub-module
    └── error_book/       # Error book sub-module
```

### Sub-module Internal Standard Structure (Example: `video_summarizer`)
To maintain high consistency in code organization, every sub-module must strictly adhere to the following package structure:
```text
video_summarizer/
├── models/               # Data model definitions
│   ├── entities/         # Room database entity classes
│   ├── dtos/             # Network request/response Data Transfer Objects
│   └── states/           # UI state classes (UiState)
├── services/             # Business services and data sources
│   ├── api/              # Retrofit interface definitions
│   ├── repository/       # Data repository implementations
│   └── usecases/         # Use cases for complex business logic (optional)
├── utils/                # Exclusive utility classes (Formatters, Helpers)
└── presentation/         # Presentation layer
    ├── components/       # Exclusive business components (e.g., SummaryOptionCard)
    ├── screens/          # Page-level components (e.g., VideoDownloadScreen)
    └── viewmodels/       # ViewModels corresponding to pages
```

## 3. Module Division and Dependency Standards

- **High Cohesion, Low Coupling**: Each main module (e.g., `summarizer`) independently handles a large business domain.
- **Single Responsibility**: Sub-modules under the main module (e.g., `video_summarizer`) only focus on specific functional dimensions.
- **Dependency Rules**:
  - The `app` module is the ONLY host module that can depend on all other business modules.
  - **Horizontal Isolation (FeatureApi Routing Mechanism)**: Peer business modules (`ai_tutor`, `solver`, `summarizer`, `review`) are **strictly prohibited from depending on each other**. Each business module must implement the `FeatureApi` interface defined in the `common` module (e.g., `AiTutorFeatureApi`, `SolverFeatureApi`), injected uniformly by Hilt in the `app` module, and finally call `registerGraph` in `MainScreen` to register routes. This achieves complete decoupling of cross-module communication and page navigation.
  - **Core Foundation**: All business modules have a one-way dependency on the `common` module to obtain basic capabilities (e.g., unified LLM network layer, DispatcherProvider, global components, and utils).

## 4. Component Hierarchy and Compose Best Practices

Compose UI components are divided into three levels based on their scope of reuse and responsibility:
1. **Base Components**: Located in `common/presentation/components`. Provides business-agnostic basic UI (e.g., upgraded `SafeMarkdownText`, standardized `ApiKeyDialog`, global loading indicators).
2. **Business Components**: Located in `presentation/components` of each sub-module. Possesses specific business logic but is reused across multiple pages within the module.
3. **Page Components**: Located in `presentation/screens` of each sub-module. Responsible for composing business components and solely responsible for interacting with the ViewModel.

### Compose Architecture & Best Practices
1. **Unified Top App Bar Standard**: Must use `CenterAlignedTopAppBar` (along with `TopAppBarDefaults.centerAlignedTopAppBarColors`), set a centered title via `title = { Text(...) }`, and provide a standard back button via `navigationIcon` to align with and decouple from system back button behavior.
2. **Lifecycle-Aware State Collection**: Directly using `.collectAsState()` is prohibited. All UI layer observations of ViewModel `StateFlow` must use `.collectAsStateWithLifecycle()` from the `lifecycle-runtime-compose` package to avoid resource consumption when the app is in the background.
3. **One-time UI Event Dispatching (UiEvent)**: For non-continuous events like Snackbar prompts, Toasts, and page navigation, placing them in `StateFlow` (which causes repeated triggering) is prohibited. They must be sent using `Channel<UiEvent>` and collected/consumed by `LaunchedEffect` in the UI layer.
4. **Animation Standards**: Each module must supplement core interaction paths with animation feedback using `AnimatedVisibility` (e.g., `fadeIn+expandVertically` / `fadeOut+shrinkVertically`) and NavHost page transitions (slide/fade).
5. **Height Measurement Adaptation**: When applying `verticalScroll` to a `Column`, internal `MarkdownText` might become invisible or truncated due to receiving infinite height constraints. `verticalScroll` should be applied directly to the `modifier` of `MarkdownText`, combined with `Box` or `weight(1f)` on the parent container to ensure correct height measurement.

## 5. State Flow and Exception Handling Standards

1. **StateFlow Replaces LiveData/MutableState**: All ViewModels must use `MutableStateFlow` to expose a single UI state.
   ```kotlin
   private val _uiState = MutableStateFlow(MyUiState())
   val uiState: StateFlow<MyUiState> = _uiState.asStateFlow()
   ```
2. **Safe Coroutine Scheduling**: Must obtain the coroutine context through the injected `DispatcherProvider`. Hardcoding `Dispatchers.IO` or `Dispatchers.Main` is strictly prohibited to ensure complete controllability of ViewModel unit tests.
3. **Unified Exception Catching**: Error-prone operations such as network requests, file parsing, and model inference must be caught using `try-catch` in the ViewModel layer. Error messages must be converted into `uiState.error` and handed over to the UI layer's unified ErrorCard component for rendering.

## 6. Testing Standards

- **Unit Testing**:
  - Core logic (Repository business aggregation, ViewModel state transitions, utility class parsing) must be covered by unit tests.
  - The code coverage for the infrastructure in the `common` module is required to be above 90%.
  - Must uniformly adopt `UnconfinedTestDispatcher` to resolve coroutine suspension inconsistencies, and be careful to use `anyOrNull()` instead of `any()` during Mockito matching to accommodate Kotlin's nullable types.
- **UI Testing / Compose Testing**: Can be selectively added for critical user paths (e.g., input box debouncing, navigation jumps).

## 7. Security and Offline Strategy Standards

1. **API Key Secure Storage**:
   - Hardcoding plaintext API Keys in the code is strictly prohibited.
   - Uses Android NDK (C/C++) layer storage and obfuscates the Key using simple encryption like XOR, providing it to upper layers via JNI interface (`NativeLib`). Due to the complexity of cross-platform configuration, it is currently configured in `local.properties` and obtained via `BuildConfig` and `GlobalConfigRepository` as a graceful fallback security solution.
2. **Global Offline Degradation Strategy**:
   - Must monitor the network connection status in real-time through the global singleton `NetworkMonitor` (based on `ConnectivityManager.NetworkCallback` + `StateFlow`).
   - All interactions involving cloud LLM inference or network downloads must check the network status at the beginning of ViewModel intent processing. Requests must be blocked when offline, gracefully degrading to reading local data or providing clear offline prompts.

## 8. Multimodal and Low-level JNI Memory Standards

When dealing with large image processing (e.g., photo-based problem solving) or offline model inference (e.g., Sherpa-ONNX speech transcription), Native and JVM memory must be strictly managed:
1. **Bitmap Lifecycle and Memory Recycling**: Temporary Bitmaps generated after cropping or rotation processing (e.g., base64 preview images generated in `AiTutorViewModel`, `SolverViewModel`) must have their Native memory actively reclaimed by calling `bitmap.recycle()` after conversion or upload is complete, to avoid OOM during multimodal input.
2. **Streaming Parsing of Audio/Video to Prevent Crashes**:
   - When parsing large audio files using C++'s `WaveReader`, **passing the entire file to the bottom layer at once is strictly prohibited**, otherwise it will cause `Channel unrecoverably broken` or `SIGABRT`.
   - Must use `RandomAccessFile` in the Kotlin layer to parse WAV in chunks (skipping the 44-byte header to process PCM), allocating only small chunks (e.g., 30 seconds/3MB FloatArray) at a time and looping them to `acceptWaveform` for streaming decoding.
3. **Multi-threading Concurrency Limits**: In some JNI inference operations (like certain offline models of Sherpa-ONNX), multi-threading race conditions leading to underlying crashes must be prevented by configuring `numThreads=1`.

## 9. Global and Configuration Standards

1. **Internationalization (i18n)**: **Hardcoded Chinese strings are strictly prohibited** in the project. All text displayed to users must be extracted to `strings.xml` and referenced via `stringResource(R.string.xxx)` (Compose) or `Context.getString()`. Supported languages (like default `values` and `values-en`) must be configured in `locales_config.xml` to achieve app-level dynamic language switching.
2. **JSON Parser**: Uniformly use `Gson` as the serialization and deserialization tool. Disabling `JSONObject` to ensure type safety and code readability in object mapping.
3. **Mirror Sources and Network Downloads**: To improve download stability in mainland China, dependency downloads (like `ModelDownloader.kt`, `sherpa_setup.gradle`) have been fully replaced with domestic mirrors (e.g., `kkgithub.com`, `ghproxy`, `hf-mirror.com`). New model dependencies must follow this standard.