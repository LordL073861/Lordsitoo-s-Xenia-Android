# Xenia-Android: Technical Architecture Specification

## 1. CPU Recompiler (PowerPC -> ARM64 JIT)

### 1.1 Host Execution Model
- **Source Architecture**: IBM PowerPC 64-bit with AltiVec SIMD extensions (Xenon: 3 cores, 6 hardware threads @ 3.2 GHz).
- **Target Architecture**: ARM64 (ARMv8-A / ARMv8.2-A / ARMv9-A).
- **Codegen Backend**: Xbyak AArch64 dynamic code generator.

### 1.2 Calling Convention & Register Allocation
- Xenon GPRs (r0-r31) mapped to ARM64 registers (x19-x28, x9-x15) with spill stack.
- AltiVec 128-bit vector registers (v0-v127) mapped to ARM64 NEON registers (q0-q31).
- Host stack management conforms strictly to the AAPCS64 standard.

### 1.3 Memory Ordering & Atomics
- PowerPC uses a weakly-ordered memory model with `sync` and `eieio` barrier instructions.
- On ARM64 hosts with **Large System Extensions (LSE)** (e.g., Dimensity 9000+, Snapdragon 8 Gen 1+), atomic instructions use `LDADD`, `SWP`, `CAS` directly.
- On older ARMv8.0 cores (e.g., Helio G90T, Helio P90), atomic sequences use `LDXR`/`STXR` loops with appropriate `DMB ISH` barriers.

---

## 2. GPU Emulation & Vulkan Pipeline

### 2.1 Xenos GPU Architecture
- Custom ATI Xenos GPU featuring unified shader architecture, 10MB embedded eDRAM (EDRAM) with 256GB/s bandwidth, and hardware tessellation.

### 2.2 Vulkan Pipeline Mapping
- **Shaders**: Xenos microcode is disassembled, translated to an intermediate representation (IR), and compiled into standard SPIR-V bytecodes.
- **eDRAM Emulation**: Implemented via Vulkan Render Pass subpass attachments and compute shader tile resolve passes.
- **Surface Presentation**: Native `ANativeWindow` provided by Android `SurfaceView` bound to `VkSurfaceKHR` via `VK_KHR_android_surface`.
- **Swapchain**: Utilizes `VK_PRESENT_MODE_MAILBOX_KHR` for low latency or `VK_PRESENT_MODE_FIFO_KHR` for strict 60Hz/30Hz VSync timing.

### 2.3 Mali / PowerVR GPU Driver Accommodations
- Mali GPUs utilize tile-based deferred rendering (TBDR). Custom border colors and dynamic rendering extensions (`VK_KHR_dynamic_rendering`) are verified at startup; if absent, traditional `VkRenderPass` paths are selected.
- Conservative descriptor indexing limits are enforced on Mali G52/G57 drivers to prevent driver faults.

---

## 3. Storage & Virtual Memory Subsystems

### 3.1 512MB Unified Memory Allocation
- The Xbox 360 physical address space (512MB UMA) is allocated via POSIX `mmap` with `PROT_READ | PROT_WRITE`.
- Page size alignment is dynamically calibrated for both standard 4KB and modern 16KB ARM64 Linux kernels.

### 3.2 Storage Access Framework (SAF)
- Game files (.iso, .xex, .stfs) are accessed using Android `ContentResolver.openFileDescriptor(uri, "r")`.
- The native file descriptor is passed directly to the POSIX I/O layer via `dup()` to ensure zero-copy streaming.

---

## 4. Audio Engine

- High-performance audio output is managed through the Android **AAudio** native API with low-latency `AAUDIO_PERFORMANCE_MODE_LOW_LATENCY`.
- Audio processing runs strictly on a dedicated high-priority realtime audio thread (`SCHED_FIFO`), completely decoupled from the UI and emulation main loops.
