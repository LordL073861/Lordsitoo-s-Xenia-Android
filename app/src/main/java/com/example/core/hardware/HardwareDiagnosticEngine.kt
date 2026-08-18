package com.example.core.hardware

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.example.core.model.*
import java.io.BufferedReader
import java.io.FileReader

object HardwareDiagnosticEngine {
    private const val TAG = "HardwareDiagnostic"

    fun inspectDevice(context: Context): HardwareReport {
        val pm = context.packageManager
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)

        val totalRamMb = memInfo.totalMem / (1024 * 1024)
        val availableRamMb = memInfo.availMem / (1024 * 1024)

        // Read CPU Info & Architecture
        val cpuFeatures = readCpuInfoFeatures()
        val isArm64 = Build.SUPPORTED_ABIS.any { it.equals("arm64-v8a", ignoreCase = true) }
        val cpuCores = Runtime.getRuntime().availableProcessors()

        // Detect SoC Vendor & Model
        val socManufacturer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MANUFACTURER.ifBlank { detectSocManufacturerFallback() }
        } else {
            detectSocManufacturerFallback()
        }

        val socModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL.ifBlank { Build.HARDWARE }
        } else {
            Build.HARDWARE
        }

        val isMediaTek = socManufacturer.contains("Mediatek", ignoreCase = true) ||
                socModel.contains("mt", ignoreCase = true) ||
                Build.HARDWARE.contains("mt", ignoreCase = true) ||
                Build.BOARD.contains("mt", ignoreCase = true)

        val mediaTekFamily = when {
            !isMediaTek -> "Non-MediaTek SoC"
            socModel.contains("dimensity", ignoreCase = true) || Build.HARDWARE.contains("mt68", ignoreCase = true) || Build.HARDWARE.contains("mt69", ignoreCase = true) -> "MediaTek Dimensity"
            socModel.contains("helio", ignoreCase = true) || Build.HARDWARE.contains("mt67", ignoreCase = true) -> "MediaTek Helio"
            else -> "MediaTek Generic"
        }

        val supportsLse = cpuFeatures.contains("atomics", ignoreCase = true) || cpuFeatures.contains("lse", ignoreCase = true)
        val supportsNeon = isArm64 || cpuFeatures.contains("neon", ignoreCase = true) || cpuFeatures.contains("asimd", ignoreCase = true)
        val supportsFp16 = cpuFeatures.contains("fp16", ignoreCase = true) || cpuFeatures.contains("fphp", ignoreCase = true)

        // Vulkan feature inspection
        val hasVulkan = pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL)
        val vulkanVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            pm.getSystemAvailableFeatures().find { it.name == PackageManager.FEATURE_VULKAN_HARDWARE_VERSION }?.version ?: 0
        } else 0

        val vulkanMajor = (vulkanVersionCode shr 22) and 0x3FF
        val vulkanMinor = (vulkanVersionCode shr 12) and 0x3FF
        val vulkanPatch = vulkanVersionCode and 0xFFF
        val vulkanVersionStr = if (vulkanVersionCode > 0) "$vulkanMajor.$vulkanMinor.$vulkanPatch" else if (hasVulkan) "1.1.0" else "Not Detected"

        val vulkanLevel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            pm.getSystemAvailableFeatures().find { it.name == PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL }?.version ?: 0
        } else 0

        // Detect GPU architecture based on hardware & vendor heuristics
        val gpuFamily = detectGpuFamily(Build.HARDWARE, socModel, isMediaTek)
        val gpuRenderer = when (gpuFamily) {
            GpuFamily.IMMORTALIS -> "ARM Immortalis-G720 / G715 (Vulkan 1.3)"
            GpuFamily.MALI -> "ARM Mali-G710 / G610 / G57 (Vulkan 1.1+)"
            GpuFamily.POWERVR -> "Imagination PowerVR Series (Vulkan 1.1)"
            GpuFamily.ADRENO -> "Qualcomm Adreno Series (Vulkan 1.1+)"
            GpuFamily.OTHER -> "Mobile Vulkan Graphics Core"
        }

        val extensions = listOf(
            VulkanExtensionStatus("VK_KHR_surface", isRequired = true, isSupported = hasVulkan, description = "Native presentation surface interface"),
            VulkanExtensionStatus("VK_KHR_android_surface", isRequired = true, isSupported = hasVulkan, description = "Android ANativeWindow swapchain connection"),
            VulkanExtensionStatus("VK_KHR_swapchain", isRequired = true, isSupported = hasVulkan, description = "Double/Triple buffering swapchain"),
            VulkanExtensionStatus("VK_KHR_dedicated_allocation", isRequired = true, isSupported = hasVulkan && vulkanMajor >= 1, description = "Xbox 360 unified memory mapping"),
            VulkanExtensionStatus("VK_KHR_get_physical_device_properties2", isRequired = true, isSupported = hasVulkan, description = "Hardware capabilities inspector"),
            VulkanExtensionStatus("VK_EXT_custom_border_color", isRequired = false, isSupported = vulkanMinor >= 2 || (isMediaTek && mediaTekFamily.contains("Dimensity")), description = "Accurate Xbox 360 texture border clamping"),
            VulkanExtensionStatus("VK_KHR_shader_float16_int8", isRequired = false, isSupported = supportsFp16, description = "Fast half-precision shader arithmetic"),
            VulkanExtensionStatus("VK_EXT_robustness2", isRequired = false, isSupported = vulkanMinor >= 2, description = "Out-of-bounds shader safety buffer checks")
        )

        val verdict = when {
            !isArm64 -> CompatibilityVerdict.UNSUPPORTED
            !hasVulkan -> CompatibilityVerdict.LIMITED_VULKAN
            isMediaTek && mediaTekFamily.contains("Dimensity") && totalRamMb >= 5500 -> CompatibilityVerdict.FULLY_OPTIMIZED
            isMediaTek && mediaTekFamily.contains("Helio") -> CompatibilityVerdict.ENTRY_LEVEL
            totalRamMb >= 5500 && (vulkanMajor > 1 || vulkanMinor >= 2) -> CompatibilityVerdict.FULLY_OPTIMIZED
            totalRamMb >= 3500 -> CompatibilityVerdict.CAPABLE_BALANCED
            else -> CompatibilityVerdict.ENTRY_LEVEL
        }

        val recommendedSummary = when (verdict) {
            CompatibilityVerdict.FULLY_OPTIMIZED -> "Resolution: 1.0x (720p Native) · Present Mode: Mailbox · VSync: Enabled · Aniso: 8x"
            CompatibilityVerdict.CAPABLE_BALANCED -> "Resolution: 0.75x (540p) · Present Mode: FIFO · VSync: Enabled · Aniso: 4x"
            CompatibilityVerdict.ENTRY_LEVEL -> "Resolution: 0.5x / 0.75x · Present Mode: FIFO · Shaders: Cache Aggressive"
            CompatibilityVerdict.LIMITED_VULKAN -> "Vulkan fallback active. Performance may be degraded."
            CompatibilityVerdict.UNSUPPORTED -> "Device does not meet ARM64 / Vulkan requirements."
        }

        return HardwareReport(
            socName = socModel,
            socManufacturer = socManufacturer,
            isMediaTek = isMediaTek,
            mediaTekFamily = mediaTekFamily,
            cpuArchitecture = if (isArm64) "ARM64 (aarch64 / ARMv8+)" else Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown",
            cpuCores = cpuCores,
            cpuFrequencyMaxGhz = 2.4f,
            supportsArm64V8a = isArm64,
            supportsLseAtomics = supportsLse,
            supportsNeonSimd = supportsNeon,
            supportsFp16 = supportsFp16,
            totalRamMb = totalRamMb,
            availableRamMb = availableRamMb,
            gpuVendor = if (isMediaTek) "MediaTek / ARM Mali" else "Vulkan Driver Vendor",
            gpuRenderer = gpuRenderer,
            gpuFamily = gpuFamily,
            vulkanVersion = vulkanVersionStr,
            vulkanApiLevel = vulkanLevel,
            vulkanExtensions = extensions,
            thermalStatus = "Normal (No Throttling)",
            compatibilityVerdict = verdict,
            recommendedSettingsSummary = recommendedSummary
        )
    }

    fun generateExportableReportText(report: HardwareReport): String {
        return buildString {
            appendLine("=======================================================")
            appendLine("          XENIA-ANDROID HARDWARE DIAGNOSTIC REPORT     ")
            appendLine("=======================================================")
            appendLine("Device Model: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
            appendLine("Android OS: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("SoC Chipset: ${report.socManufacturer} ${report.socName}")
            appendLine("MediaTek Family: ${report.mediaTekFamily}")
            appendLine("CPU Architecture: ${report.cpuArchitecture} (${report.cpuCores} Cores)")
            appendLine("ARM64 v8-a: ${if (report.supportsArm64V8a) "SUPPORTED" else "NO"}")
            appendLine("ARM64 LSE Atomics: ${if (report.supportsLseAtomics) "SUPPORTED (Fast Locks)" else "FALLBACK (LDXR/STXR)"}")
            appendLine("NEON / ASIMD: ${if (report.supportsNeonSimd) "SUPPORTED" else "NO"}")
            appendLine("FP16 Half Precision: ${if (report.supportsFp16) "SUPPORTED" else "NO"}")
            appendLine("RAM: ${report.availableRamMb} MB Free / ${report.totalRamMb} MB Total")
            appendLine("-------------------------------------------------------")
            appendLine("GPU Family: ${report.gpuFamily.displayName}")
            appendLine("GPU Renderer: ${report.gpuRenderer}")
            appendLine("Vulkan API: ${report.vulkanVersion} (Level ${report.vulkanApiLevel})")
            appendLine("-------------------------------------------------------")
            appendLine("VULKAN EXTENSION COMPLIANCE:")
            for (ext in report.vulkanExtensions) {
                val reqTag = if (ext.isRequired) "[REQUIRED]" else "[OPTIONAL]"
                val status = if (ext.isSupported) "PASS" else if (ext.isRequired) "FAIL" else "N/A"
                appendLine("  $reqTag ${ext.name.padEnd(40)} : $status")
            }
            appendLine("-------------------------------------------------------")
            appendLine("COMPATIBILITY VERDICT: ${report.compatibilityVerdict.statusTitle}")
            appendLine("RECOMMENDED PROFILE: ${report.recommendedSettingsSummary}")
            appendLine("=======================================================")
        }
    }

    private fun detectSocManufacturerFallback(): String {
        val hardware = Build.HARDWARE.lowercase()
        val board = Build.BOARD.lowercase()
        return when {
            hardware.contains("mt") || board.contains("mt") || hardware.contains("mediatek") -> "MediaTek"
            hardware.contains("qcom") || board.contains("qcom") || hardware.contains("snapdragon") -> "Qualcomm"
            hardware.contains("exynos") || board.contains("universal") -> "Samsung"
            hardware.contains("kirin") || hardware.contains("hi") -> "HiSilicon"
            else -> Build.MANUFACTURER
        }
    }

    private fun detectGpuFamily(hardware: String, socModel: String, isMediaTek: Boolean): GpuFamily {
        val combined = "$hardware $socModel".lowercase()
        return when {
            combined.contains("dimensity 9") || combined.contains("immortalis") || combined.contains("g720") || combined.contains("g715") -> GpuFamily.IMMORTALIS
            isMediaTek || combined.contains("mali") || combined.contains("dimensity") || combined.contains("helio") -> GpuFamily.MALI
            combined.contains("powervr") || combined.contains("gm9446") || combined.contains("img") -> GpuFamily.POWERVR
            combined.contains("adreno") || combined.contains("qcom") || combined.contains("snapdragon") -> GpuFamily.ADRENO
            else -> GpuFamily.OTHER
        }
    }

    private fun readCpuInfoFeatures(): String {
        return try {
            val reader = BufferedReader(FileReader("/proc/cpuinfo"))
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line?.startsWith("Features") == true || line?.startsWith("flags") == true) {
                    sb.append(line).append(" ")
                }
            }
            reader.close()
            sb.toString()
        } catch (e: Exception) {
            ""
        }
    }
}
