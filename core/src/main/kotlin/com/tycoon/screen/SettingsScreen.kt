package com.tycoon.screen

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.tycoon.ui.onClick
import com.tycoon.TycoonGame
import com.tycoon.tycoon.Settings
import kotlin.math.roundToInt

/**
 * Options screen. Every control writes straight into the shared [Settings] and
 * persists immediately, so the changes survive even if the player leaves
 * without touching anything else.
 */
class SettingsScreen(game: TycoonGame) : MenuScreen(game) {

    private val settings: Settings = game.settings

    private val sensitivityValue = Label("", skin, "hint")
    private val sensitivitySlider = Slider(Settings.MIN_SENSITIVITY, Settings.MAX_SENSITIVITY, 0.05f, false, skin)

    private val volumeValue = Label("", skin, "hint")
    private val volumeSlider = Slider(Settings.MIN_VOLUME, Settings.MAX_VOLUME, 0.05f, false, skin)

    private val invertCheck = CheckBox("Инвертировать вертикаль", skin)
    private val crosshairCheck = CheckBox("Показывать прицел", skin)
    private val joystickCheck = CheckBox("Показывать джойстик", skin)
    private val soundCheck = CheckBox("Звук", skin)
    private val flyCheck = CheckBox("Полёт в мире блоков", skin)

    init {
        val table = contentTable()
        table.defaults().pad(8f)

        table.add(Label("Настройки", skin, "title")).colspan(2).padBottom(16f).row()

        sensitivitySlider.value = settings.lookSensitivity
        sensitivitySlider.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                settings.lookSensitivity = sensitivitySlider.value
                refreshSensitivity()
                game.saveSettings()
            }
        })
        table.add(Label("Чувствительность обзора", skin)).left()
        table.add(sensitivitySlider).width(380f).left().row()
        table.add().left()
        table.add(sensitivityValue).left().row()

        table.add(bind(invertCheck, settings.invertLookY) { settings.invertLookY = it }).colspan(2).left().row()
        table.add(bind(crosshairCheck, settings.showCrosshair) { settings.showCrosshair = it }).colspan(2).left().row()
        table.add(bind(joystickCheck, settings.showJoystick) { settings.showJoystick = it }).colspan(2).left().row()
        table.add(bind(flyCheck, settings.voxelFlying) { settings.voxelFlying = it }).colspan(2).left().row()

        volumeSlider.value = settings.masterVolume
        volumeSlider.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                settings.masterVolume = volumeSlider.value
                refreshVolume()
                game.saveSettings()
            }
        })
        table.add(Label("Громкость", skin)).left().padTop(16f)
        table.add(volumeSlider).width(380f).left().row()
        table.add().left()
        table.add(volumeValue).left().row()

        table.add(bind(soundCheck, settings.sfxEnabled) { settings.sfxEnabled = it }).colspan(2).left().row()

        val reset = TextButton("Вернуть настройки по умолчанию", skin, "big")
        reset.onClick(game.sound) {
            settings.restore(Settings().snapshot())
            game.saveSettings()
            syncControls()
        }
        table.add(reset).colspan(2).width(480f).height(68f).padTop(16f).row()

        val back = TextButton("Назад", skin, "big")
        back.onClick(game.sound) { game.swapLater { game.showMainMenu() } }
        table.add(back).colspan(2).width(480f).height(86f).padTop(8f).row()

        syncControls()
    }

    /** Wires a checkbox to [apply], keeping the shared settings in sync. */
    private fun bind(check: CheckBox, initial: Boolean, apply: (Boolean) -> Unit): CheckBox = check.apply {
        isChecked = initial
        addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                apply(isChecked)
                game.saveSettings()
            }
        })
    }

    /** Pushes the current settings back into the widgets after an external change. */
    private fun syncControls() {
        sensitivitySlider.value = settings.lookSensitivity
        invertCheck.isChecked = settings.invertLookY
        crosshairCheck.isChecked = settings.showCrosshair
        joystickCheck.isChecked = settings.showJoystick
        flyCheck.isChecked = settings.voxelFlying
        volumeSlider.value = settings.masterVolume
        soundCheck.isChecked = settings.sfxEnabled
        refreshSensitivity()
        refreshVolume()
    }

    private fun refreshSensitivity() {
        sensitivityValue.setText("×${(settings.lookSensitivity * 100).roundToInt() / 100f}")
    }

    private fun refreshVolume() {
        volumeValue.setText("${(settings.masterVolume * 100).roundToInt()}%")
    }
}
