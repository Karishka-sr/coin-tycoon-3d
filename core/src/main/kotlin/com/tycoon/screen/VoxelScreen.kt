package com.tycoon.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.ScreenUtils
import com.tycoon.TycoonGame
import com.tycoon.audio.SfxId
import com.tycoon.gfx.VoxelRenderer
import com.tycoon.tycoon.Upgrade
import com.tycoon.ui.VoxelHud
import com.tycoon.voxel.BlockGeometry
import com.tycoon.voxel.BlockType
import com.tycoon.voxel.TerrainGenerator
import com.tycoon.voxel.VoxelInventory
import com.tycoon.voxel.VoxelPlayer
import com.tycoon.voxel.VoxelRaycast
import com.tycoon.voxel.VoxelWorld
import com.tycoon.world.InputMath
import kotlin.random.Random

/**
 * The block world: a Minecraft-like mode where the player walks, mines and builds
 * on procedurally generated terrain.
 *
 * Mining pays coins through the same [com.tycoon.tycoon.TycoonState] the coin
 * arena uses, so the two modes share one economy: dig ore, spend the coins on
 * upgrades, dig faster and richer.
 *
 * The heavy lifting lives in `com.tycoon.voxel` (pure Java, no GL). This screen
 * only drives the camera, feeds input into [VoxelPlayer] and hands the world to
 * [VoxelRenderer].
 */
class VoxelScreen(private val game: TycoonGame) : ScreenAdapter() {

    private val state = game.state

    private val camera = PerspectiveCamera(75f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
    private val modelBatch = ModelBatch()

    private val world = VoxelWorld()
    private val player = VoxelPlayer()
    private val inventory = VoxelInventory()
    private val raycast = VoxelRaycast()
    private val renderer = VoxelRenderer(world)

    private val hud = VoxelHud(
        game.skin, state, game.settings, game.sound, inventory,
        { mine() }, { place() }, { player.jump() }, { toggleFly() }, { buy(it) }, { openMenu() },
    )

    val environment = Environment().apply {
        set(ColorAttribute(ColorAttribute.AmbientLight, 0.62f, 0.64f, 0.68f, 1f))
        add(DirectionalLight().apply {
            color.set(1f, 0.97f, 0.9f, 1f)
            direction.set(-0.5f, -1f, -0.4f).nor()
        })
    }

    private val moveInput = Vector2()
    private val worldMove = Vector2()
    private val eye = Vector3()
    private val look = Vector3()

    private var lookPointer = -1
    private var lastX = 0f
    private var lastY = 0f

    private var actionCooldown = 0f
    private var timeSinceSave = 0f

    init {
        camera.near = 0.05f
        camera.far = 500f

        val seed = if (state.voxelSeed != 0L) state.voxelSeed else Random.nextLong().also { state.voxelSeed = it }
        TerrainGenerator(seed).generate(world)
        replayEdits()

        inventory.restore(state.voxelInventory)
        if (state.voxelInventory.isEmpty()) {
            inventory.fillStartingKit()
        }

        player.spawn(world, VoxelWorld.SIZE_X / 2f + 0.5f, VoxelWorld.SIZE_Z / 2f + 0.5f)
        player.flying = game.settings.voxelFlying

        Gdx.input.inputProcessor = InputMultiplexer(hud.stage, VoxelInput())
        syncCamera()
    }

    /** Replays the saved block edits on top of the freshly generated terrain. */
    private fun replayEdits() {
        val edits = state.voxelEdits
        var i = 0
        while (i + 3 < edits.size) {
            world.set(edits[i], edits[i + 1], edits[i + 2], BlockType.byId(edits[i + 3]))
            i += 4
        }
    }

    private fun openMenu() {
        saveVoxelState()
        game.saveProgress()
        game.swapLater { game.showMainMenu() }
    }

    private fun buy(upgrade: Upgrade) {
        val bought = state.buy(upgrade)
        game.sound.play(if (bought) SfxId.PURCHASE else SfxId.DENIED)
    }

    private fun toggleFly() {
        game.settings.voxelFlying = !game.settings.voxelFlying
        player.flying = game.settings.voxelFlying
        game.saveSettings()
        hud.onFlyChanged()
    }

    // ---- block interaction -------------------------------------------------

    private fun targetBlock(): Boolean {
        player.eye(eye)
        player.look(look)
        return raycast.cast(world, eye.x, eye.y, eye.z, look.x, look.y, look.z, REACH)
    }

    private fun mine() {
        if (actionCooldown > 0f) return
        if (!targetBlock()) return

        val type = world.get(raycast.blockX, raycast.blockY, raycast.blockZ)
        if (type == BlockType.AIR || type == BlockType.BEDROCK) return

        actionCooldown = ACTION_DELAY
        world.setAndRecord(raycast.blockX, raycast.blockY, raycast.blockZ, BlockType.AIR)
        markDirtyAround(raycast.blockX, raycast.blockZ)

        inventory.add(type.drop(), 1)
        val value = type.coinValue() * state.coinValueMultiplier
        if (value > 0.0) state.onCollect(value)

        game.sound.play(SfxId.BLOCK_DIG)
    }

    private fun place() {
        if (actionCooldown > 0f) return
        if (!targetBlock()) return

        val block = inventory.selectedBlock()
        if (!inventory.consumeSelected()) {
            game.sound.play(SfxId.DENIED)
            return
        }

        val x = raycast.placementX()
        val y = raycast.placementY()
        val z = raycast.placementZ()

        if (!VoxelWorld.inBounds(x, y, z) || !world.isAir(x, y, z) || intersectsPlayer(x, y, z)) {
            // Put the block back: the placement was refused, not consumed.
            inventory.add(block, 1)
            return
        }

        actionCooldown = ACTION_DELAY
        world.setAndRecord(x, y, z, block)
        markDirtyAround(x, z)
        game.sound.play(SfxId.BLOCK_PLACE)
    }

    /** True when placing at these coordinates would trap the player inside a block. */
    private fun intersectsPlayer(x: Int, y: Int, z: Int): Boolean {
        val half = VoxelPlayer.WIDTH / 2f
        return player.position.x + half > x && player.position.x - half < x + 1 &&
            player.position.y + VoxelPlayer.HEIGHT > y && player.position.y < y + 1 &&
            player.position.z + half > z && player.position.z - half < z + 1
    }

    /** A block on a chunk border also changes its neighbour's visible faces. */
    private fun markDirtyAround(x: Int, z: Int) {
        renderer.markDirty(x, z)
        if (x % BlockGeometry.CHUNK_SIZE == 0) renderer.markDirty(x - 1, z)
        if (x % BlockGeometry.CHUNK_SIZE == BlockGeometry.CHUNK_SIZE - 1) renderer.markDirty(x + 1, z)
        if (z % BlockGeometry.CHUNK_SIZE == 0) renderer.markDirty(x, z - 1)
        if (z % BlockGeometry.CHUNK_SIZE == BlockGeometry.CHUNK_SIZE - 1) renderer.markDirty(x, z + 1)
    }

    // ---- frame -------------------------------------------------------------

    override fun render(delta: Float) {
        actionCooldown = maxOf(0f, actionCooldown - delta)

        turnFromKeys(delta)

        moveInput.set(movement())
        InputMath.worldMove(moveInput.x, moveInput.y, player.yaw, worldMove)

        val speed = MOVE_SPEED * delta
        val vertical = when {
            player.flying && Gdx.input.isKeyPressed(Input.Keys.SPACE) -> 1f
            player.flying && Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) -> -1f
            else -> 0f
        }
        player.update(world, delta, worldMove.x * speed, worldMove.y * speed, vertical)

        renderer.flush()
        syncCamera()

        ScreenUtils.clear(0.55f, 0.72f, 0.95f, 1f, true)
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)

        modelBatch.begin(camera)
        renderer.render(modelBatch, environment)
        modelBatch.end()

        hud.render(delta)

        timeSinceSave += delta
        if (timeSinceSave >= SAVE_INTERVAL) {
            timeSinceSave = 0f
            saveVoxelState()
            game.saveProgress()
        }
    }

    private fun syncCamera() {
        player.eye(eye)
        player.look(look)
        camera.position.set(eye)
        camera.direction.set(look)
        camera.up.set(Vector3.Y)
        camera.update()
    }

    /** Keyboard movement, plus the on-screen stick when it is shown. */
    private fun movement(): Vector2 {
        val out = KEYBOARD_MOVE
        out.setZero()
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) out.y += 1f
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) out.y -= 1f
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) out.x -= 1f
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) out.x += 1f
        out.add(hud.stickOutput)
        if (out.len2() > 1f) out.nor()
        return out
    }

    private fun turnFromKeys(delta: Float) {
        var turn = 0f
        if (Gdx.input.isKeyPressed(Input.Keys.Q)) turn += 1f
        if (Gdx.input.isKeyPressed(Input.Keys.E)) turn -= 1f
        if (turn != 0f) {
            player.yaw = wrap(player.yaw + turn * KEY_TURN_SPEED * delta)
        }
    }

    override fun resize(width: Int, height: Int) {
        camera.viewportWidth = width.toFloat()
        camera.viewportHeight = height.toFloat()
        camera.update()
        hud.resize(width, height)
    }

    override fun pause() {
        saveVoxelState()
        game.saveProgress()
    }

    override fun dispose() {
        saveVoxelState()
        game.saveProgress()
        modelBatch.dispose()
        renderer.dispose()
        hud.dispose()
    }

    /** Copies the world edits and hotbar into [state] so they reach the save file. */
    private fun saveVoxelState() {
        val edits = world.edits()
        val flat = IntArray(edits.size * 4)
        for (i in edits.indices) {
            val edit = edits[i]
            flat[i * 4] = edit[0]
            flat[i * 4 + 1] = edit[1]
            flat[i * 4 + 2] = edit[2]
            flat[i * 4 + 3] = edit[3]
        }
        state.voxelEdits = flat
        state.voxelInventory = inventory.snapshot()
    }

    /** Mouse, touch and key input for the block world. */
    private inner class VoxelInput : InputAdapter() {

        override fun keyDown(keycode: Int): Boolean {
            when (keycode) {
                Input.Keys.SPACE -> {
                    if (!player.flying) player.jump()
                    return true
                }
                Input.Keys.F -> {
                    toggleFly()
                    return true
                }
                Input.Keys.TAB -> {
                    hud.toggleShop()
                    return true
                }
                Input.Keys.ESCAPE -> {
                    if (hud.shopVisible) hud.toggleShop() else openMenu()
                    return true
                }
            }

            if (keycode >= Input.Keys.NUM_1 && keycode <= Input.Keys.NUM_9) {
                inventory.select(keycode - Input.Keys.NUM_1)
                return true
            }
            return false
        }

        override fun scrolled(amountX: Float, amountY: Float): Boolean {
            if (amountY > 0f) inventory.selectNext() else if (amountY < 0f) inventory.selectPrevious()
            return true
        }

        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            if (button == Input.Buttons.LEFT) {
                mine()
            } else if (button == Input.Buttons.RIGHT) {
                place()
            }
            if (lookPointer == -1) {
                lookPointer = pointer
                lastX = screenX.toFloat()
                lastY = screenY.toFloat()
            }
            return true
        }

        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            if (pointer != lookPointer) return false
            val sensitivity = BASE_LOOK_SENSITIVITY * game.settings.lookSensitivity
            player.yaw = wrap(player.yaw - (screenX - lastX) * sensitivity)
            val dy = if (game.settings.invertLookY) (screenY - lastY) else -(screenY - lastY)
            player.pitch = MathUtils.clamp(player.pitch + dy * sensitivity, -PITCH_LIMIT, PITCH_LIMIT)
            lastX = screenX.toFloat()
            lastY = screenY.toFloat()
            return true
        }

        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            if (pointer == lookPointer) lookPointer = -1
            return true
        }
    }

    private companion object {
        const val REACH = 6f
        const val MOVE_SPEED = 9f
        const val ACTION_DELAY = 0.18f
        const val SAVE_INTERVAL = 5f
        const val BASE_LOOK_SENSITIVITY = 0.18f
        const val KEY_TURN_SPEED = 140f
        const val PITCH_LIMIT = 88f

        val KEYBOARD_MOVE = Vector2()

        fun wrap(degrees: Float): Float = ((degrees % 360f) + 360f) % 360f
    }
}
