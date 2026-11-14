package com.tycoon.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ScreenUtils
import com.tycoon.TycoonGame
import com.tycoon.audio.SfxId
import com.tycoon.gfx.CoinRenderer
import com.tycoon.gfx.WorldRenderer
import com.tycoon.tycoon.Upgrade
import com.tycoon.ui.Hud
import com.tycoon.world.CoinField
import com.tycoon.world.FirstPersonCamera
import com.tycoon.world.InputController
import com.tycoon.world.InputMath
import com.tycoon.world.LookInput
import com.tycoon.world.Player

/**
 * The gameplay screen: a first-person view over the coin arena plus the upgrade
 * shop.
 *
 * Movement comes from WASD or the on-screen stick and is resolved relative to
 * where the camera looks; the camera itself turns with mouse drag, a swipe
 * outside the stick, or the arrow / QE keys. Escape opens the main menu.
 */
class GameScreen(private val game: TycoonGame) : ScreenAdapter() {

    private val state = game.state
    private val camera = PerspectiveCamera(72f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
    private val firstPerson = FirstPersonCamera(camera)
    private val modelBatch = ModelBatch()

    private val world = WorldRenderer()
    private val coinRenderer = CoinRenderer()

    private val player = Player()
    private val input = InputController()
    private val field = CoinField()

    private val hud = Hud(game.skin, state, game.settings, game.sound, { buy(it) }, { openMenu() })

    private val moveInput = Vector2()
    private val worldMove = Vector2()

    private var timeSinceSave = 0f

    init {
        camera.near = 0.1f
        camera.far = 400f
        firstPerson.eyeHeight = EYE_HEIGHT
        firstPerson.update(player.position, 0f)

        field.fill()
        applyUpgrades()

        val multiplexer = InputMultiplexer()
        multiplexer.addProcessor(hud.stage)
        multiplexer.addProcessor(LookInput(firstPerson))
        multiplexer.addProcessor(object : InputAdapter() {
            override fun keyDown(keycode: Int): Boolean {
                when (keycode) {
                    Input.Keys.TAB -> hud.toggleShop()
                    Input.Keys.ESCAPE -> if (hud.shopVisible) hud.toggleShop() else openMenu()
                    else -> return false
                }
                return true
            }

            override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
                // Desktop right-button drag also turns the view.
                if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
                    firstPerson.rotate(Gdx.input.deltaX.toFloat(), Gdx.input.deltaY.toFloat())
                    return true
                }
                return false
            }
        })
        Gdx.input.inputProcessor = multiplexer
    }

    private fun openMenu() {
        game.saveProgress()
        game.swapLater { game.showMainMenu() }
    }

    private fun buy(upgrade: Upgrade) {
        val bought = state.buy(upgrade)
        game.sound.play(if (bought) SfxId.PURCHASE else SfxId.DENIED)
        if (bought) applyUpgrades()
    }

    private fun applyUpgrades() {
        field.configure(state.capacity, state.spawnInterval, state.coinValue)
        field.setMagnetSpeed(state.magnetSpeed)
        field.setBigCoinChance(state.bigCoinChance)
        player.speed = state.moveSpeed
    }

    override fun render(delta: Float) {
        firstPerson.sensitivity = BASE_LOOK_SENSITIVITY * game.settings.lookSensitivity
        firstPerson.invertY = game.settings.invertLookY

        firstPerson.turn(input.turn(delta))

        moveInput.set(input.movement(hud.stickOutput))
        InputMath.worldMove(moveInput.x, moveInput.y, firstPerson.yaw, worldMove)
        player.update(delta, worldMove)

        val collected = field.update(delta, player.position, state.magnetRadius)
        if (collected > 0.0) {
            game.sound.play(if (field.bigCoinCollected()) SfxId.BIG_COIN else SfxId.COIN_PICKUP)
            state.onCollect(collected * state.coinValueMultiplier)
            hud.pulseCoins()
        }
        state.update(delta)

        firstPerson.update(player.position, player.bobOffset)

        // clearDepth must be true: stale depth values would otherwise reject every fragment.
        ScreenUtils.clear(0.45f, 0.7f, 0.95f, 1f, true)
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)

        modelBatch.begin(camera)
        world.render(modelBatch)
        coinRenderer.render(modelBatch, world.environment, field)
        modelBatch.end()

        hud.render(delta)

        timeSinceSave += delta
        if (timeSinceSave >= SAVE_INTERVAL) {
            timeSinceSave = 0f
            game.saveProgress()
        }
    }

    override fun resize(width: Int, height: Int) {
        camera.viewportWidth = width.toFloat()
        camera.viewportHeight = height.toFloat()
        camera.update()
        hud.resize(width, height)
    }

    override fun pause() {
        game.saveProgress()
    }

    override fun dispose() {
        game.saveProgress()
        modelBatch.dispose()
        world.dispose()
        coinRenderer.dispose()
        hud.dispose()
    }

    private companion object {
        const val SAVE_INTERVAL = 5f
        const val EYE_HEIGHT = 1.7f
        const val BASE_LOOK_SENSITIVITY = 0.18f
    }
}
