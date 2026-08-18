package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.ControllerType
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun ControllerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val controllerManager = viewModel.controllerManager
    val connectedGamepads by controllerManager.connectedGamepads.collectAsState()
    val currentProfile by controllerManager.currentProfile.collectAsState()

    var deadzoneLeft by remember(currentProfile) { mutableStateOf(currentProfile.deadzoneLeft) }
    var deadzoneRight by remember(currentProfile) { mutableStateOf(currentProfile.deadzoneRight) }
    var triggerDeadzone by remember(currentProfile) { mutableStateOf(currentProfile.triggerDeadzone) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Section: Connected Gamepads & Multi-Player Slots
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "CONNECTED GAMEPADS (P1 - P4)",
                color = XeniaGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            IconButton(
                onClick = { controllerManager.refreshConnectedGamepads() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = TechTextSecondary)
            }
        }

        if (connectedGamepads.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(TechBorder, Color(0xFF1B222D))))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(DarkSurfaceContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.SportsEsports, contentDescription = null, tint = TechTextMuted)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("No Physical Gamepads Connected", color = TechTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Connect DualSense, DualShock 4, or Xbox controllers via Bluetooth or USB-OTG.", color = TechTextSecondary, fontSize = 11.sp)
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                connectedGamepads.forEach { pad ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(XeniaGreen, TechBorder)))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(XeniaGreenContainer, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("P${pad.playerSlot}", color = XeniaGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(pad.name, color = TechTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(pad.controllerType.displayName, color = XeniaGreen, fontSize = 11.sp)
                                    Text("Vendor: 0x${pad.vendorId.toString(16).uppercase()} Product: 0x${pad.productId.toString(16).uppercase()}", color = TechTextMuted, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace)
                                }
                            }

                            if (pad.hasVibrator) {
                                IconButton(
                                    onClick = { controllerManager.triggerVibration(playerSlot = pad.playerSlot, durationMs = 250) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(DarkSurfaceContainer, CircleShape)
                                ) {
                                    Icon(Icons.Filled.Vibration, contentDescription = "Test Vibration", tint = XeniaGreen, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Controller Profiles
        Text(
            "MAPPING PROFILES",
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
                Text("Select Default Mapping Schema", color = TechTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        ControllerType.XBOX_SERIES to "Xbox Series / One",
                        ControllerType.DUALSENSE to "DualSense (PS5)",
                        ControllerType.DUALSHOCK4 to "DualShock 4"
                    ).forEach { (type, label) ->
                        val isSelected = currentProfile.controllerType == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { controllerManager.selectProfile(type) },
                            label = { Text(label, fontSize = 11.sp) },
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

        // Section: Thumbsticks & Triggers Calibration
        Text(
            "ANALOG STICKS & TRIGGERS CALIBRATION",
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
                // Left Stick Deadzone
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Left Stick Deadzone", color = TechTextPrimary, fontSize = 13.sp)
                        Text("${(deadzoneLeft * 100).toInt()}%", color = XeniaGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = deadzoneLeft,
                        onValueChange = { deadzoneLeft = it },
                        valueRange = 0.02f..0.35f,
                        colors = SliderDefaults.colors(thumbColor = XeniaGreen, activeTrackColor = XeniaGreen)
                    )
                }

                // Right Stick Deadzone
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Right Stick Deadzone", color = TechTextPrimary, fontSize = 13.sp)
                        Text("${(deadzoneRight * 100).toInt()}%", color = XeniaGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = deadzoneRight,
                        onValueChange = { deadzoneRight = it },
                        valueRange = 0.02f..0.35f,
                        colors = SliderDefaults.colors(thumbColor = XeniaGreen, activeTrackColor = XeniaGreen)
                    )
                }

                // Trigger Threshold
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Triggers Deadzone (LT / RT)", color = TechTextPrimary, fontSize = 13.sp)
                        Text("${(triggerDeadzone * 100).toInt()}%", color = XeniaGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = triggerDeadzone,
                        onValueChange = { triggerDeadzone = it },
                        valueRange = 0.01f..0.25f,
                        colors = SliderDefaults.colors(thumbColor = XeniaGreen, activeTrackColor = XeniaGreen)
                    )
                }

                Button(
                    onClick = { controllerManager.triggerVibration(durationMs = 200) },
                    colors = ButtonDefaults.buttonColors(containerColor = XeniaGreen),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("test_vibration_button")
                ) {
                    Icon(Icons.Filled.Vibration, contentDescription = null, tint = Color(0xFF00391A))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test Controller Vibration Haptics", color = Color(0xFF00391A), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
