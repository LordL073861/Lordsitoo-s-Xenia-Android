package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.data.GameRepository
import com.example.core.hardware.HardwareDiagnosticEngine
import com.example.core.input.ControllerManager
import com.example.core.model.*
import com.example.core.xenia.XeniaNativeBridge
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val repository = GameRepository(application)
    val controllerManager = ControllerManager(application)
    val nativeBridge = XeniaNativeBridge(application)

    val allGames: StateFlow<List<GameItem>> = repository.allGames
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteGames: StateFlow<List<GameItem>> = repository.favoriteGames
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentGames: StateFlow<List<GameItem>> = repository.recentGames
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _scanProgress = MutableStateFlow(GameRepository.ScanProgress())
    val scanProgress: StateFlow<GameRepository.ScanProgress> = _scanProgress.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(LibraryFilter.ALL)
    val selectedFilter: StateFlow<LibraryFilter> = _selectedFilter.asStateFlow()

    private val _selectedGameForDetails = MutableStateFlow<GameItem?>(null)
    val selectedGameForDetails: StateFlow<GameItem?> = _selectedGameForDetails.asStateFlow()

    private val _globalSettings = MutableStateFlow(GameSettings())
    val globalSettings: StateFlow<GameSettings> = _globalSettings.asStateFlow()

    private val _hardwareReport = MutableStateFlow(HardwareDiagnosticEngine.inspectDevice(application))
    val hardwareReport: StateFlow<HardwareReport> = _hardwareReport.asStateFlow()

    val emulationState: StateFlow<EmulationState> = nativeBridge.emulationState
    val logEntries: StateFlow<List<DiagnosticLogEntry>> = nativeBridge.logEntries

    enum class LibraryFilter(val label: String) {
        ALL("All Games"),
        FAVORITES("Favorites"),
        RECENT("Recent"),
        PLAYABLE("Playable"),
        INGAME("In-Game")
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: LibraryFilter) {
        _selectedFilter.value = filter
    }

    fun selectGameForDetails(game: GameItem?) {
        _selectedGameForDetails.value = game
    }

    fun scanFolder(treeUri: Uri) {
        viewModelScope.launch {
            repository.scanDirectory(treeUri) { progress ->
                _scanProgress.value = progress
            }
        }
    }

    fun toggleFavorite(game: GameItem) {
        viewModelScope.launch {
            repository.toggleFavorite(game)
        }
    }

    fun updateGameSettings(game: GameItem, newSettings: GameSettings) {
        viewModelScope.launch {
            val updated = game.copy(settings = newSettings)
            repository.updateGameSettings(updated)
            if (_selectedGameForDetails.value?.id == game.id) {
                _selectedGameForDetails.value = updated
            }
        }
    }

    fun updateCustomCover(game: GameItem, inputStream: java.io.InputStream) {
        viewModelScope.launch {
            repository.updateCustomCover(game, inputStream)
        }
    }

    fun rescanMetadata(game: GameItem) {
        viewModelScope.launch {
            repository.refreshMetadata(game)
        }
    }

    fun removeGame(game: GameItem) {
        viewModelScope.launch {
            repository.removeGameFromLibrary(game)
            if (_selectedGameForDetails.value?.id == game.id) {
                _selectedGameForDetails.value = null
            }
        }
    }

    fun launchGame(game: GameItem) {
        viewModelScope.launch {
            repository.recordGamePlay(game)
            nativeBridge.launchGame(game, null)
        }
    }

    fun pauseEmulation() = nativeBridge.pauseEmulation()
    fun resumeEmulation() = nativeBridge.resumeEmulation()
    fun stopEmulation() = nativeBridge.stopEmulation()

    fun updateGlobalSettings(settings: GameSettings) {
        _globalSettings.value = settings
    }

    fun refreshHardwareReport() {
        _hardwareReport.value = HardwareDiagnosticEngine.inspectDevice(getApplication())
    }
}
