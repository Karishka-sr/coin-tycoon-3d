package com.tycoon

import com.badlogic.gdx.Game
import com.tycoon.screen.GameScreen

/** Application entry point, shared by the desktop and Android launchers. */
class TycoonGame : Game() {

    override fun create() {
        setScreen(GameScreen())
    }
}
