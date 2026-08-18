# Xenia-Android: Xbox 360 Emulator Port for Android ARM64

## Overview

**Xenia-Android** is a native port of the **Xenia Canary** Xbox 360 emulator codebase to Android ARM64 (`aarch64`), specifically engineered for high-performance execution on modern mobile hardware, with extensive support for **MediaTek Dimensity and Helio** SoCs, **Mali / Immortalis / PowerVR / Adreno** GPUs, and the **Vulkan** graphics API.

This project preserves the authoritative Xenia Canary core (PowerPC CPU emulation, JIT recompiler, Xenos GPU microcode translation, STFS/XEX kernel services) while replacing the Win32/DirectX platform layer with a modern Android/POSIX stack.

---

## Architectural Principles

1. **Authoritative Core Preservation**: Emulation logic (PowerPC CPU JIT, Xenos GPU pipeline, Xbox 360 memory map, STFS filesystem, and kernel thunks) is derived from Xenia Canary (`a5a18f5`).
2. **Native Android Platform Layer**: Replaces Windows-specific APIs (`HWND`, `VirtualAlloc`, `XAudio2`, `XInput`, `Direct3D 12`) with POSIX/Android equivalents (`ANativeWindow`, `mmap`, `AAudio`, `Android Gamepad API`, `Vulkan`).
3. **Hardware-Derived Compatibility**: No hardcoded SoC lockouts. MediaTek Helio and Dimensity chipsets are supported through dynamic capability detection of Vulkan extensions, ARM64 CPU features (LSE atomics, FPCR/FZ), and Mali GPU driver limits.
4. **Clean Decoupled Architecture**: Android UI (Jetpack Compose, Material 3 Dark Theme) communicates with the native emulation engine through a streamlined JNI bridge (`XeniaNativeBridge`).

---

## Subsystem Architecture

```
┌────────────────────────────────────────────────────────┐
│                   Xenia-Android UI                     │
│  (Jetpack Compose · Material 3 · Room · Game Library)  │
└──────────────────────────┬─────────────────────────────┘
                           │ JNI (XeniaNativeBridge)
┌──────────────────────────▼─────────────────────────────┐
│                 Android Platform Layer                 │
│  ┌───────────────────────┬───────────────────────────┐ │
│  │ Window & Surface      │ Storage & Filesystem      │ │
│  │ (ANativeWindow)       │ (SAF / ParcelFileDesc)    │ │
│  ├───────────────────────┼───────────────────────────┤ │
│  │ Gamepad & Input       │ Audio Engine              │ │
│  │ (AInputEvent / OTouch)│ (AAudio / Oboe)           │ │
│  └───────────────────────┴───────────────────────────┘ │
└──────────────────────────┬─────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────┐
│                 Xenia Canary Core                      │
│  ┌───────────────────────────────────────────────────┐ │
│  │ CPU: PowerPC 64-bit Recompiler (ARM64 JIT backend)│ │
│  ├───────────────────────────────────────────────────┤ │
│  │ GPU: Xenos Microcode -> SPIR-V -> Vulkan 1.1/1.3  │ │
│  ├───────────────────────────────────────────────────┤ │
│  │ Memory: 512MB Unified RAM (POSIX mmap / 4KB/16KB) │ │
│  ├───────────────────────────────────────────────────┤ │
│  │ Kernel: Xbox 360 OS syscall emulation & STFS      │ │
│  └───────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────┘
```

---

## MediaTek & Mali GPU Support Matrix

MediaTek processors are evaluated strictly based on real hardware capabilities:

| SoC Family | Example Chips | GPU Architecture | Vulkan Level | Compatibility Tier |
|---|---|---|---|---|
| **Dimensity Flagship** | 9300, 9200+, 9000 | Immortalis-G720 / G715 | Vulkan 1.3 | **Tier 1 (Maximum)** |
| **Dimensity High-End** | 8300, 8200, 8100, 1200 | Mali-G610 / G77 / G78 | Vulkan 1.2 / 1.3 | **Tier 1 (High Performance)** |
| **Dimensity Mid-Range** | 7050, 1080, 920, 700 | Mali-G68 / G57 | Vulkan 1.1 / 1.2 | **Tier 2 (Balanced)** |
| **Helio Performance** | G99, G96, G95, G90T | Mali-G57 / G76 | Vulkan 1.1 | **Tier 3 (Entry-Level 360)** |
| **Helio Entry** | P90, P65, G85, G80 | Mali-G52 / IMG 9XM | Vulkan 1.1 | **Tier 4 (Diagnostic/Light)** |

---

## Key Features

- **Real Binary Metadata Extraction**: Parses STFS (`CON`, `LIVE`, `PIRS`), XEX2 executables, and Xbox 360 ISOs (`GDFX`) to read real Title IDs, Media IDs, Title Names, and disc layouts.
- **Automated Cover Art & Metadata**: Multi-provider scraper (Xbox Live CDN, XboxUnity) with local caching in app storage and full offline mode support.
- **Universal Gamepad Subsystem**: Automatic detection and mapping for **DualSense (PS5)**, **DualShock 4 (PS4)**, **Xbox Series X/S**, **Xbox One**, and **Xbox 360** controllers, with real vibration haptics and 4-player multiplayer assignment.
- **On-Screen Touch Overlay**: Highly responsive dual analog sticks, D-pad, ABXY, bumpers, triggers, and Xbox Guide button.
- **Hardware Diagnostic Suite**: Built-in hardware inspector testing ARM64 LSE atomics, Vulkan extension compliance, and Mali driver workarounds with exportable reports.
