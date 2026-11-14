package com.tycoon.tycoon

import java.util.EnumMap
import kotlin.math.max
import kotlin.math.pow

/**
 * The whole economy of the game: the coin balance, the upgrade levels and every
 * value derived from them. Pure state, no rendering and no LibGDX dependency
 * beyond what [CoinField] needs, so it is cheap to unit-test.
 */
class TycoonState {

    private val levels = EnumMap<Upgrade, Int>(Upgrade::class.java)

    var coins: Double = 0.0
        private set

    var totalEarned: Double = 0.0
        private set

    var coinsCollected: Long = 0
        private set

    var playtime: Float = 0f
        private set

    /**
     * Seed of the voxel world. The terrain is regenerated from it on load and the
     * player's edits are replayed on top, which keeps the save file small.
     */
    var voxelSeed: Long = 0L

    /** Player edits to the voxel world, flattened as x, y, z, block id quads. */
    var voxelEdits: IntArray = IntArray(0)

    /** Hotbar contents, in [com.tycoon.voxel.VoxelInventory.HOTBAR] order. */
    var voxelInventory: IntArray = IntArray(0)

    init {
        Upgrade.entries.forEach { levels[it] = 0 }
    }

    fun levelOf(upgrade: Upgrade): Int = levels[upgrade] ?: 0

    fun costOf(upgrade: Upgrade): Double =
        upgrade.baseCost * upgrade.costGrowth.pow(levelOf(upgrade).toDouble())

    fun canAfford(upgrade: Upgrade): Boolean = coins >= costOf(upgrade)

    fun buy(upgrade: Upgrade): Boolean {
        if (!canAfford(upgrade)) return false
        coins -= costOf(upgrade)
        levels[upgrade] = levelOf(upgrade) + 1
        return true
    }

    fun onCollect(value: Double) {
        if (value <= 0.0) return
        addCoins(value)
        coinsCollected++
    }

    fun addCoins(amount: Double) {
        if (amount <= 0.0) return
        coins += amount
        totalEarned += amount
    }

    /** Advances timers and pays out the passive income. */
    fun update(delta: Float) {
        playtime += delta
        val income = passiveIncome
        if (income > 0.0) addCoins(income * delta)
    }

    // ---- values derived from upgrade levels ---------------------------------

    val coinValue: Double get() = BASE_COIN_VALUE * coinValueMultiplier
    val coinValueMultiplier: Double get() = 1.0 + 0.25 * levelOf(Upgrade.COIN_VALUE)

    val spawnInterval: Float
        get() = max(MIN_SPAWN_INTERVAL, 1.0f / (1.0f + 0.22f * levelOf(Upgrade.SPAWN_RATE)))

    val magnetRadius: Float get() = 2.5f + 1.1f * levelOf(Upgrade.MAGNET)
    val magnetSpeed: Float get() = 12f + 2.5f * levelOf(Upgrade.MAGNET)

    val capacity: Int get() = 12 + 4 * levelOf(Upgrade.CAPACITY)
    val passiveIncome: Double get() = 1.0 * levelOf(Upgrade.PASSIVE_INCOME)
    val moveSpeed: Float get() = 10f + 1.4f * levelOf(Upgrade.MOVE_SPEED)
    val bigCoinChance: Double get() = 0.04 + 0.03 * levelOf(Upgrade.LUCK)

    val totalLevels: Int get() = levels.values.sum()
    val incomePerSecond: Double get() = passiveIncome

    // ---- persistence -------------------------------------------------------

    fun snapshot(): SaveData = SaveData().also { data ->
        data.coins = coins
        data.totalEarned = totalEarned
        data.coinsCollected = coinsCollected
        data.playtime = playtime
        data.voxelSeed = voxelSeed
        data.voxelEdits = voxelEdits
        data.voxelInventory = voxelInventory
        Upgrade.entries.forEach { data.levels[it.name] = levelOf(it) }
    }

    fun restore(data: SaveData) {
        coins = data.coins
        totalEarned = data.totalEarned
        coinsCollected = data.coinsCollected
        playtime = data.playtime
        voxelSeed = data.voxelSeed
        voxelEdits = data.voxelEdits
        voxelInventory = data.voxelInventory
        Upgrade.entries.forEach { levels[it] = data.levels[it.name] ?: 0 }
    }

    companion object {
        const val BASE_COIN_VALUE = 1.0
        const val MIN_SPAWN_INTERVAL = 0.12f
        const val BIG_COIN_MULTIPLIER = 5.0
    }
}
