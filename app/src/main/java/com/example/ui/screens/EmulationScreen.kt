package com.example.ui.screens

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.core.model.EmulationState
import com.example.core.model.GameItem
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun EmulationScreen(
    game: GameItem,
    viewModel: MainViewModel,
    onExitEmulation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val emulationState by viewModel.emulationState.collectAsState()
    var showPauseMenu by remember { mutableStateOf(false) }
    var showTouchControls by remember { mutableStateOf(true) }
    var showHud by remember { mutableStateOf(true) }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // Native Vulkan SurfaceView Holder
        AndroidView(
            factory = { context ->
                SurfaceView(context).apply {
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            // Surface is ready for Vulkan swapchain presentation
                        }

                        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                            // Dimensions updated
                        }

                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            // Clean up surface
                        }
                    })
                }
            },
            modifier = Modifier.fillMaxSize().testTag("vulkan_surface_view")
        )

        // Running State HUD & Touch Overlay
        if (emulationState is EmulationState.Running) {
            val state = emulationState as EmulationState.Running

            // Top HUD Overlay
            if (showHud) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, start = 12.dp, end = 12.dp)
                        .align(Alignment.TopCenter)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x99000000))
                            .border(1.dp, Color(0x3300E676), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(XeniaGreen, CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "${String.format("%.1f", state.fps)} FPS",
                                color = XeniaGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Text(
                            "${String.format("%.1f", state.frameTimeMs)} ms",
                            color = TechTextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        Text(
                            "CPU: ${String.format("%.0f", state.guestCpuUsage)}%",
                            color = TechTextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        Text(
                            "RAM: ${state.hostRamUsageMb} MB",
                            color = TechTextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        Text(
                            state.activeResolution,
                            color = TechTextMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Top Action Icons (Pause Menu, Toggle Touch Controls)
                    Row(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = { showTouchControls = !showTouchControls },
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0x80000000), CircleShape)
                        ) {
                            Icon(
                                if (showTouchControls) Icons.Filled.VideogameAsset else Icons.Filled.VideogameAssetOff,
                                contentDescription = "Toggle Touch Controls",
                                tint = if (showTouchControls) XeniaGreen else TechTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { showPauseMenu = true },
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0x80000000), CircleShape)
                                .testTag("pause_emulation_button")
                        ) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // On-Screen Touch Controls
            if (showTouchControls && !showPauseMenu) {
                TouchControllerOverlay(
                    controllerManager = viewModel.controllerManager,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Loading & Shader Compilation Screen
        if (emulationState is EmulationState.Initializing || emulationState is EmulationState.CompilingShaders) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    CircularProgressIndicator(
                        color = XeniaGreen,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        game.titleName,
                        color = TechTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val statusMsg = when (val s = emulationState) {
                        is EmulationState.Initializing -> s.step
                        is EmulationState.CompilingShaders -> "Compiling Vulkan Pipeline Shaders (${s.current}/${s.total})..."
                        else -> "Starting Emulation..."
                    }

                    Text(
                        statusMsg,
                        color = XeniaGreen,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Error Screen
        if (emulationState is EmulationState.Error) {
            val err = emulationState as EmulationState.Error
            AlertDialog(
                onDismissRequest = onExitEmulation,
                containerColor = DarkSurface,
                title = { Text(err.title, color = ErrorRed, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(err.message, color = TechTextPrimary, fontSize = 13.sp)
                        if (err.technicalLog.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                err.technicalLog,
                                color = TechTextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 6
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = onExitEmulation,
                        colors = ButtonDefaults.buttonColors(containerColor = XeniaGreen)
                    ) {
                        Text("Exit to Library", color = Color(0xFF00391A), fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Pause Menu Dialog
        if (showPauseMenu) {
            AlertDialog(
                onDismissRequest = { showPauseMenu = false },
                containerColor = DarkSurface,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.PauseCircle, contentDescription = null, tint = XeniaGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(game.titleName, color = TechTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showPauseMenu = false },
                            colors = ButtonDefaults.buttonColors(containerColor = XeniaGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Resume Emulation", color = Color(0xFF00391A), fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showHud = !showHud },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (showHud) "Hide Performance HUD" else "Show Performance HUD", color = TechTextPrimary)
                        }

                        OutlinedButton(
                            onClick = { showTouchControls = !showTouchControls },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (showTouchControls) "Disable On-Screen Controls" else "Enable On-Screen Controls", color = TechTextPrimary)
                        }

                        Button(
                            onClick = {
                                showPauseMenu = false
                                viewModel.stopEmulation()
                                onExitEmulation()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3E1212)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.ExitToApp, contentDescription = null, tint = ErrorRed)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Exit Emulation", color = ErrorRed, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                confirmButton = {}
            )
        }
    }
}
