package com.example.core.model

data class GameItem(
    val id: Long = 0,
    val titleId: String,
    val mediaId: String = "",
    val titleName: String,
    val discNumber: Int = 1,
    val discCount: Int = 1,
    val region: String = "Region Free",
    val fileUri: String,
    val filePath: String,
    val fileFormat: String, // ISO, XEX, STFS
    val fileSizeFormatted: String = "",
    val coverUrl: String? = null,
    val localCoverPath: String? = null,
    val customCoverPath: String? = null,
    val playTimeMinutes: Long = 0,
    val lastPlayedTimestamp: Long = 0,
    val isFavorite: Boolean = false,
    val compatibility: CompatibilityLevel = CompatibilityLevel.UNTESTED,
    val userNotes: String = "",
    val settings: GameSettings = GameSettings()
)

enum class CompatibilityLevel(val displayName: String) {
    PLAYABLE("Playable"),
    INGAME("In-Game"),
    LOADS("Loads"),
    UNTESTED("Untested"),
    BROKEN("Issues Detected")
}
