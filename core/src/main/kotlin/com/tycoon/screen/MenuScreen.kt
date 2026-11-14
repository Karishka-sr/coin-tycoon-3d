package com.tycoon.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.tycoon.TycoonGame

/**
 * Shared scaffolding for the menu screens: a gradient background, a centred
 * Scene2D stage and the usual lifecycle wiring.
 *
 * The skin belongs to [TycoonGame], so it is deliberately not disposed here.
 */
abstract class MenuScreen(protected val game: TycoonGame) : ScreenAdapter() {

    protected val skin: Skin get() = game.skin

    protected val stage = Stage(ScreenViewport())

    init {
        val background = Image(skin.getDrawable("menu-bg"))
        background.setFillParent(true)
        stage.addActor(background)
    }

    /** The content table, already centred and padded, for subclasses to fill. */
    protected fun contentTable(): Table {
        val table = Table()
        table.setFillParent(true)
        table.center().pad(24f)
        stage.addActor(table)
        return table
    }

    override fun show() {
        Gdx.input.inputProcessor = stage
    }

    override fun render(delta: Float) {
        ScreenUtils.clear(0.03f, 0.04f, 0.08f, 1f, true)
        stage.act(delta)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    override fun dispose() {
        stage.dispose()
    }
}
