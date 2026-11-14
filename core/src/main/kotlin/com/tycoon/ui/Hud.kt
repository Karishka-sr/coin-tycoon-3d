package com.tycoon.ui

import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.tycoon.audio.SfxId
import com.tycoon.audio.SoundBank
import com.tycoon.tycoon.Settings
import com.tycoon.tycoon.TycoonState
import com.tycoon.tycoon.Upgrade

/**
 * The in-game interface: a live stat readout, the movement stick, a centre
 * crosshair, the upgrade shop and a button back to the main menu.
 *
 * The stick owns the left half of the screen and the shop owns the top-right;
 * every other overlay is [Touchable.disabled] so drags fall through to the
 * game's look input and the camera can be turned from anywhere else.
 */
class Hud(
    private val skin: Skin,
    private val state: TycoonState,
    private val settings: Settings,
    private val sound: SoundBank,
    onBuy: (Upgrade) -> Unit,
    onMenu: () -> Unit,
) {

    val stage = Stage(ScreenViewport())

    /** Normalised stick output; x strafes right, y moves forward. */
    val stickOutput get() = joystick.output

    private val stats = Label("", skin, "title")
    private val hint = Label(
        "WASD / стик — движение, мышь / свайп — камера, Tab — магазин, Esc — меню",
        skin, "hint",
    )

    private val joystick = TouchJoystick(skin.getDrawable("stick-base"), skin.getDrawable("stick-knob"))
    private val crosshair = Image(skin.getDrawable("crosshair"))

    private val shop = ShopPanel(skin, state, onBuy)
    private val shopOverlay = Table()

    var shopVisible = false
        private set

    init {
        val root = Table()
        root.setFillParent(true)
        root.top().left().pad(12f)
        root.touchable = Touchable.disabled
        root.add(stats).left().row()
        root.add(hint).left().padTop(4f)

        val aim = Table()
        aim.setFillParent(true)
        aim.center()
        aim.touchable = Touchable.disabled
        aim.add(crosshair).size(28f)

        shopOverlay.setFillParent(true)
        shopOverlay.top().right().pad(12f)
        shopOverlay.touchable = Touchable.childrenOnly
        shopOverlay.isVisible = false

        val panel = Table()
        panel.defaults().pad(4f)
        panel.add(shop).row()
        val close = TextButton("Закрыть", skin)
        close.onClick(sound) { toggleShop() }
        panel.add(close).padTop(6f)
        shopOverlay.add(panel)

        val menuButton = TextButton("Меню", skin)
        menuButton.onClick(sound) { onMenu() }
        val topRight = Table()
        topRight.setFillParent(true)
        topRight.top().right().pad(12f)
        topRight.touchable = Touchable.childrenOnly
        topRight.add(menuButton).width(120f)

        stage.addActor(root)
        stage.addActor(aim)
        stage.addActor(joystick)
        stage.addActor(topRight)
        stage.addActor(shopOverlay)
    }

    fun toggleShop() {
        shopVisible = !shopVisible
        shopOverlay.isVisible = shopVisible
    }

    fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
        // The stick gets the left half; the rest of the screen stays free for looking.
        joystick.setBounds(0f, 0f, width * 0.5f, height.toFloat())
    }

    fun render(delta: Float) {
        refresh()
        stage.act(delta)
        stage.draw()
    }

    private fun refresh() {
        stats.setText("${state.coins.toLong()} монет   (+${state.incomePerSecond.toInt()}/с)")
        joystick.isVisible = settings.showJoystick
        crosshair.isVisible = settings.showCrosshair
        if (shopVisible) shop.refresh()
    }

    fun dispose() {
        stage.dispose()
    }
}
