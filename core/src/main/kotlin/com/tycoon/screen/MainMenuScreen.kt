package com.tycoon.screen

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.tycoon.ui.onClick
import com.tycoon.TycoonGame

/** The title screen: play, options, the upgrade shop, or quit. */
class MainMenuScreen(game: TycoonGame) : MenuScreen(game) {

    init {
        val table = contentTable()
        table.defaults().width(360f).pad(6f)

        table.add(Label("Coin Tycoon 3D", skin, "title")).padBottom(4f).row()
        table.add(Label(progressLine(), skin, "hint")).padBottom(20f).row()

        table.add(button("Играть") { game.swapLater { game.startGame() } }).height(56f).row()
        table.add(button("Мир блоков") { game.swapLater { game.showVoxelWorld() } }).height(56f).row()
        table.add(button("Настройки") { game.swapLater { game.showSettings() } }).height(56f).row()
        table.add(button("Магазин") { game.swapLater { game.showShop() } }).height(56f).row()
        table.add(button("Выход") { game.swapLater { game.exit() } }).height(56f).row()
    }

    private fun button(text: String, action: () -> Unit): TextButton =
        TextButton(text, skin).apply { onClick(game.sound, action = action) }

    private fun progressLine(): String {
        val state = game.state
        val earned = state.totalEarned.toLong()
        val minutes = (state.playtime / 60f).toInt()
        return "Всего заработано: $earned   ·   собрано монет: ${state.coinsCollected}   ·   в игре: $minutes мин"
    }
}
