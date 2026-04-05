# Dongda Zhida

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
* **Split Result View & Knowledge Point Extraction**: Elegantly separates the final answer (shown on the main screen) from the detailed step-by-step derivation (displayed in a full-screen dialog), and intelligently extracts core knowledge points as a concise summary for historical records.
* **Error Book Integration**: Supports one-click addition to the Error Book after successful problem solving, synchronizing states in the solving history.

### 3. 📑 Intelligent Summarizer
* **Multi-format Text Summarization**: Supports direct pasting of long texts, or importing and parsing PDF (PDFBox), Word (XML parsing), HTML (Jsoup), TXT/CSV files to extract core summaries.
* **Audio & Video Summarization**: Efficient offline Automatic Speech Recognition (ASR) using Sherpa ONNX. Implements advanced **streaming memory chunking** to prevent JNI OOM and C++ aborts when processing long media files, combined with LLMs for refinement and summarization.
* **Dialogue Review & Universal PDF Export**: Supports importing historical dialogues from the Room database for review. All summarization results (text, audio, video, dialogue) and review modules can be **exported to beautifully formatted PDF documents** with one click for easy local archiving and sharing.

### 4. 📚 Smart Review
* **Ebbinghaus Review Planner**: Supports customized review schedules for different subjects, intelligently planning daily review tasks using the memory curve algorithm.
* **Error Book & Smart Variants**: Features collapsible error cards, categorized browsing, and an integrated "One-click Test" that forces the AI to output structured JSON variant problems (estimated similarity ≥0.85). It also offers an immersive single-question answering modal, an interactive answer sheet grid, AI batch grading, and the ability to track historical variant test records.

---

## 🛠️ Tech Stack & Architecture

### Architecture Standards
* **Pattern**: Clean Architecture + MVVM + Unidirectional Data Flow (MVI mindset with StateFlow).
* **Multi-module Decoupling**: Split by business boundaries (`app`, `common`, `ai_tutor`, `solver`, `summarizer`, `review`). Features a robust `FeatureApi` mechanism injected via Hilt, ensuring zero cross-module hardcoded navigation dependencies in the main `app` module. Each module is strictly layered into `models`, `services`, and `presentation`.
* **High Availability & Offline Fallback**: Built-in `NetworkMonitor` listens to network states in real-time, providing global offline fallback strategies (seamless switching to local Error Book, history, offline ASR, and local canvas rendering when offline).

### Core Frameworks
* **UI Layer**: Jetpack Compose (fully applies Material 3 specifications, globally unified and deduplicated `CenterAlignedTopAppBar` navigation, NavHost `AnimatedVisibility` transitions, Dark Mode adaptation, and system-level Accessibility support).
* **Markdown & Math**: Globally enhanced `SafeMarkdownText` component, integrating Markwon with custom LaTeX regex parsing (`$$` and `$`). It dynamically calculates the base font size using `LocalDensity`, thoroughly fixing underlying rendering bugs where block formulas wrapping Chinese text caused microscopic fonts.
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

### 1. Environment & Device Prerequisites
* **IDE**: Android Studio Ladybug or higher is recommended.
* **JDK**: JDK 17 or higher.
* **Android SDK**: Ensure your local Android SDK is properly configured (Min SDK 24, Target SDK 35).
* **Test Device**: A **physical Android device** is highly recommended. This project relies heavily on the camera (photo-based problem solving) and microphone (speech recognition), which may not function optimally on an emulator.

### 2. Configure LLM API Key
The core functionality relies on Large Language Model APIs (defaults to Qwen, using an OpenAI-compatible interface). For security, API Keys **must not** be hardcoded.
Please create a `local.properties` file in the project root directory and add your configurations:

```properties
# Replace with your actual API Key
API_KEY=your_actual_api_key_here
# Modify Base URL and Model Name if using a different provider
BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1/
MODEL_NAME=qwen-vl-plus
```
*(Note: You can also dynamically modify and save these configurations at runtime via "Profile -> Global Settings" in the app)*

### 3. Build & Download Dependencies
* **Automatic Dependency Sync**: The project is configured with domestic mirror sources (for HuggingFace and Github resources) to accelerate downloads in mainland China. Upon first opening the project in Android Studio, it may automatically download offline speech recognition (Sherpa ONNX) models. Please ensure a stable internet connection.
* **Run**: Wait for Gradle Sync to complete, then click the `Run` button in Android Studio to deploy to your device.

### 4. Troubleshooting
* **Build Deadlocks or Cache Issues**: If you encounter Gradle cache-related build failures, double-click the `clean_project.bat` script (Windows) in the root directory. This script performs a deep clean of `.gradle`, `.idea`, and `build` directories, and terminates lingering Gradle processes. Afterwards, reopen the project.
* **Permissions**: The app will request permissions for the camera, microphone, storage, and photo gallery upon first use. Please grant these to ensure proper functionality.

---

## 🧪 Testing & Quality Assurance

* **Unit Testing**: Covers all core business ViewModels, data transformation layers, and Room DB operations using Mockito, Turbine, and UnconfinedTestDispatcher to ensure Coroutine testing stability and StateFlow correctness.
* **Code Style**: Constrained by `ktlint` to ensure high code quality. Format code automatically using `./gradlew ktlintFormat`.