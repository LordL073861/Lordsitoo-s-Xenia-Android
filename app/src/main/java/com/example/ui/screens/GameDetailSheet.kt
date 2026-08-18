package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core.model.GameItem
import com.example.core.model.GameSettings
import com.example.core.model.PresentMode
import com.example.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailSheet(
    game: GameItem,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onToggleFavorite: (GameItem) -> Unit,
    onRescan: (GameItem) -> Unit,
    onRemove: (GameItem) -> Unit,
    onSaveSettings: (GameItem, GameSettings) -> Unit
) {
    val context = LocalContext.current
    var currentSettings by remember(game) { mutableStateOf(game.settings) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                // Custom cover handling in repository
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        scrimColor = Color(0x99000000),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = TechBorder)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Top Section: Box Art & Title Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Cover Thumbnail Frame
                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .aspectRatio(0.72f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black)
                        .border(1.dp, TechBorder, RoundedCornerShape(8.dp))
                ) {
                    val coverSource = game.customCoverPath ?: game.localCoverPath ?: game.coverUrl
                    if (coverSource != null) {
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
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(DarkSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                game.titleName.take(2).uppercase(),
                                color = XeniaGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        game.titleName,
                        color = TechTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Title ID: ${game.titleId}",
                        color = XeniaGreen,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (game.mediaId.isNotBlank()) {
                        Text(
                            "Media ID: ${game.mediaId}",
                            color = TechTextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CompatibilityBadge(game.compatibility)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(DarkSurfaceContainerHighest)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(game.fileFormat, color = TechTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Launch & Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(containerColor = XeniaGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("launch_game_button")
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color(0xFF00391A))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PLAY EMULATION", color = Color(0xFF00391A), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                IconButton(
                    onClick = { onToggleFavorite(game) },
                    modifier = Modifier
                        .size(48.dp)
                        .background(DarkSurfaceVariant, RoundedCornerShape(10.dp))
                        .border(1.dp, TechBorder, RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        if (game.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (game.isFavorite) ErrorRed else TechTextSecondary
                    )
                }

                IconButton(
                    onClick = { onRescan(game) },
                    modifier = Modifier
                        .size(48.dp)
                        .background(DarkSurfaceVariant, RoundedCornerShape(10.dp))
                        .border(1.dp, TechBorder, RoundedCornerShape(10.dp))
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Rescan Metadata", tint = TechTextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Metadata Statistics Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(TechBorder, Color(0xFF1B222D))))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow(label = "File Path", value = game.filePath)
                    DetailRow(label = "File Size", value = game.fileSizeFormatted.ifBlank { "Unknown" })
                    DetailRow(label = "Disc Info", value = "Disc ${game.discNumber} of ${game.discCount}")
                    DetailRow(label = "Region", value = game.region)
                    DetailRow(label = "Play Time", value = "${game.playTimeMinutes} min")
                    DetailRow(
                        label = "Last Played",
                        value = if (game.lastPlayedTimestamp > 0) SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(game.lastPlayedTimestamp)) else "Never"
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Per-Game Configuration
            Text("PER-GAME GRAPHICS & CPU SETTINGS", color = XeniaGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Resolution Scaling
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Resolution Scale", color = TechTextPrimary, fontSize = 13.sp)
                            Text("${currentSettings.resolutionScale}x (${(1280 * currentSettings.resolutionScale).toInt()}x${(720 * currentSettings.resolutionScale).toInt()})", color = XeniaGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = currentSettings.resolutionScale,
                            onValueChange = {
                                val rounded = (it * 4).toInt() / 4f
                                currentSettings = currentSettings.copy(resolutionScale = rounded)
                                onSaveSettings(game, currentSettings)
                            },
                            valueRange = 0.5f..2.0f,
                            steps = 5,
                            colors = SliderDefaults.colors(thumbColor = XeniaGreen, activeTrackColor = XeniaGreen)
                        )
                    }

                    // Vulkan Present Mode
                    Column {
                        Text("Vulkan Present Mode", color = TechTextPrimary, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            PresentMode.values().forEach { mode ->
                                val selected = currentSettings.presentMode == mode
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        currentSettings = currentSettings.copy(presentMode = mode)
                                        onSaveSettings(game, currentSettings)
                                    },
                                    label = { Text(mode.name, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = XeniaGreenContainer,
                                        selectedLabelColor = OnXeniaGreenContainer,
                                        containerColor = DarkSurfaceContainerHighest,
                                        labelColor = TechTextSecondary
                                    )
                                )
                            }
                        }
                    }

                    // VSync & Shader Cache Toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Vulkan VSync Lock (60 FPS)", color = TechTextPrimary, fontSize = 13.sp)
                        Switch(
                            checked = currentSettings.vsync,
                            onCheckedChange = {
                                currentSettings = currentSettings.copy(vsync = it)
                                onSaveSettings(game, currentSettings)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = XeniaGreen, checkedTrackColor = XeniaGreenContainer)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Persistent Shader Cache (PSO)", color = TechTextPrimary, fontSize = 13.sp)
                        Switch(
                            checked = currentSettings.shaderCache,
                            onCheckedChange = {
                                currentSettings = currentSettings.copy(shaderCache = it)
                                onSaveSettings(game, currentSettings)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = XeniaGreen, checkedTrackColor = XeniaGreenContainer)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Remove Game from Library
            OutlinedButton(
                onClick = { showDeleteConfirm = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(ErrorRed, Color(0xFF880000)))),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Remove from Library", fontWeight = FontWeight.SemiBold)
            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                containerColor = DarkSurface,
                title = { Text("Remove from Library?", color = TechTextPrimary) },
                text = {
                    Text(
                        "This will remove '${game.titleName}' from your Xenia library index. The game file on your storage will NOT be deleted.",
                        color = TechTextSecondary
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirm = false
                            onRemove(game)
                        }
                    ) {
                        Text("Remove", color = ErrorRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel", color = TechTextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TechTextMuted, fontSize = 12.sp)
        Text(
            value,
            color = TechTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
