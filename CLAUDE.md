# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an Android education application called "AI导学" (AI Learning Guide) that consists of multiple modules for educational assistance:
- Main app module with authentication and navigation
- AI Tutor module with chat capabilities and multimodal input
- Geometry Solver module with camera integration
- Timeline Map module
- Video Summarizer module
- Common module with shared utilities

## Project Architecture

### Modules Structure
- `app`: Main application module containing navigation, authentication, and UI structure
- `ai_tutor`: AI-powered tutoring system with text and image analysis capabilities
- `geometry_solver`: Mathematical geometry problem solving with camera integration
- `timeline_map`: Timeline mapping feature
- `video_summarizer`: Video content summarization
- `common`: Shared utilities including network clients, database, and preferences
- `:common`: Database entities, DAOs, network clients using Room, Retrofit, and OkHttp

### Tech Stack
- Kotlin (Kotlin Compose for UI)
- Jetpack Compose for UI
- Android Architecture Components (ViewModel, LiveData)
- Room Database
- Retrofit & OkHttp for networking
- Coroutines for asynchronous operations
- Navigation Compose for navigation
- DataStore for preferences
- TensorFlow Lite engine (for AI capabilities)
- CameraX for camera functionality
- MarkdownText for rich text display

### Key Features
- User authentication system (login/register)
- AI-powered tutoring with multimodal support (text, voice, images)
- Camera integration for capturing and analyzing problems
- Multi-language support (Chinese/English)
- Settings screen with language switching

## Build & Development Commands

### Building the Project
```bash
# Build the project
./gradlew build

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean build
./gradlew clean
```

### Running Tests
```bash
# Run all unit tests
./gradlew test

# Run connected device tests
./gradlew connectedAndroidTest

# Run tests for a specific module
./gradlew :app:testDebugUnitTest
./gradlew :ai_tutor:testDebugUnitTest
```

### Development Workflow
```bash
# Run spotless code formatting (if configured)
./gradlew spotlessApply

# Run lint checks
./gradlew lint

# Install debug app to connected device
./gradlew installDebug
```

## Important Configuration

### API Keys
- API keys may be configured in `common/config/AppConfig.kt` or as Android resources
- Base URL is configured in `AppConfig.BASE_URL`
- For development, ensure proper API endpoints are configured

### Dependencies
- Dependencies are managed in `libs.versions.toml` (not directly visible but referenced in build files)
- Common dependencies like Room, Retrofit, OkHttp, Gson, and Compose libraries are shared via the common module
- KSP (Kotlin Symbol Processing) is used for annotation processing

## Module-Specific Details

### AI Tutor Module (`ai_tutor`)
- Contains `TutorAgent` for AI interactions
- Supports text, voice, and image input methods
- Implements `DialogueManager` for conversation flow
- Uses `KnowledgeGraphManager` for knowledge representation
- Has `ImageAnalysisManager` for multimodal input processing
- Includes voice input functionality with `VoiceInputManager`

### Geometry Solver Module (`geometry_solver`)
- Integrates camera functionality using CameraX
- Contains `GeometrySolverEngine` for mathematical computations
- UI components for camera preview and problem solving

### Common Module (`common`)
- Shared database entities and DAOs
- Network client configuration using Retrofit and OkHttp
- User repository pattern for managing user state
- Preference management system
- Security and encryption utilities

## File Locations
- Main activities: `app/src/main/java/com/example/education/`
- UI components: `app/src/main/java/com/example/education/ui/`
- Database: `common/src/main/java/com/example/common/database/`
- Networking: `common/src/main/java/com/example/common/network/`
- Repository layer: `common/src/main/java/com/example/common/repository/`

## Internationalization
- String resources available in both Chinese and English
- Language switching handled in `MainViewModel`
- Resource files located in `app/src/main/res/values[-zh]/strings.xml`

## Important Notes
- The application follows MVVM architecture pattern
- Uses Jetpack Compose for modern UI development
- Multi-module architecture with common components shared via the `common` module
- Camera permissions must be handled properly
- The project uses modern Android development practices