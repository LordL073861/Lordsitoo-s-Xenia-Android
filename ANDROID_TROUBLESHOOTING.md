# Xenia-Android: Troubleshooting & Diagnostic Guide

## 1. Vulkan Surface Initialization Errors
- **Symptom**: `VK_ERROR_INITIALIZATION_FAILED` on `vkCreateAndroidSurfaceKHR`.
- **Cause**: The Android `SurfaceView` has not finalized its native window handle prior to initialization.
- **Resolution**: Ensure `surfaceCreated` callback on `SurfaceHolder.Callback` has fully dispatched before triggering native renderer attach.

## 2. MediaTek / Mali GPU Specific Diagnostics
- **Symptom**: Black screen or shader failure on older Mali-G52/G57 chipsets.
- **Cause**: Lack of `VK_EXT_custom_border_color` or conservative descriptor set limits.
- **Resolution**: The app automatically selects fallback render pass shaders and clamp-to-edge samplers when these extensions are missing.

## 3. Storage Access Framework (SAF) URI Revocation
- **Symptom**: "Game location is no longer accessible".
- **Cause**: The user moved the game file or Android revoked persistent directory permissions after a clean boot.
- **Resolution**: Open the Games Library, select "Rescan Folder" and re-grant directory access through the Android document tree picker.

## 4. JIT Memory Mapping on 16KB Page Kernels
- **Symptom**: Signal 11 (SIGSEGV) during executable code emission.
- **Cause**: Android devices running Linux kernels configured with 16KB page granularity require memory chunks aligned to `0x4000` rather than `0x1000`.
- **Resolution**: Native `mmap` allocations in Xenia-Android automatically query `sysconf(_SC_PAGESIZE)` and align recompiled code buffers accordingly.
