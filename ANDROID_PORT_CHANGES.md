# Xenia-Android: Port Audit & Code Modifications

## Port Origin Verification

| Subsystem / Module | Origin Classification | Description |
|---|---|---|
| `cpu/backend/arm64` | **ORIGINAL XENIA (PRESERVED)** | ARM64 recompiler emitter & Xbyak AArch64 dynamic codegen backend. |
| `gpu/vulkan` | **ORIGINAL XENIA (PRESERVED)** | Core Xenos GPU command processor, SPIR-V translator & pipeline cache. |
| `kernel/stfs` | **ORIGINAL XENIA (PRESERVED)** | STFS container parsing, virtual file system & Xbox 360 title execution thunks. |
| `ui/surface_android` | **ANDROID ADAPTATION** | Bridge between Android `SurfaceView` / `ANativeWindow` and `VkSurfaceKHR`. |
| `base/filesystem_android` | **ANDROID ADAPTATION** | Storage Access Framework (SAF) integration via `ParcelFileDescriptor`. |
| `apu/audio_android` | **ANDROID ADAPTATION** | Low-latency audio rendering using Android AAudio realtime thread. |
| `hid/input_android` | **ANDROID ADAPTATION** | Multi-gamepad detection, auto-mapping (DualSense, DS4, Xbox), and on-screen touch overlay. |
| `ui/compose` | **NEW ANDROID FEATURE** | Material 3 Dark theme interface, Game Library, Cover Art scraper, MediaTek diagnostic engine. |

---

## Detailed File Change Log

### 1. `src/xenia/ui/surface_android.cc`
- **Change**: Adapted lifecycle handler to support dynamic surface recreation on orientation switch without invalidating guest emulation state.
- **Reason**: Android destroys and recreates the `ANativeWindow` on system UI changes and configuration updates.
- **Impact**: Smooth window resizing without tearing or lost guest context.

### 2. `src/xenia/base/filesystem_android.cc`
- **Change**: Added support for content URI resolution and streaming through `ParcelFileDescriptor.detachFd()`.
- **Reason**: Android 11+ enforces scoped storage requiring Storage Access Framework (SAF).
- **Impact**: Full compatibility with external SD cards and USB OTG drives.

### 3. `src/xenia/gpu/vulkan/vulkan_graphics_system.cc`
- **Change**: Added capability checks for ARM Mali GPUs (fallback paths when `VK_EXT_custom_border_color` is absent).
- **Reason**: Prevent validation layer crashes on Mali-G52 and older drivers.
- **Impact**: Broadened compatibility for MediaTek Helio and entry Dimensity SoCs.
