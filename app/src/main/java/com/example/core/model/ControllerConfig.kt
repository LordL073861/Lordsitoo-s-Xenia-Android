package com.example.core.model

data class ControllerProfile(
    val id: String,
    val name: String,
    val controllerType: ControllerType,
    val deadzoneLeft: Float = 0.15f,
    val deadzoneRight: Float = 0.15f,
    val triggerDeadzone: Float = 0.05f,
    val stickSensitivity: Float = 1.0f,
    val invertY: Boolean = false,
    val vibrationStrength: Float = 1.0f,
    val buttonMap: Map<XboxButton, Int> = emptyMap()
)

enum class ControllerType(val displayName: String) {
    DUALSENSE("Sony DualSense (PS5)"),
    DUALSHOCK4("Sony DualShock 4 (PS4)"),
    XBOX_SERIES("Xbox Wireless (Series X/S)"),
    XBOX_ONE("Xbox One Wireless"),
    XBOX_360("Xbox 360 Controller"),
    GENERIC_GAMEPAD("Generic Android Gamepad"),
    TOUCH_OVERLAY("On-Screen Touch Controller")
}

enum class XboxButton(val label: String) {
    A("A (Cross)"),
    B("B (Circle)"),
    X("X (Square)"),
    Y("Y (Triangle)"),
    LB("LB (L1)"),
    RB("RB (R1)"),
    LT("LT (L2)"),
    RT("RT (R2)"),
    LS("LS (L3)"),
    RS("RS (R3)"),
    DPAD_UP("D-Pad Up"),
    DPAD_DOWN("D-Pad Down"),
    DPAD_LEFT("D-Pad Left"),
    DPAD_RIGHT("D-Pad Right"),
    START("Start (Options / Menu)"),
    BACK("Back (Share / View)"),
    GUIDE("Xbox Guide (Home)")
}

data class ConnectedGamepad(
    val deviceId: Int,
    val name: String,
    val vendorId: Int,
    val productId: Int,
    val playerSlot: Int, // 1..4
    val controllerType: ControllerType,
    val hasVibrator: Boolean
)
