package com.example.core.input

import android.content.Context
import android.hardware.input.InputManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import com.example.core.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ControllerManager(private val context: Context) : InputManager.InputDeviceListener {
    private val TAG = "ControllerManager"
    private val inputManager = context.getSystemService(Context.INPUT_SERVICE) as? InputManager

    private val _connectedGamepads = MutableStateFlow<List<ConnectedGamepad>>(emptyList())
    val connectedGamepads: StateFlow<List<ConnectedGamepad>> = _connectedGamepads.asStateFlow()

    private val _currentProfile = MutableStateFlow(createDefaultProfile(ControllerType.XBOX_SERIES))
    val currentProfile: StateFlow<ControllerProfile> = _currentProfile.asStateFlow()

    // 4-Player Virtual State Buffers
    val playerStates = Array(4) { PlayerInputState() }

    data class PlayerInputState(
        var buttonsMask: Int = 0,
        var leftStickX: Float = 0f,
        var leftStickY: Float = 0f,
        var rightStickX: Float = 0f,
        var rightStickY: Float = 0f,
        var leftTrigger: Float = 0f,
        var rightTrigger: Float = 0f
    )

    init {
        inputManager?.registerInputDeviceListener(this, null)
        refreshConnectedGamepads()
    }

    fun refreshConnectedGamepads() {
        val list = mutableListOf<ConnectedGamepad>()
        val deviceIds = InputDevice.getDeviceIds()
        var playerIndex = 1

        for (id in deviceIds) {
            val dev = InputDevice.getDevice(id) ?: continue
            val sources = dev.sources
            val isGamepad = (sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                    (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK

            if (isGamepad) {
                val vendorId = dev.vendorId
                val productId = dev.productId
                val name = dev.name
                val type = classifyController(vendorId, productId, name)
                val hasVib = dev.vibrator?.hasVibrator() == true

                list.add(
                    ConnectedGamepad(
                        deviceId = id,
                        name = name,
                        vendorId = vendorId,
                        productId = productId,
                        playerSlot = playerIndex.coerceAtMost(4),
                        controllerType = type,
                        hasVibrator = hasVib
                    )
                )
                playerIndex++
            }
        }
        _connectedGamepads.value = list
    }

    fun classifyController(vendorId: Int, productId: Int, name: String): ControllerType {
        val lower = name.lowercase()
        return when {
            vendorId == 0x054C && (productId == 0x0CE6 || productId == 0x0DF2) || lower.contains("dualsense") || lower.contains("ps5") -> ControllerType.DUALSENSE
            vendorId == 0x054C && (productId == 0x05C4 || productId == 0x09CC) || lower.contains("wireless controller") || lower.contains("dualshock") || lower.contains("ps4") -> ControllerType.DUALSHOCK4
            vendorId == 0x045E && (productId == 0x0B12 || productId == 0x0B13 || productId == 0x02E0) || lower.contains("xbox series") || lower.contains("xbox wireless") -> ControllerType.XBOX_SERIES
            vendorId == 0x045E && (productId == 0x02D1 || productId == 0x02DD) || lower.contains("xbox one") -> ControllerType.XBOX_ONE
            vendorId == 0x045E && productId == 0x028E || lower.contains("xbox 360") -> ControllerType.XBOX_360
            else -> ControllerType.GENERIC_GAMEPAD
        }
    }

    fun handleKeyEvent(event: KeyEvent, playerSlot: Int = 1): Boolean {
        if (playerSlot !in 1..4) return false
        val state = playerStates[playerSlot - 1]
        val isDown = event.action == KeyEvent.ACTION_DOWN
        val bit = when (event.keyCode) {
            KeyEvent.KEYCODE_BUTTON_A -> 0x1000 // Xbox A
            KeyEvent.KEYCODE_BUTTON_B -> 0x2000 // Xbox B
            KeyEvent.KEYCODE_BUTTON_X -> 0x4000 // Xbox X
            KeyEvent.KEYCODE_BUTTON_Y -> 0x8000 // Xbox Y
            KeyEvent.KEYCODE_BUTTON_L1 -> 0x0100 // LB
            KeyEvent.KEYCODE_BUTTON_R1 -> 0x0200 // RB
            KeyEvent.KEYCODE_BUTTON_THUMBL -> 0x0040 // LS
            KeyEvent.KEYCODE_BUTTON_THUMBR -> 0x0080 // RS
            KeyEvent.KEYCODE_BUTTON_START -> 0x0010 // START
            KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BACK -> 0x0020 // BACK
            KeyEvent.KEYCODE_BUTTON_MODE -> 0x0400 // GUIDE
            KeyEvent.KEYCODE_DPAD_UP -> 0x0001
            KeyEvent.KEYCODE_DPAD_DOWN -> 0x0002
            KeyEvent.KEYCODE_DPAD_LEFT -> 0x0004
            KeyEvent.KEYCODE_DPAD_RIGHT -> 0x0008
            else -> 0
        }

        if (bit != 0) {
            state.buttonsMask = if (isDown) {
                state.buttonsMask or bit
            } else {
                state.buttonsMask and bit.inv()
            }
            return true
        }
        return false
    }

    fun handleMotionEvent(event: MotionEvent, playerSlot: Int = 1): Boolean {
        if (playerSlot !in 1..4) return false
        val state = playerStates[playerSlot - 1]

        // Left Analog Stick
        val lx = event.getAxisValue(MotionEvent.AXIS_X)
        val ly = event.getAxisValue(MotionEvent.AXIS_Y)
        state.leftStickX = applyDeadzone(lx, _currentProfile.value.deadzoneLeft)
        state.leftStickY = applyDeadzone(-ly, _currentProfile.value.deadzoneLeft) // Y is inverted in Android MotionEvent

        // Right Analog Stick (Z / RZ on standard Android Gamepad mapping)
        val rx = event.getAxisValue(MotionEvent.AXIS_Z)
        val ry = event.getAxisValue(MotionEvent.AXIS_RZ)
        state.rightStickX = applyDeadzone(rx, _currentProfile.value.deadzoneRight)
        state.rightStickY = applyDeadzone(-ry, _currentProfile.value.deadzoneRight)

        // Triggers (LTRIGGER / RTRIGGER or BRAKE / GAS)
        val lt = event.getAxisValue(MotionEvent.AXIS_LTRIGGER).coerceAtLeast(event.getAxisValue(MotionEvent.AXIS_BRAKE))
        val rt = event.getAxisValue(MotionEvent.AXIS_RTRIGGER).coerceAtLeast(event.getAxisValue(MotionEvent.AXIS_GAS))
        state.leftTrigger = if (lt > _currentProfile.value.triggerDeadzone) lt else 0f
        state.rightTrigger = if (rt > _currentProfile.value.triggerDeadzone) rt else 0f

        // D-Pad Hat Axes (AXIS_HAT_X / AXIS_HAT_Y)
        val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)

        var dpadMask = 0
        if (hatY < -0.5f) dpadMask = dpadMask or 0x0001 // UP
        if (hatY > 0.5f) dpadMask = dpadMask or 0x0002 // DOWN
        if (hatX < -0.5f) dpadMask = dpadMask or 0x0004 // LEFT
        if (hatX > 0.5f) dpadMask = dpadMask or 0x0008 // RIGHT

        // Merge D-Pad bits
        state.buttonsMask = (state.buttonsMask and 0xFFF0) or dpadMask
        return true
    }

    fun triggerVibration(playerSlot: Int = 1, leftMotor: Float = 0.5f, rightMotor: Float = 0.5f, durationMs: Long = 120) {
        try {
            val gamepad = _connectedGamepads.value.find { it.playerSlot == playerSlot }
            if (gamepad != null) {
                val dev = InputDevice.getDevice(gamepad.deviceId)
                dev?.vibrator?.let { vib ->
                    if (vib.hasVibrator()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val strength = ((leftMotor + rightMotor) / 2f * 255).toInt().coerceIn(1, 255)
                            vib.vibrate(VibrationEffect.createOneShot(durationMs, strength))
                        } else {
                            vib.vibrate(durationMs)
                        }
                        return
                    }
                }
            }

            // Fallback to phone system vibrator
            val sysVibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (sysVibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val strength = ((leftMotor + rightMotor) / 2f * 255).toInt().coerceIn(1, 255)
                    sysVibrator.vibrate(VibrationEffect.createOneShot(durationMs, strength))
                } else {
                    sysVibrator.vibrate(durationMs)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibration failed: ${e.message}")
        }
    }

    private fun applyDeadzone(value: Float, deadzone: Float): Float {
        if (Math.abs(value) < deadzone) return 0f
        val sign = if (value > 0) 1f else -1f
        return sign * ((Math.abs(value) - deadzone) / (1f - deadzone)).coerceIn(0f, 1f)
    }

    fun selectProfile(type: ControllerType) {
        _currentProfile.value = createDefaultProfile(type)
    }

    fun createDefaultProfile(type: ControllerType): ControllerProfile {
        return ControllerProfile(
            id = type.name.lowercase(),
            name = "${type.displayName} Default",
            controllerType = type,
            deadzoneLeft = 0.12f,
            deadzoneRight = 0.12f,
            triggerDeadzone = 0.05f,
            stickSensitivity = 1.0f,
            vibrationStrength = 1.0f
        )
    }

    override fun onInputDeviceAdded(deviceId: Int) {
        refreshConnectedGamepads()
    }

    override fun onInputDeviceRemoved(deviceId: Int) {
        refreshConnectedGamepads()
    }

    override fun onInputDeviceChanged(deviceId: Int) {
        refreshConnectedGamepads()
    }
}
