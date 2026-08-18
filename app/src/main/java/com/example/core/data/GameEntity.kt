package com.example.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.core.model.CompatibilityLevel
import com.example.core.model.GameItem
import com.example.core.model.GameSettings

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val titleId: String,
    val mediaId: String = "",
    val titleName: String,
    val discNumber: Int = 1,
    val discCount: Int = 1,
    val region: String = "Region Free",
    val fileUri: String,
    val filePath: String,
    val fileFormat: String,
    val fileSizeFormatted: String = "",
    val coverUrl: String? = null,
    val localCoverPath: String? = null,
    val customCoverPath: String? = null,
    val playTimeMinutes: Long = 0,
    val lastPlayedTimestamp: Long = 0,
    val isFavorite: Boolean = false,
    val compatibility: String = "UNTESTED",
    val userNotes: String = "",
    val resolutionScale: Float = 1.0f,
    val vsync: Boolean = true,
    val shaderCache: Boolean = true
) {
    fun toDomain(): GameItem {
        return GameItem(
            id = id,
            titleId = titleId,
            mediaId = mediaId,
            titleName = titleName,
            discNumber = discNumber,
            discCount = discCount,
            region = region,
            fileUri = fileUri,
            filePath = filePath,
            fileFormat = fileFormat,
            fileSizeFormatted = fileSizeFormatted,
            coverUrl = coverUrl,
            localCoverPath = localCoverPath,
            customCoverPath = customCoverPath,
            playTimeMinutes = playTimeMinutes,
            lastPlayedTimestamp = lastPlayedTimestamp,
            isFavorite = isFavorite,
            compatibility = runCatching { CompatibilityLevel.valueOf(compatibility) }.getOrDefault(CompatibilityLevel.UNTESTED),
            userNotes = userNotes,
            settings = GameSettings(
                resolutionScale = resolutionScale,
                vsync = vsync,
                shaderCache = shaderCache
            )
        )
    }

    companion object {
        fun fromDomain(item: GameItem): GameEntity {
            return GameEntity(
                id = item.id,
                titleId = item.titleId,
                mediaId = item.mediaId,
                titleName = item.titleName,
                discNumber = item.discNumber,
                discCount = item.discCount,
                region = item.region,
                fileUri = item.fileUri,
                filePath = item.filePath,
                fileFormat = item.fileFormat,
                fileSizeFormatted = item.fileSizeFormatted,
                coverUrl = item.coverUrl,
                localCoverPath = item.localCoverPath,
                customCoverPath = item.customCoverPath,
                playTimeMinutes = item.playTimeMinutes,
                lastPlayedTimestamp = item.lastPlayedTimestamp,
                isFavorite = item.isFavorite,
                compatibility = item.compatibility.name,
                userNotes = item.userNotes,
                resolutionScale = item.settings.resolutionScale,
                vsync = item.settings.vsync,
                shaderCache = item.settings.shaderCache
            )
        }
    }
}
