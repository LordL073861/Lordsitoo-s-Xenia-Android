package com.example.core.model

data class GameSettings(
    val resolutionScale: Float = 1.0f, // 0.5x, 0.75x, 1.0x, 1.25x, 1.5x, 2.0x
    val vsync: Boolean = true,
    val presentMode: PresentMode = PresentMode.MAILBOX,
    val shaderCache: Boolean = true,
    val textureCache: Boolean = true,
    val anisotropicFiltering: Int = 4, // 1x, 2x, 4x, 8x, 16x
    val cpuThreads: Int = 6, // Xenon 6 HW threads
    val arm64LseAtomics: Boolean = true,
    val audioLatencyMode: AudioLatency = AudioLatency.LOW_LATENCY,
    val audioBufferSize: Int = 256,
    val customControllerProfileId: String = "default",
    val keepScreenOn: Boolean = true
)

enum class PresentMode(val displayName: String) {
    MAILBOX("Mailbox (Low Latency / Uncapped)"),
    FIFO("FIFO (Strict VSync 60Hz)"),
    IMMEDIATE("Immediate (Tearing Allowed)")
}

enum class AudioLatency(val displayName: String) {
    LOW_LATENCY("Low Latency (AAudio)"),
    BALANCED("Balanced (256 samples)"),
    HIGH_STABILITY("High Stability (512 samples)")
}
