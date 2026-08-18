package com.example.ui.screens

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.input.ControllerManager
import com.example.ui.theme.XeniaGreen
import kotlin.math.*

@Composable
fun TouchControllerOverlay(
    controllerManager: ControllerManager,
    alpha: Float = 0.85f,
    modifier: Modifier = Modifier
) {
    var leftStickOffset by remember { mutableStateOf(Offset.Zero) }
    var rightStickOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(alpha)
            .padding(16.dp)
    ) {
        // TOP TRIGGERS & BUMPERS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Bumpers & Triggers
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TouchTriggerButton(
                    label = "LT",
                    onPress = { isDown ->
                        controllerManager.playerStates[0].leftTrigger = if (isDown) 1.0f else 0f
                        if (isDown) controllerManager.triggerVibration(durationMs = 40)
                    }
                )
                TouchBumperButton(
                    label = "LB",
                    onPress = { isDown ->
                        updateButtonMask(controllerManager, 0x0100, isDown)
                    }
                )
            }

            // Central Guide & System Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                TouchSystemButton(
                    label = "BACK",
                    onPress = { isDown -> updateButtonMask(controllerManager, 0x0020, isDown) }
                )
                TouchGuideButton(
                    onPress = { isDown -> updateButtonMask(controllerManager, 0x0400, isDown) }
                )
                TouchSystemButton(
                    label = "START",
                    onPress = { isDown -> updateButtonMask(controllerManager, 0x0010, isDown) }
                )
            }

            // Right Bumpers & Triggers
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TouchBumperButton(
                    label = "RB",
                    onPress = { isDown ->
                        updateButtonMask(controllerManager, 0x0200, isDown)
                    }
                )
                TouchTriggerButton(
                    label = "RT",
                    onPress = { isDown ->
                        controllerManager.playerStates[0].rightTrigger = if (isDown) 1.0f else 0f
                        if (isDown) controllerManager.triggerVibration(durationMs = 40)
                    }
                )
            }
        }

        // BOTTOM CONTROLS: LEFT (Thumbstick + D-Pad), RIGHT (ABXY + Right Thumbstick)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Left Section: Analog Stick (LS) & D-Pad
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                VirtualThumbstick(
                    label = "LS",
                    offset = leftStickOffset,
                    onOffsetChange = {
                        leftStickOffset = it
                        controllerManager.playerStates[0].leftStickX = it.x / 50f
                        controllerManager.playerStates[0].leftStickY = -it.y / 50f
                    }
                )

                VirtualDPad(
                    onDirection = { mask, isDown ->
                        updateButtonMask(controllerManager, mask, isDown)
                    }
                )
            }

            // Right Section: ABXY Action Diamond & Right Thumbstick (RS)
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                VirtualActionDiamond(
                    onButtonPress = { bit, isDown ->
                        updateButtonMask(controllerManager, bit, isDown)
                    }
                )

                VirtualThumbstick(
                    label = "RS",
                    offset = rightStickOffset,
                    onOffsetChange = {
                        rightStickOffset = it
                        controllerManager.playerStates[0].rightStickX = it.x / 50f
                        controllerManager.playerStates[0].rightStickY = -it.y / 50f
                    }
                )
            }
        }
    }
}

private fun updateButtonMask(manager: ControllerManager, mask: Int, isDown: Boolean) {
    val state = manager.playerStates[0]
    state.buttonsMask = if (isDown) state.buttonsMask or mask else state.buttonsMask and mask.inv()
    if (isDown) {
        manager.triggerVibration(durationMs = 35)
    }
}

@Composable
fun VirtualThumbstick(
    label: String,
    offset: Offset,
    onOffsetChange: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxRadius = 50f

    Box(
        modifier = modifier
            .size(110.dp)
            .clip(CircleShape)
            .background(Color(0x33FFFFFF))
            .border(1.5.dp, Color(0x66FFFFFF), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { onOffsetChange(Offset.Zero) },
                    onDragCancel = { onOffsetChange(Offset.Zero) },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = offset + dragAmount
                        val dist = sqrt(newOffset.x * newOffset.x + newOffset.y * newOffset.y)
                        val clamped = if (dist > maxRadius) {
                            Offset(newOffset.x / dist * maxRadius, newOffset.y / dist * maxRadius)
                        } else {
                            newOffset
                        }
                        onOffsetChange(clamped)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(offset.x.toInt(), offset.y.toInt()) }
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(listOf(Color(0xCC00E676), Color(0x99003817)))
                )
                .border(1.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun VirtualDPad(
    onDirection: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(110.dp),
        contentAlignment = Alignment.Center
    ) {
        // UP
        DPadArrow(
            label = "▲",
            modifier = Modifier.align(Alignment.TopCenter),
            onPress = { onDirection(0x0001, it) }
        )
        // DOWN
        DPadArrow(
            label = "▼",
            modifier = Modifier.align(Alignment.BottomCenter),
            onPress = { onDirection(0x0002, it) }
        )
        // LEFT
        DPadArrow(
            label = "◀",
            modifier = Modifier.align(Alignment.CenterStart),
            onPress = { onDirection(0x0004, it) }
        )
        // RIGHT
        DPadArrow(
            label = "▶",
            modifier = Modifier.align(Alignment.CenterEnd),
            onPress = { onDirection(0x0008, it) }
        )
    }
}

@Composable
fun DPadArrow(
    label: String,
    onPress: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x4D000000))
            .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(6.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPress(true)
                        tryAwaitRelease()
                        onPress(false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun VirtualActionDiamond(
    onButtonPress: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        // Y (Top - Yellow)
        ActionButton(
            label = "Y",
            color = Color(0xFFFFD600),
            modifier = Modifier.align(Alignment.TopCenter),
            onPress = { onButtonPress(0x8000, it) }
        )
        // A (Bottom - Green)
        ActionButton(
            label = "A",
            color = Color(0xFF00E676),
            modifier = Modifier.align(Alignment.BottomCenter),
            onPress = { onButtonPress(0x1000, it) }
        )
        // X (Left - Blue)
        ActionButton(
            label = "X",
            color = Color(0xFF2979FF),
            modifier = Modifier.align(Alignment.CenterStart),
            onPress = { onButtonPress(0x4000, it) }
        )
        // B (Right - Red)
        ActionButton(
            label = "B",
            color = Color(0xFFFF1744),
            modifier = Modifier.align(Alignment.CenterEnd),
            onPress = { onButtonPress(0x2000, it) }
        )
    }
}

@Composable
fun ActionButton(
    label: String,
    color: Color,
    onPress: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.8f))
            .border(1.5.dp, Color.White, CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPress(true)
                        tryAwaitRelease()
                        onPress(false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun TouchTriggerButton(
    label: String,
    onPress: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 54.dp, height = 36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x66107C10))
            .border(1.dp, XeniaGreen, RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPress(true)
                        tryAwaitRelease()
                        onPress(false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TouchBumperButton(
    label: String,
    onPress: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 50.dp, height = 32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x4D000000))
            .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(6.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPress(true)
                        tryAwaitRelease()
                        onPress(false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TouchSystemButton(
    label: String,
    onPress: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 46.dp, height = 26.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0x4D000000))
            .border(1.dp, Color(0x4DFFFFFF), RoundedCornerShape(4.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPress(true)
                        tryAwaitRelease()
                        onPress(false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TouchGuideButton(
    onPress: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color(0xFF00E676), Color(0xFF004D1A))))
            .border(1.5.dp, Color.White, CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPress(true)
                        tryAwaitRelease()
                        onPress(false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text("✕", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
