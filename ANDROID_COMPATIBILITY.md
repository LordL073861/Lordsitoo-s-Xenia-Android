# Xenia-Android: SoC & GPU Compatibility Matrix

## 1. MediaTek Dimensity Series (ARM64)

| Chipset | CPU Setup | GPU | Vulkan Version | Compatibility Status | Recommended Settings |
|---|---|---|---|---|---|
| **Dimensity 9300 / 9300+** | 4x Cortex-X4 + 4x Cortex-A720 | Immortalis-G720 MC12 | Vulkan 1.3 | **Playable / Optimal** | 1080p, Mailbox VSync, 16x Aniso |
| **Dimensity 9200 / 9200+** | 1x X3 + 3x A715 + 4x A510 | Immortalis-G715 MC11 | Vulkan 1.3 | **Playable / Optimal** | 720p/1080p, Mailbox VSync |
| **Dimensity 9000 / 9000+** | 1x X2 + 3x A710 + 4x A510 | Mali-G710 MC10 | Vulkan 1.3 | **Playable / High** | 720p, Fifo VSync |
| **Dimensity 8300 / 8300 Ultra** | 4x A715 + 4x A510 | Mali-G615 MC6 | Vulkan 1.3 | **Playable / High** | 720p, Fifo VSync |
| **Dimensity 8200 / 8100** | 4x A78 + 4x A55 | Mali-G610 MC6 | Vulkan 1.2 | **Playable / Good** | 720p, Fifo VSync |
| **Dimensity 7050 / 1080 / 920** | 2x A78 + 6x A55 | Mali-G68 MC4 | Vulkan 1.2 | **In-Game / Moderate** | 0.75x Scale, Fifo VSync |
| **Dimensity 700 / 6080** | 2x A76 + 6x A55 | Mali-G57 MC2 | Vulkan 1.1 | **Loads / Entry** | 0.5x-0.75x Scale, Low FX |

---

## 2. MediaTek Helio Series (ARM64)

| Chipset | CPU Setup | GPU | Vulkan Version | Compatibility Status | Notes |
|---|---|---|---|---|---|
| **Helio G99 / G96 / G95** | 2x A76 + 6x A55 | Mali-G57 MC2 / G76 MC4 | Vulkan 1.1 | **In-Game (Light Titles)** | Requires strict 4KB/16KB page checks |
| **Helio G90T** | 2x A76 + 6x A55 | Mali-G76 MC4 | Vulkan 1.1 | **Loads / In-Game** | Heavy thermal throttling mitigation |
| **Helio P90 / P65** | 2x A75 + 6x A55 | PowerVR GM9446 / Mali-G52 | Vulkan 1.1 | **Diagnostic / Entry** | Fallback render pass mode enabled |

---

## 3. Required Vulkan Extensions Verification Table

| Extension Name | Purpose | Status on Modern Mali/Adreno |
|---|---|---|
| `VK_KHR_surface` | Core Window Presentation | **Required (Mandatory)** |
| `VK_KHR_android_surface` | Android Native Surface presentation | **Required (Mandatory)** |
| `VK_KHR_swapchain` | Presentation Swapchain management | **Required (Mandatory)** |
| `VK_KHR_dedicated_allocation` | Memory allocation efficiency | **Required (Mandatory)** |
| `VK_KHR_get_physical_device_properties2` | Dynamic capability querying | **Required (Mandatory)** |
| `VK_EXT_custom_border_color` | Accurate texture border sampling | **Optional (Graceful Fallback)** |
| `VK_KHR_shader_float16_int8` | Fast arithmetic on ARM mobile GPUs | **Optional (High Perf Boost)** |
| `VK_EXT_robustness2` | Out-of-bounds shader safety | **Optional (Graceful Fallback)** |
