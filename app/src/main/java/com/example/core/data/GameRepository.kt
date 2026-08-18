package com.example.core.data

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.example.core.model.CompatibilityLevel
import com.example.core.model.GameItem
import com.example.core.parser.XboxMetadataExtractor
import com.example.core.scraper.CoverMetadataProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class GameRepository(private val context: Context) {
    private val TAG = "GameRepository"
    private val database = XeniaAppDatabase.getInstance(context)
    private val gameDao = database.gameDao()
    private val coverProvider = CoverMetadataProvider(context)

    val allGames: Flow<List<GameItem>> = gameDao.getAllGames().map { list -> list.map { it.toDomain() } }
    val favoriteGames: Flow<List<GameItem>> = gameDao.getFavoriteGames().map { list -> list.map { it.toDomain() } }
    val recentGames: Flow<List<GameItem>> = gameDao.getRecentGames().map { list -> list.map { it.toDomain() } }

    data class ScanProgress(
        val isScanning: Boolean = false,
        val currentFileName: String = "",
        val scannedCount: Int = 0,
        val totalCount: Int = 0,
        val lastImportedTitle: String = ""
    )

    suspend fun scanDirectory(
        treeUri: Uri,
        onProgress: (ScanProgress) -> Unit = {}
    ): Int = withContext(Dispatchers.IO) {
        var importedCount = 0
        try {
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext 0
            val supportedFiles = mutableListOf<DocumentFile>()

            fun collectFiles(dir: DocumentFile) {
                val files = dir.listFiles()
                for (file in files) {
                    if (file.isDirectory) {
                        collectFiles(file)
                    } else {
                        val name = file.name ?: ""
                        val lower = name.lowercase()
                        if (lower.endsWith(".iso") || lower.endsWith(".xex") ||
                            lower.endsWith(".stfs") || lower.endsWith(".god") ||
                            lower.endsWith(".bin")
                        ) {
                            supportedFiles.add(file)
                        }
                    }
                }
            }

            collectFiles(rootDoc)
            val total = supportedFiles.size

            onProgress(ScanProgress(isScanning = true, totalCount = total))

            for ((index, docFile) in supportedFiles.withIndex()) {
                val name = docFile.name ?: "Unknown"
                val uri = docFile.uri
                val sizeBytes = docFile.length()
                val sizeFormatted = formatFileSize(sizeBytes)

                onProgress(
                    ScanProgress(
                        isScanning = true,
                        currentFileName = name,
                        scannedCount = index + 1,
                        totalCount = total
                    )
                )

                val parsed = XboxMetadataExtractor.extract(context, uri, name)

                // Check if already in database
                val existing = gameDao.getGameByUri(uri.toString())
                val localCover = coverProvider.getOrFetchCover(parsed.titleId, parsed.titleName)

                val entity = GameEntity(
                    id = existing?.id ?: 0L,
                    titleId = parsed.titleId,
                    mediaId = parsed.mediaId,
                    titleName = parsed.titleName,
                    discNumber = parsed.discNumber,
                    discCount = parsed.discCount,
                    region = parsed.region,
                    fileUri = uri.toString(),
                    filePath = uri.path ?: name,
                    fileFormat = parsed.fileFormat,
                    fileSizeFormatted = sizeFormatted,
                    localCoverPath = localCover ?: existing?.localCoverPath,
                    isFavorite = existing?.isFavorite ?: false,
                    playTimeMinutes = existing?.playTimeMinutes ?: 0L,
                    lastPlayedTimestamp = existing?.lastPlayedTimestamp ?: 0L,
                    compatibility = determineInitialCompatibility(parsed.titleId).name
                )

                gameDao.insertGame(entity)
                importedCount++

                onProgress(
                    ScanProgress(
                        isScanning = true,
                        currentFileName = name,
                        scannedCount = index + 1,
                        totalCount = total,
                        lastImportedTitle = parsed.titleName
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning directory: ${e.message}", e)
        } finally {
            onProgress(ScanProgress(isScanning = false))
        }

        return@withContext importedCount
    }

    suspend fun toggleFavorite(game: GameItem) = withContext(Dispatchers.IO) {
        val updated = game.copy(isFavorite = !game.isFavorite)
        gameDao.updateGame(GameEntity.fromDomain(updated))
    }

    suspend fun recordGamePlay(game: GameItem, sessionMinutes: Long = 1) = withContext(Dispatchers.IO) {
        val updated = game.copy(
            lastPlayedTimestamp = System.currentTimeMillis(),
            playTimeMinutes = game.playTimeMinutes + sessionMinutes
        )
        gameDao.updateGame(GameEntity.fromDomain(updated))
    }

    suspend fun updateGameSettings(game: GameItem) = withContext(Dispatchers.IO) {
        gameDao.updateGame(GameEntity.fromDomain(game))
    }

    suspend fun updateCustomCover(game: GameItem, inputStream: java.io.InputStream) = withContext(Dispatchers.IO) {
        val path = coverProvider.saveCustomCover(game.titleId, inputStream)
        if (path != null) {
            val updated = game.copy(customCoverPath = path)
            gameDao.updateGame(GameEntity.fromDomain(updated))
        }
    }

    suspend fun refreshMetadata(game: GameItem) = withContext(Dispatchers.IO) {
        val localCover = coverProvider.getOrFetchCover(game.titleId, game.titleName)
        if (localCover != null) {
            val updated = game.copy(localCoverPath = localCover)
            gameDao.updateGame(GameEntity.fromDomain(updated))
        }
    }

    suspend fun removeGameFromLibrary(game: GameItem) = withContext(Dispatchers.IO) {
        gameDao.deleteGame(GameEntity.fromDomain(game))
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 MB"
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        return if (gb >= 1.0) {
            String.format("%.2f GB", gb)
        } else {
            val mb = bytes / (1024.0 * 1024.0)
            String.format("%.1f MB", mb)
        }
    }

    private fun determineInitialCompatibility(titleId: String): CompatibilityLevel {
        // Well-known Xbox 360 title compatibility baseline from Xenia Canary verification matrix
        return when (titleId.uppercase()) {
            "4D5307E6", "584109C2", "58410A5A", "4D53085B", "4541080F" -> CompatibilityLevel.PLAYABLE
            "545407F2", "415607E7", "4D53082D", "4D5307D5" -> CompatibilityLevel.INGAME
            "4D53084D", "534507DE" -> CompatibilityLevel.LOADS
            else -> CompatibilityLevel.UNTESTED
        }
    }
}
