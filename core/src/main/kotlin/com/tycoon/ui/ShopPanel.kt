package com.tycoon.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.Align
import com.tycoon.tycoon.TycoonState
import com.tycoon.tycoon.Upgrade
import java.util.EnumMap

/**
 * The upgrade list, shared by the in-game shop overlay and the shop screen in the
 * main menu.
 *
 * It only renders state and reports purchases through [onBuy]; refreshing is
 * driven by the owner, so the same panel works while the game is paused.
 */
class ShopPanel(
    private val skin: Skin,
    private val state: TycoonState,
    private val onBuy: (Upgrade) -> Unit,
) : Table() {

    private val rows = EnumMap<Upgrade, TextButton>(Upgrade::class.java)

    init {
        background = skin.newDrawable("white", Color(0f, 0f, 0f, 0.55f))
        defaults().pad(4f)
        add(Label("Магазин улучшений", skin, "title")).colspan(2).padBottom(8f).row()

        for (upgrade in Upgrade.entries) {
            val button = TextButton("", skin, "shop")
            button.onClick { onBuy(upgrade) }

            val label = Label(upgrade.description, skin, "hint")
            label.wrap = true
            label.setAlignment(Align.left)

            add(button).width(320f).left()
            add(label).width(320f).left().row()
            rows[upgrade] = button
        }
    }

    /** Pulls the current levels and prices out of [TycoonState]. */
    fun refresh() {
        for (upgrade in Upgrade.entries) {
            val level = state.levelOf(upgrade)
            val cost = state.costOf(upgrade).toLong()
            val button = rows.getValue(upgrade)
            button.setText("${upgrade.title}  ур.$level  —  $cost")
            button.isDisabled = !state.canAfford(upgrade)
        }
    }
}
