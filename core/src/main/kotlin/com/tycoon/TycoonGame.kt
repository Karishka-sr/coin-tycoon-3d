package com.tycoon

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.tycoon.screen.GameScreen
import com.tycoon.screen.MainMenuScreen
import com.tycoon.screen.SettingsScreen
import com.tycoon.screen.ShopScreen
import com.tycoon.tycoon.SaveManager
import com.tycoon.tycoon.Settings
import com.tycoon.tycoon.SettingsManager
import com.tycoon.tycoon.TycoonState
import com.tycoon.ui.UiSkin

/**
 * Application entry point, shared by the desktop and Android launchers.
 *
 * The [skin], the [settings] and the [state] live here because every screen
 * needs them and rebuilding the fonts per screen would be wasteful. Screens are
 * swapped through the `show*` methods; the outgoing screen is disposed, but the
 * shared skin is not, since the incoming screen reuses it.
 */
class TycoonGame : Game() {

    lateinit var skin: Skin
        private set

    lateinit var settings: Settings
        private set

    /** The running economy, carried across visits to the menu. */
    lateinit var state: TycoonState
        private set

    override fun create() {
        skin = UiSkin.create()
        settings = SettingsManager.load()
        state = TycoonState().apply { SaveManager.load()?.let(::restore) }
        swap(MainMenuScreen(this))
    }

    fun showMainMenu() = swap(MainMenuScreen(this))

    fun startGame() = swap(GameScreen(this))

    fun showSettings() = swap(SettingsScreen(this))

    fun showShop() = swap(ShopScreen(this))

    fun saveSettings() = SettingsManager.save(settings)

    fun saveProgress() = SaveManager.save(state)

    fun exit() = Gdx.app.exit()

    /**
     * Defers a screen change to the start of the next frame. Button listeners run
     * inside the current stage's `act`, so swapping (and disposing) that stage
     * from within the callback would leave it walking a disposed tree.
     */
    fun swapLater(next: () -> Unit) = Gdx.app.postRunnable(next)

    private fun swap(next: Screen) {
        val previous = screen
        setScreen(next)
        previous?.dispose()
    }

    override fun dispose() {
        super.dispose()
        skin.dispose()
    }
}
