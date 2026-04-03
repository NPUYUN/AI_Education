# AI Education (Smart Learning Assistant)

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green.svg" alt="Android">
  <img src="https://img.shields.io/badge/Language-Kotlin-blue.svg" alt="Kotlin">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-blueviolet.svg" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/Architecture-Clean%20Architecture%20%7C%20MVI-orange.svg" alt="Clean Architecture">
  <img src="https://img.shields.io/badge/LLM-Multimodal-yellow.svg" alt="LLM">
</p>

[English](README.md) | [简体中文](README_ZH.md)

A comprehensive, multimodal, LLM-powered smart education Android application. This project uses modern Android development technology stacks (Jetpack Compose, Hilt, Room, MVI architecture) and achieves high cohesion and low coupling through a multi-module architecture. It provides a complete learning loop from **Knowledge Tutoring**, **Smart Problem Solving**, **Multimodal Summarization** to **Review and Reinforcement**.

---

## 🌟 Core Modules

This project is divided into four core business modules to deeply cover all learning scenarios:

### 1. 🤖 AI Tutor
* **Multimodal Chat**: Supports smart Q&A with text, images, and voice, featuring complete context memory and historical dialogue stream management.
* **Timeline Map**: Combines the OSMDroid map engine with LLMs to concretize abstract historical events into interactive nodes on a global map. Supports pinch-to-zoom, panoramic world map preloading, dynamic markers, and detailed linkages to help structure historical events and geographic contexts.
* **Personal Center & Config**: Unified management and seamless switching of global LLM model parameters (API Key, Base URL, Model Selection).

### 2. 📝 Smart Solver
* **Multimodal Input**: Supports photo-based problem solving (integrated with CameraX, featuring built-in image rotation and cropping), gallery uploads, and quick history retrieval. Supports mixed text and image queries with an interactive "wait-to-solve" flow.
* **Smart Classification & Parsing**: Automatically identifies the subject of the problem (Geometry, Algebra, Physics, Chemistry, Biology, etc.) and loads subject-specific system Prompts. Features an intelligent fallback mechanism to pure vision models (e.g., `qwen-vl-plus`) when identifying problems containing images.
* **Dynamic Geometry Canvas**: Deeply integrated with the `exp4j` math engine to dynamically draw function graphs (including tangents and shaded areas), geometric shapes, physics force diagrams, and chemistry experiment schematics based on AI-output JSON instructions.
* **Split Result View**: Elegantly separates the final answer (shown on the main screen) from the detailed step-by-step derivation (displayed in a full-screen dialog).
* **Error Book Integration**: Supports one-click addition to the Error Book after successful problem solving, synchronizing states in the solving history.

### 3. 📑 Intelligent Summarizer
* **Multi-format Text Summarization**: Supports direct pasting of long texts, or importing and parsing PDF (PDFBox), Word (XML parsing), HTML (Jsoup), TXT/CSV files to extract core summaries.
* **Audio & Video Summarization**: Efficient offline Automatic Speech Recognition (ASR) using Sherpa ONNX. Implements advanced **streaming memory chunking** to prevent JNI OOM and C++ aborts when processing long media files, combined with LLMs for refinement and summarization.
* **Dialogue Review & Universal PDF Export**: Supports importing historical dialogues from the Room database for review. All summarization results (text, audio, video, dialogue) and review modules can be **exported to beautifully formatted PDF documents** with one click for easy local archiving and sharing.

### 4. 📚 Smart Review
* **Ebbinghaus Review Planner**: Supports customized review schedules for different subjects, intelligently planning daily review tasks using the memory curve algorithm.
* **Error Book & Reinforcement**: Provides error search, categorized browsing, and reinforcement quizzes to form a complete learning feedback loop.

---

## 🛠️ Tech Stack & Architecture

### Architecture Standards
* **Pattern**: Clean Architecture + MVVM + Unidirectional Data Flow (MVI mindset with StateFlow).
* **Multi-module Decoupling**: Split by business boundaries (`app`, `common`, `ai_tutor`, `solver`, `summarizer`, `review`). Features a robust `FeatureApi` mechanism injected via Hilt, ensuring zero cross-module hardcoded navigation dependencies in the main `app` module. Each module is strictly layered into `models`, `services`, and `presentation`.
* **High Availability & Offline Fallback**: Built-in `NetworkMonitor` listens to network states in real-time, providing global offline fallback strategies (seamless switching to local Error Book, history, offline ASR, and local canvas rendering when offline).

### Core Frameworks
* **UI Layer**: Jetpack Compose (fully applies Material 3 specifications, unified `CenterAlignedTopAppBar`, NavHost `AnimatedVisibility` transitions, Dark Mode adaptation, and system-level Accessibility support).
* **Markdown & Math**: Enhanced `SafeMarkdownText` globally integrating Markwon with custom LaTeX regex parsing (`$$`, `$`) and Coil for mixed text-and-image rendering.
* **Dependency Injection**: Dagger Hilt, including custom Coroutine DispatcherProvider for enhanced testability.
* **Local Persistence**: Room Database (with relational tables, TypeConverters) and DataStore/SharedPreferences.
* **Networking**: Retrofit, OkHttp, unified Server-Sent Events (SSE) streaming API for LLMs. Custom timeouts for long-text generations (e.g., 90s for Timeline Map).
* **Security**: API Key secure storage mechanism based on Android NDK (C++) with XOR encryption and BuildConfig graceful fallback.
* **Map Engine**: Integrated OSMDroid open-source mapping with configured tile mirrors for fast and stable loading.
* **Multimedia & Processing**: CameraX (Camera), Sherpa ONNX (Offline ASR), Coil (Image loading/cropping), PDFBox-Android (PDF Parsing).

---

## 📁 Project Structure

```text
AI_Education/
├── app/                  # Main entry, DI config, global config, and main navigation
├── common/               # Core shared module (network, DB, base UI, utils)
├── ai_tutor/             # AI Tutor module
│   ├── multimodal_chat/  # Chat subsystem
│   └── timeline_map/     # Timeline Map subsystem
├── solver/               # Smart Solver module
│   ├── geometry_solver/  # Geometry subsystem (Dynamic Canvas)
│   ├── algebra_solver/   # Algebra subsystem
│   └── comprehensive/    # Main solver flow (Camera, Upload)
├── summarizer/           # Intelligent Summarizer module
│   ├── video_summarizer/ # Video summarization
│   ├── text_summarizer/  # Multi-format document parsing & PDF Export
│   ├── audio_summarizer/ # Offline ASR summarization
│   └── dialogue_summarizer/ # Dialogue review
└── review/               # Smart Review module
    ├── planner/          # Ebbinghaus planner
    ├── reinforcement/    # Reinforcement quizzes
    └── error_book/       # Error book subsystem
```

---

## 🚀 Build & Run Instructions

### 1. Prerequisites
* **Android Studio**: Ladybug or higher recommended.
* **JDK**: 17 or higher.
* **Android SDK**: Ensure your local Android SDK is properly configured.

### 2. Configure API Key
The project relies on LLM APIs (OpenAI-compatible, defaults to Qwen). For security, API Keys **should not** be hardcoded.
Add your configuration to the `local.properties` file in the root directory (create it if it doesn't exist):

```properties
API_KEY=your_actual_api_key_here
BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1/
MODEL_NAME=qwen-vl-plus
```
*(Note: A global settings dialog is also available in the app to modify these configurations at runtime)*

### 3. Build & Clean
* Simply click `Run` in Android Studio to build and launch the project.
* If you encounter Gradle cache issues or build deadlocks, you can run the clean script in the root directory:
  * **Windows**: Double-click `clean_project.bat` to deep clean `.gradle`, `.idea`, `build` directories and kill related background processes.

---

## 🧪 Testing & Quality Assurance

* **Unit Testing**: Covers all core business ViewModels, data transformation layers, and Room DB operations using Mockito, Turbine, and UnconfinedTestDispatcher to ensure Coroutine testing stability and StateFlow correctness.
* **Code Style**: Constrained by `ktlint` to ensure high code quality. Format code automatically using `./gradlew ktlintFormat`.