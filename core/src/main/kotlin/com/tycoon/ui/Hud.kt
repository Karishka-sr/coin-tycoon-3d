package com.tycoon.ui

import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.tycoon.tycoon.TycoonState
import com.tycoon.tycoon.Upgrade
import java.util.EnumMap

/**
 * The on-screen interface: a live stat readout in the top-left corner and the
 * upgrade shop on the right. Labels are refreshed each frame so the numbers
 * stay current.
 */
class Hud(
    private val skin: Skin,
    private val state: TycoonState,
    private val onBuy: (Upgrade) -> Unit,
) {

    val stage = Stage(ScreenViewport())

    private val stats = Label("", skin, "title")
    private val hint = Label(
        "WASD / стрелки — движение, ПКМ — камера, Tab — магазин",
        skin, "hint",
    )

    private val shop = Table()
    private val rows = EnumMap<Upgrade, TextButton>(Upgrade::class.java)

    var shopVisible = false
        private set

    init {
        val root = Table()
        root.setFillParent(true)
        root.top().left().pad(12f)
        root.add(stats).left().row()
        root.add(hint).left().padTop(4f)

        shop.setFillParent(true)
        shop.top().right().pad(12f)
        shop.isVisible = false

        buildShop()

        stage.addActor(root)
        stage.addActor(shop)
    }

    private fun buildShop() {
        val panel = Table()
        panel.background = skin.newDrawable("white", Color(0f, 0f, 0f, 0.55f))
        panel.defaults().pad(4f)
        panel.add(Label("Магазин улучшений", skin, "title")).colspan(2).padBottom(8f).row()

        for (upgrade in Upgrade.entries) {
            val button = TextButton("", skin, "shop")
            button.addListener { _ -> onBuy(upgrade); true }

            val label = Label(upgrade.description, skin, "hint")
            label.wrap = true

            panel.add(button).width(320f).left()
            panel.add(label).width(320f).left().row()
            rows[upgrade] = button
        }

        val toggle = TextButton("Закрыть", skin)
        toggle.addListener { _ -> toggleShop(); true }
        panel.add(toggle).colspan(2).padTop(10f).row()

        shop.add(panel)
    }

    fun toggleShop() {
        shopVisible = !shopVisible
        shop.isVisible = shopVisible
    }

    fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    fun render(delta: Float) {
        refresh()
        stage.act(delta)
        stage.draw()
    }

    private fun refresh() {
        stats.setText("${state.coins.toLong()} монет   (+${state.incomePerSecond.toInt()}/с)")
        for (upgrade in Upgrade.entries) {
            val level = state.levelOf(upgrade)
            val cost = state.costOf(upgrade).toLong()
            rows.getValue(upgrade).setText("${upgrade.title}  ур.$level  —  $cost")
        }
    }

    fun dispose() {
        stage.dispose()
    }
}
