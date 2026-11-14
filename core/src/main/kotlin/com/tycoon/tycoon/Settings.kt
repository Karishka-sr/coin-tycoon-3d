package com.tycoon.tycoon

/**
 * User-adjustable options. Setters clamp their input, so the UI can push raw
 * slider values straight in without validating them first.
 */
class Settings {

    var lookSensitivity: Float = 1f
        set(value) {
            field = value.coerceIn(MIN_SENSITIVITY, MAX_SENSITIVITY)
        }

    var invertLookY: Boolean = false

    var showCrosshair: Boolean = true

    var showJoystick: Boolean = true

    fun snapshot(): SettingsData = SettingsData().also { data ->
        data.lookSensitivity = lookSensitivity
        data.invertLookY = invertLookY
        data.showCrosshair = showCrosshair
        data.showJoystick = showJoystick
    }

    fun restore(data: SettingsData) {
        lookSensitivity = data.lookSensitivity
        invertLookY = data.invertLookY
        showCrosshair = data.showCrosshair
        showJoystick = data.showJoystick
    }

    companion object {
        const val MIN_SENSITIVITY = 0.25f
        const val MAX_SENSITIVITY = 3f
    }
}
