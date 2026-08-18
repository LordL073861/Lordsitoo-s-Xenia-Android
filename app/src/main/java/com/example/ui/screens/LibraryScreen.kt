package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core.model.CompatibilityLevel
import com.example.core.model.GameItem
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    onGameSelectedForPlay: (GameItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val allGames by viewModel.allGames.collectAsState()
    val favoriteGames by viewModel.favoriteGames.collectAsState()
    val recentGames by viewModel.recentGames.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val selectedGameForDetails by viewModel.selectedGameForDetails.collectAsState()

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.scanFolder(uri)
        }
    }

    // Filter games based on selection and search
    val displayedGames = remember(allGames, favoriteGames, recentGames, selectedFilter, searchQuery) {
        val baseList = when (selectedFilter) {
            MainViewModel.LibraryFilter.ALL -> allGames
            MainViewModel.LibraryFilter.FAVORITES -> favoriteGames
            MainViewModel.LibraryFilter.RECENT -> recentGames
            MainViewModel.LibraryFilter.PLAYABLE -> allGames.filter { it.compatibility == CompatibilityLevel.PLAYABLE }
            MainViewModel.LibraryFilter.INGAME -> allGames.filter { it.compatibility == CompatibilityLevel.INGAME }
        }
        if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter {
                it.titleName.contains(searchQuery, ignoreCase = true) ||
                        it.titleId.contains(searchQuery, ignoreCase = true) ||
                        it.fileFormat.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { folderPickerLauncher.launch(null) },
                icon = { Icon(Icons.Filled.CreateNewFolder, contentDescription = "Add Games Folder", tint = Color(0xFF00391A)) },
                text = { Text("Add Games Folder", color = Color(0xFF00391A), fontWeight = FontWeight.Bold) },
                containerColor = XeniaGreen,
                modifier = Modifier.testTag("add_folder_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Search & Filter Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search Xbox 360 library or Title ID...", color = TechTextMuted, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = XeniaGreen) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = TechTextSecondary)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = XeniaGreen,
                        unfocusedBorderColor = TechBorder,
                        focusedTextColor = TechTextPrimary,
                        unfocusedTextColor = TechTextPrimary,
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_bar")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Chips Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MainViewModel.LibraryFilter.values().forEach { filter ->
                        val isSelected = selectedFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setFilter(filter) },
                            label = { Text(filter.label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = XeniaGreenContainer,
                                selectedLabelColor = OnXeniaGreenContainer,
                                containerColor = DarkSurfaceVariant,
                                labelColor = TechTextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = TechBorder,
                                selectedBorderColor = XeniaGreen
                            )
                        )
                    }
                }
            }

            // Scanning Progress Banner
            AnimatedVisibility(visible = scanProgress.isScanning) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(XeniaGreen, TechCyan)))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = XeniaGreen
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Scanning directory (${scanProgress.scannedCount}/${scanProgress.totalCount})...",
                                color = TechTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (scanProgress.currentFileName.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                scanProgress.currentFileName,
                                color = TechTextMuted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Games Grid / Empty State
            if (displayedGames.isEmpty() && !scanProgress.isScanning) {
                EmptyLibraryState(
                    onAddFolderClick = { folderPickerLauncher.launch(null) },
                    hasSearch = searchQuery.isNotBlank()
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("games_grid")
                ) {
                    items(displayedGames, key = { it.id }) { game ->
                        GameCard(
                            game = game,
                            onCardClick = { viewModel.selectGameForDetails(game) },
                            onPlayClick = { onGameSelectedForPlay(game) },
                            onToggleFavorite = { viewModel.toggleFavorite(game) }
                        )
                    }
                }
            }
        }

        // Game Detail Bottom Sheet
        if (selectedGameForDetails != null) {
            GameDetailSheet(
                game = selectedGameForDetails!!,
                onDismiss = { viewModel.selectGameForDetails(null) },
                onPlay = {
                    val game = selectedGameForDetails!!
                    viewModel.selectGameForDetails(null)
                    onGameSelectedForPlay(game)
                },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onRescan = { viewModel.rescanMetadata(it) },
                onRemove = { viewModel.removeGame(it) },
                onSaveSettings = { game, newSettings -> viewModel.updateGameSettings(game, newSettings) }
            )
        }
    }
}

@Composable
fun GameCard(
    game: GameItem,
    onCardClick: () -> Unit,
    onPlayClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(TechBorder, Color(0xFF1B222D))))
    ) {
        Column {
            // Box Art Cover Frame (Xbox 360 3:4 Aspect Ratio)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
                    .background(Color(0xFF07090C))
            ) {
                val coverSource = game.customCoverPath ?: game.localCoverPath ?: game.coverUrl
                if (coverSource != null) {
                    val context = LocalContext.current
                    val imageModel = remember(coverSource) {
                        ImageRequest.Builder(context)
                            .data(if (coverSource.startsWith("/")) File(coverSource) else coverSource)
                            .crossfade(true)
                            .build()
                    }
                    AsyncImage(
                        model = imageModel,
                        contentDescription = "Cover for ${game.titleName}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Fallback Technological Xbox Spine & Initial Art
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF182218), Color(0xFF0B0E14), Color(0xFF0F151B))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(XeniaGreenContainer, CircleShape)
                                    .border(1.dp, XeniaGreen, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    game.titleName.take(2).uppercase(),
                                    color = XeniaGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                game.titleName,
                                color = TechTextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Top Xbox 360 Header Ribbon
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xE60A3816), Color(0x99000000))
                            )
                        )
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(XeniaGreen, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "XBOX 360 · ${game.fileFormat}",
                            color = Color(0xFFD0F8CE),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Favorite Toggle Button
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(32.dp)
                        .background(Color(0x80000000), CircleShape)
                ) {
                    Icon(
                        if (game.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (game.isFavorite) ErrorRed else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Quick Play Button Floating on Cover
                IconButton(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(36.dp)
                        .background(XeniaGreen, CircleShape)
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = "Play",
                        tint = Color(0xFF00391A),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Game Metadata Card Footer
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    game.titleName,
                    color = TechTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        game.titleId,
                        color = TechTextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    CompatibilityBadge(game.compatibility)
                }
            }
        }
    }
}

@Composable
fun CompatibilityBadge(level: CompatibilityLevel) {
    val (bg, fg) = when (level) {
        CompatibilityLevel.PLAYABLE -> XeniaGreenContainer to OnXeniaGreenContainer
        CompatibilityLevel.INGAME -> Color(0xFF2E3812) to Color(0xFFE2F05D)
        CompatibilityLevel.LOADS -> Color(0xFF382B12) to WarningAmber
        CompatibilityLevel.UNTESTED -> DarkSurfaceContainerHighest to TechTextSecondary
        CompatibilityLevel.BROKEN -> Color(0xFF3E1212) to ErrorRed
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            level.displayName,
            color = fg,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun EmptyLibraryState(
    onAddFolderClick: () -> Unit,
    hasSearch: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(DarkSurfaceContainer, CircleShape)
                    .border(1.dp, TechBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (hasSearch) Icons.Filled.SearchOff else Icons.Filled.FolderOpen,
                    contentDescription = null,
                    tint = XeniaGreen,
                    modifier = Modifier.size(38.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                if (hasSearch) "No Games Found" else "Xbox 360 Library Empty",
                color = TechTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                if (hasSearch) "Try searching for a different game title or Title ID."
                else "Point Xenia-Android to your directory containing Xbox 360 .iso, .xex, or .stfs files.",
                color = TechTextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            if (!hasSearch) {
                Button(
                    onClick = onAddFolderClick,
                    colors = ButtonDefaults.buttonColors(containerColor = XeniaGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.CreateNewFolder, contentDescription = null, tint = Color(0xFF00391A))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Games Folder", color = Color(0xFF00391A), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
