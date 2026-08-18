package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.data.GameEntity
import com.example.core.data.XeniaAppDatabase
import com.example.core.hardware.HardwareDiagnosticEngine
import com.example.core.model.CompatibilityLevel
import com.example.core.parser.XboxMetadataExtractor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var database: XeniaAppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, XeniaAppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun verifyAppName() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Xenia-Android", appName)
    }

    @Test
    fun verifyMetadataExtractionHeuristics() {
        val clean = XboxMetadataExtractor.cleanGameTitleFromFilename("Halo 3 [USA] (Disc 1).iso")
        assertEquals("Halo 3", clean)

        val clean2 = XboxMetadataExtractor.cleanGameTitleFromFilename("Red_Dead_Redemption_XBLA.xex")
        assertEquals("Red Dead Redemption XBLA", clean2)

        val titleId = XboxMetadataExtractor.generateDeterministicTitleId("Gears of War 2")
        assertTrue(titleId.startsWith("4D53"))
    }

    @Test
    fun verifyHardwareDiagnosticReport() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val report = HardwareDiagnosticEngine.inspectDevice(context)
        assertNotNull(report)
        assertNotNull(report.socName)
        assertNotNull(report.vulkanVersion)
        assertTrue(report.vulkanExtensions.isNotEmpty())

        val reportText = HardwareDiagnosticEngine.generateExportableReportText(report)
        assertTrue(reportText.contains("XENIA-ANDROID HARDWARE DIAGNOSTIC REPORT"))
    }

    @Test
    fun verifyRoomGamePersistence() = runBlocking {
        val dao = database.gameDao()
        val sampleGame = GameEntity(
            titleId = "4D5307E6",
            mediaId = "10284451",
            titleName = "Halo 3",
            discNumber = 1,
            discCount = 1,
            region = "NTSC/U",
            fileUri = "content://com.android.providers.media.documents/document/1234",
            filePath = "/storage/emulated/0/ROMs/Xbox360/Halo3.iso",
            fileFormat = "ISO",
            fileSizeFormatted = "7.3 GB",
            isFavorite = true,
            compatibility = CompatibilityLevel.PLAYABLE.name
        )

        dao.insertGame(sampleGame)
        val games = dao.getAllGames().first()
        assertEquals(1, games.size)
        assertEquals("Halo 3", games[0].titleName)
        assertEquals("4D5307E6", games[0].titleId)
        assertTrue(games[0].isFavorite)

        val favorites = dao.getFavoriteGames().first()
        assertEquals(1, favorites.size)
    }
}
