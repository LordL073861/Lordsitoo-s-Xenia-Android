# Xenia-Android: Build & Compilation Guide

## Prerequisites & Environment Requirements

- **Android SDK Version**: Min SDK 24 (Android 7.0), Target SDK 36 (Android 15+ / 16)
- **Target ABI**: `arm64-v8a` (Primary target for ARM64 devices including MediaTek Dimensity & Helio)
- **Java / Kotlin**: JDK 17 / 21, Kotlin 2.2+, Jetpack Compose Compiler
- **Vulkan Header Support**: Vulkan 1.1 / 1.2 / 1.3 SDK headers

---

## Build Commands

### 1. Verification & Compilation
To compile and assemble the full application package:
```bash
gradle :app:assembleDebug
```

### 2. Release Package Generation
To generate the optimized release APK:
```bash
gradle :app:assembleRelease
```

### 3. Running Unit and Architecture Tests
```bash
gradle :app:testDebugUnitTest
```

---

## Compiler Flags & Optimization Parameters

For the native ARM64 compilation pipeline:
- `-march=armv8-a+crc+crypto`
- `-O3 -fomit-frame-pointer -flto`
- `-ffast-math -fno-finite-math-only`
- `-DVK_USE_PLATFORM_ANDROID_KHR`
- `-DXENIA_PLATFORM_ANDROID=1`
