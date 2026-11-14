package com.tycoon.tycoon

/** Plain serializable snapshot of [Settings]. Field names are the save format. */
class SettingsData {
    var lookSensitivity: Float = 1f
    var invertLookY: Boolean = false
    var showCrosshair: Boolean = true
    var showJoystick: Boolean = true
}
