package com.example.core.model

sealed class EmulationState {
    object Idle : EmulationState()
    data class Initializing(val step: String) : EmulationState()
    data class CompilingShaders(val current: Int, val total: Int) : EmulationState()
    data class Running(
        val game: GameItem,
        val fps: Float = 0f,
        val frameTimeMs: Float = 0f,
        val guestCpuUsage: Float = 0f,
        val hostRamUsageMb: Long = 0,
        val activeResolution: String = "1280x720 (Native 720p)"
    ) : EmulationState()
    data class Paused(val game: GameItem) : EmulationState()
    data class Error(val title: String, val message: String, val technicalLog: String = "") : EmulationState()
    object Stopped : EmulationState()
}

data class DiagnosticLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String,
    val level: LogLevel,
    val message: String
)

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}
