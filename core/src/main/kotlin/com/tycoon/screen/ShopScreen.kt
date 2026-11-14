package com.tycoon.screen

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.tycoon.ui.onClick
import com.tycoon.TycoonGame
import com.tycoon.tycoon.Upgrade
import com.tycoon.ui.ShopPanel

/**
 * The upgrade shop, reachable from the main menu so upgrades can be bought
 * before a run starts. It shares [ShopPanel] with the in-game overlay.
 */
class ShopScreen(game: TycoonGame) : MenuScreen(game) {

    private val coins = Label("", skin, "title")
    private val panel = ShopPanel(skin, game.state) { buy(it) }

    init {
        val table = contentTable()
        table.defaults().pad(6f)

        table.add(Label("Магазин", skin, "title")).row()
        table.add(coins).padBottom(8f).row()
        table.add(panel).row()

        val back = TextButton("Назад", skin)
        back.onClick { game.swapLater { game.showMainMenu() } }
        table.add(back).width(360f).height(52f).padTop(10f).row()

        refresh()
    }

    private fun buy(upgrade: Upgrade) {
        game.state.buy(upgrade)
        game.saveProgress()
        refresh()
    }

    private fun refresh() {
        coins.setText("${game.state.coins.toLong()} монет")
        panel.refresh()
    }
}
