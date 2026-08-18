package com.example.core.model

data class HardwareReport(
    val socName: String,
    val socManufacturer: String,
    val isMediaTek: Boolean,
    val mediaTekFamily: String, // Dimensity, Helio, or Other
    val cpuArchitecture: String,
    val cpuCores: Int,
    val cpuFrequencyMaxGhz: Float,
    val supportsArm64V8a: Boolean,
    val supportsLseAtomics: Boolean,
    val supportsNeonSimd: Boolean,
    val supportsFp16: Boolean,
    val totalRamMb: Long,
    val availableRamMb: Long,
    val gpuVendor: String,
    val gpuRenderer: String,
    val gpuFamily: GpuFamily,
    val vulkanVersion: String,
    val vulkanApiLevel: Int,
    val vulkanExtensions: List<VulkanExtensionStatus>,
    val thermalStatus: String,
    val compatibilityVerdict: CompatibilityVerdict,
    val recommendedSettingsSummary: String
)

enum class GpuFamily(val displayName: String) {
    MALI("ARM Mali"),
    IMMORTALIS("ARM Immortalis"),
    POWERVR("Imagination PowerVR"),
    ADRENO("Qualcomm Adreno"),
    OTHER("Standard Mobile GPU")
}

data class VulkanExtensionStatus(
    val name: String,
    val isRequired: Boolean,
    val isSupported: Boolean,
    val description: String
)

enum class CompatibilityVerdict(val statusTitle: String, val isCompatible: Boolean) {
    FULLY_OPTIMIZED("Fully Compatible (Tier 1 Flagship)", true),
    CAPABLE_BALANCED("Compatible (Tier 2 Standard)", true),
    ENTRY_LEVEL("Supported (Tier 3 Light 360 Titles)", true),
    LIMITED_VULKAN("Restricted (Vulkan Features Limited)", false),
    UNSUPPORTED("Hardware Requirements Not Met", false)
}
