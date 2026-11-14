package com.tycoon.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.tycoon.audio.SoundBank
import com.tycoon.tycoon.Settings
import com.tycoon.tycoon.TycoonState
import com.tycoon.tycoon.Upgrade
import com.tycoon.voxel.BlockType
import com.tycoon.voxel.VoxelInventory

/**
 * The block-world interface: the stat readout, the movement stick, a crosshair,
 * the hotbar and the touch buttons for mining, building, jumping and flying.
 *
 * The layout mirrors [Hud]: the stick owns the left half, every passive overlay is
 * [Touchable.disabled] so drags fall through to the look input, and only the
 * buttons actually swallow touches.
 */
class VoxelHud(
    private val skin: Skin,
    private val state: TycoonState,
    private val settings: Settings,
    private val sound: SoundBank,
    private val inventory: VoxelInventory,
    private val onMine: () -> Unit,
    private val onPlace: () -> Unit,
    private val onJump: () -> Unit,
    private val onToggleFly: () -> Unit,
    private val onBuy: (Upgrade) -> Unit,
    private val onMenu: () -> Unit,
) {

    val stage = Stage(ScreenViewport())

    val stickOutput get() = joystick.output

    private val stats = Label("", skin, "title")
    private val hint = Label("", skin, "hint")
    private val crosshair = Image(skin.getDrawable("crosshair"))

    private val joystick = TouchJoystick(skin.getDrawable("stick-base"), skin.getDrawable("stick-knob"))

    private val slots = ArrayList<HotbarSlot>(VoxelInventory.HOTBAR.size)
    private val flyButton = TextButton("Полёт", skin)
    private val shop = ShopPanel(skin, state, onBuy)
    private val shopOverlay = Table()

    var shopVisible = false
        private set

    init {
        val root = Table()
        root.setFillParent(true)
        root.top().left().pad(12f)
        root.touchable = Touchable.disabled
        root.add(stats).left().row()
        root.add(hint).left().padTop(4f)

        val aim = Table()
        aim.setFillParent(true)
        aim.center()
        aim.touchable = Touchable.disabled
        aim.add(crosshair).size(28f)

        val hotbar = Table()
        hotbar.setFillParent(true)
        hotbar.bottom().pad(16f)
        hotbar.touchable = Touchable.childrenOnly
        for (i in VoxelInventory.HOTBAR.indices) {
            val slot = HotbarSlot(i)
            slots.add(slot)
            hotbar.add(slot.button).size(56f).pad(3f)
        }

        val actions = Table()
        actions.setFillParent(true)
        actions.right().pad(16f)
        actions.defaults().pad(4f).width(150f).height(52f)
        actions.touchable = Touchable.childrenOnly

        val mine = TextButton("Копать", skin)
        mine.onClick(sound, action = onMine)
        val place = TextButton("Ставить", skin)
        place.onClick(sound, action = onPlace)
        val jump = TextButton("Прыжок", skin)
        jump.onClick(sound, action = onJump)
        flyButton.onClick(sound) {
            onToggleFly()
            refreshFlyButton()
        }
        val shopButton = TextButton("Магазин", skin)
        shopButton.onClick(sound) { toggleShop() }
        val menuButton = TextButton("Меню", skin)
        menuButton.onClick(sound, action = onMenu)

        actions.add(mine).row()
        actions.add(place).row()
        actions.add(jump).row()
        actions.add(flyButton).row()
        actions.add(shopButton).row()
        actions.add(menuButton).row()

        shopOverlay.setFillParent(true)
        shopOverlay.top().right().pad(12f)
        shopOverlay.touchable = Touchable.childrenOnly
        shopOverlay.isVisible = false
        val panel = Table()
        panel.defaults().pad(4f)
        panel.add(shop).row()
        val close = TextButton("Закрыть", skin)
        close.onClick(sound) { toggleShop() }
        panel.add(close).padTop(6f)
        shopOverlay.add(panel)

        stage.addActor(root)
        stage.addActor(aim)
        stage.addActor(joystick)
        stage.addActor(hotbar)
        stage.addActor(actions)
        stage.addActor(shopOverlay)

        refreshFlyButton()
    }

    fun toggleShop() {
        shopVisible = !shopVisible
        shopOverlay.isVisible = shopVisible
    }

    fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
        joystick.setBounds(0f, 0f, width * 0.5f, height.toFloat())
    }

    fun render(delta: Float) {
        refresh()
        stage.act(delta)
        stage.draw()
    }

    /** Notified when the player toggles flight with the keyboard, to relabel the button. */
    fun onFlyChanged() = refreshFlyButton()

    private fun refreshFlyButton() {
        flyButton.setText(if (settings.voxelFlying) "Полёт: вкл" else "Полёт: выкл")
    }

    private fun refresh() {
        stats.setText("${state.coins.toLong()} монет   (+${state.incomePerSecond.toInt()}/с)")
        hint.setText("WASD / стик — ходьба, мышь / свайп — камера, ЛКМ — копать, ПКМ — ставить, Space — прыжок, F — полёт, Tab — магазин")
        joystick.isVisible = settings.showJoystick
        crosshair.isVisible = settings.showCrosshair
        for (slot in slots) slot.refresh()
        if (shopVisible) shop.refresh()
    }

    fun dispose() {
        stage.dispose()
    }

    /** One hotbar entry: a colour swatch, the block name and the count held. */
    private inner class HotbarSlot(private val index: Int) {

        private val block: BlockType = VoxelInventory.HOTBAR[index]
        val button = TextButton("", skin, "shop")
        private var listening = true

        init {
            button.addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: com.badlogic.gdx.scenes.scene2d.Actor?) {
                    if (listening) inventory.select(index)
                }
            })
        }

        fun refresh() {
            listening = false
            button.isChecked = inventory.selectedIndex() == index
            listening = true
            button.setText("${shortName(block)}\n×${inventory.countAt(index)}")
            button.color = Color(1f, 1f, 1f, if (inventory.countAt(index) > 0) 1f else 0.45f)
        }
    }

    private companion object {
        fun shortName(block: BlockType): String = when (block) {
            BlockType.GRASS -> "Земля"
            BlockType.DIRT -> "Грунт"
            BlockType.STONE -> "Камень"
            BlockType.COBBLESTONE -> "Булыж."
            BlockType.SAND -> "Песок"
            BlockType.WOOD -> "Дерево"
            BlockType.PLANK -> "Доски"
            BlockType.BRICK -> "Кирпич"
            BlockType.GLASS -> "Стекло"
            else -> block.name
        }
    }
}
