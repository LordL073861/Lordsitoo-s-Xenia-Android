package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.AudioLatency
import com.example.core.model.PresentMode
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun GraphicsSettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val globalSettings by viewModel.globalSettings.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Section: Graphics & Vulkan Engine
        Text(
            "GRAPHICS & VULKAN BACKEND",
            color = XeniaGreen,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Resolution Scale
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Global Internal Resolution", color = TechTextPrimary, fontSize = 13.sp)
                        Text(
                            "${globalSettings.resolutionScale}x (${(1280 * globalSettings.resolutionScale).toInt()}x${(720 * globalSettings.resolutionScale).toInt()})",
                            color = XeniaGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = globalSettings.resolutionScale,
                        onValueChange = {
                            val rounded = (it * 4).toInt() / 4f
                            viewModel.updateGlobalSettings(globalSettings.copy(resolutionScale = rounded))
                        },
                        valueRange = 0.5f..2.0f,
                        steps = 5,
                        colors = SliderDefaults.colors(thumbColor = XeniaGreen, activeTrackColor = XeniaGreen)
                    )
                }

                // Vulkan Present Mode
                Column {
                    Text("Vulkan Presentation Mode", color = TechTextPrimary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PresentMode.values().forEach { mode ->
                            val selected = globalSettings.presentMode == mode
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.updateGlobalSettings(globalSettings.copy(presentMode = mode)) },
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

                // VSync
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Vulkan VSync Lock (60 Hz)", color = TechTextPrimary, fontSize = 13.sp)
                        Text("Eliminates screen tearing and reduces GPU thermal overhead.", color = TechTextMuted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = globalSettings.vsync,
                        onCheckedChange = { viewModel.updateGlobalSettings(globalSettings.copy(vsync = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = XeniaGreen, checkedTrackColor = XeniaGreenContainer)
                    )
                }

                // Persistent Shader Cache
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Vulkan Pipeline Shader Cache (PSO)", color = TechTextPrimary, fontSize = 13.sp)
                        Text("Pre-compiles SPIR-V bytecodes to prevent in-game shader stutter.", color = TechTextMuted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = globalSettings.shaderCache,
                        onCheckedChange = { viewModel.updateGlobalSettings(globalSettings.copy(shaderCache = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = XeniaGreen, checkedTrackColor = XeniaGreenContainer)
                    )
                }
            }
        }

        // Section: CPU Recompiler (PowerPC -> ARM64)
        Text(
            "CPU RECOMPILER & JIT (ARM64)",
            color = XeniaGreen,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ARM64 LSE Atomics Acceleration", color = TechTextPrimary, fontSize = 13.sp)
                        Text("Uses hardware Large System Extensions on Dimensity & modern ARMv8.2+ CPUs.", color = TechTextMuted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = globalSettings.arm64LseAtomics,
                        onCheckedChange = { viewModel.updateGlobalSettings(globalSettings.copy(arm64LseAtomics = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = XeniaGreen, checkedTrackColor = XeniaGreenContainer)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Xenon 6-Thread Multiprocessing", color = TechTextPrimary, fontSize = 13.sp)
                        Text("Distributes 3 Xbox 360 dual-threaded PowerPC cores across host CPU clusters.", color = TechTextMuted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = globalSettings.cpuThreads == 6,
                        onCheckedChange = { viewModel.updateGlobalSettings(globalSettings.copy(cpuThreads = if (it) 6 else 3)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = XeniaGreen, checkedTrackColor = XeniaGreenContainer)
                    )
                }
            }
        }

        // Section: Audio Latency & Output
        Text(
            "AUDIO ENGINE (AAUDIO / OBOE)",
            color = XeniaGreen,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Audio Buffer Mode", color = TechTextPrimary, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AudioLatency.values().forEach { mode ->
                        val selected = globalSettings.audioLatencyMode == mode
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.updateGlobalSettings(globalSettings.copy(audioLatencyMode = mode)) },
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
        }

        // Section: System & App Behavior
        Text(
            "SYSTEM & DISPLAY",
            color = XeniaGreen,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Keep Screen Awake During Emulation", color = TechTextPrimary, fontSize = 13.sp)
                        Text("Prevents screen timeout during active gameplay.", color = TechTextMuted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = globalSettings.keepScreenOn,
                        onCheckedChange = { viewModel.updateGlobalSettings(globalSettings.copy(keepScreenOn = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = XeniaGreen, checkedTrackColor = XeniaGreenContainer)
                    )
                }
            }
        }
    }
}
