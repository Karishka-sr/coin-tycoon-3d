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
import com.tycoon.gfx.CoinRenderer
import com.tycoon.gfx.PlayerRenderer
import com.tycoon.gfx.WorldRenderer
import com.tycoon.tycoon.SaveManager
import com.tycoon.tycoon.TycoonState
import com.tycoon.tycoon.Upgrade
import com.tycoon.ui.Hud
import com.tycoon.ui.UiSkin
import com.tycoon.world.ChaseCamera
import com.tycoon.world.CoinField
import com.tycoon.world.InputController
import com.tycoon.world.Player

/** The single gameplay screen: 3D world, coin loop and the upgrade shop. */
class GameScreen : ScreenAdapter() {

    private val state = TycoonState().apply { SaveManager.load()?.let(::restore) }
    private val camera = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
    private val chaseCamera = ChaseCamera(camera)
    private val modelBatch = ModelBatch()

    private val world = WorldRenderer()
    private val playerRenderer = PlayerRenderer()
    private val coinRenderer = CoinRenderer()

    private val player = Player()
    private val input = InputController()
    private val field = CoinField()

    private val skin = UiSkin.create()
    private val hud = Hud(skin, state) { buy(it) }

    private var timeSinceSave = 0f

    init {
        camera.position.set(0f, 13f, 18f)
        camera.lookAt(0f, 1f, 0f)
        camera.near = 0.1f
        camera.far = 400f
        camera.update()

        field.fill()
        applyUpgrades()

        val multiplexer = InputMultiplexer()
        multiplexer.addProcessor(hud.stage)
        multiplexer.addProcessor(object : InputAdapter() {
            override fun keyDown(keycode: Int): Boolean {
                if (keycode == Input.Keys.TAB) {
                    hud.toggleShop()
                    return true
                }
                if (keycode == Input.Keys.ESCAPE && hud.shopVisible) {
                    hud.toggleShop()
                    return true
                }
                return false
            }
        })
        Gdx.input.inputProcessor = multiplexer
    }

    private fun buy(upgrade: Upgrade) {
        if (state.buy(upgrade)) applyUpgrades()
    }

    private fun applyUpgrades() {
        field.configure(state.capacity, state.spawnInterval, state.coinValue)
        field.setMagnetSpeed(state.magnetSpeed)
        field.setBigCoinChance(state.bigCoinChance)
        player.speed = state.moveSpeed
    }

    override fun render(delta: Float) {
        val movement: Vector2 = input.movement()
        player.update(delta, movement)

        val collected = field.update(delta, player.position, state.magnetRadius)
        if (collected > 0.0) {
            state.onCollect(collected * state.coinValueMultiplier)
        }
        state.update(delta)

        chaseCamera.update(delta, player.position, player.headingDegrees)

        // clearDepth must be true: stale depth values would otherwise reject every fragment.
        ScreenUtils.clear(0.45f, 0.7f, 0.95f, 1f, true)
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)

        modelBatch.begin(camera)
        world.render(modelBatch)
        playerRenderer.render(modelBatch, world.environment, player)
        coinRenderer.render(modelBatch, world.environment, field)
        modelBatch.end()

        hud.render(delta)

        timeSinceSave += delta
        if (timeSinceSave >= SAVE_INTERVAL) {
            timeSinceSave = 0f
            SaveManager.save(state)
        }
    }

    override fun resize(width: Int, height: Int) {
        camera.viewportWidth = width.toFloat()
        camera.viewportHeight = height.toFloat()
        camera.update()
        hud.resize(width, height)
    }

    override fun pause() {
        SaveManager.save(state)
    }

    override fun dispose() {
        SaveManager.save(state)
        modelBatch.dispose()
        world.dispose()
        playerRenderer.dispose()
        coinRenderer.dispose()
        hud.dispose()
        skin.dispose()
    }

    private companion object {
        const val SAVE_INTERVAL = 5f
    }
}
