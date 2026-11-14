package com.tycoon

import com.badlogic.gdx.math.Vector3
import com.tycoon.tycoon.TycoonState
import com.tycoon.tycoon.Upgrade
import com.tycoon.world.CoinField
import com.tycoon.world.Player
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Headless checks for the economy and the coin loop; no OpenGL context needed. */
class TycoonStateTest {

    @Test
    fun `upgrade prices grow and purchases deduct coins`() {
        val state = TycoonState()
        state.addCoins(1_000.0)

        val firstCost = state.costOf(Upgrade.COIN_VALUE)
        assertTrue(state.buy(Upgrade.COIN_VALUE))
        assertEquals(1, state.levelOf(Upgrade.COIN_VALUE))
        assertTrue(state.costOf(Upgrade.COIN_VALUE) > firstCost)
        assertEquals(1_000.0 - firstCost, state.coins, 1e-6)
    }

    @Test
    fun `purchase is rejected without enough coins`() {
        val state = TycoonState()
        assertFalse(state.buy(Upgrade.MAGNET))
        assertEquals(0, state.levelOf(Upgrade.MAGNET))
    }

    @Test
    fun `passive income accumulates over time`() {
        val state = TycoonState()
        state.addCoins(1_000.0)
        state.buy(Upgrade.PASSIVE_INCOME)

        val before = state.coins
        state.update(2f)
        assertEquals(before + 2.0, state.coins, 1e-6)
    }

    @Test
    fun `coin value upgrade raises the per-coin payout`() {
        val state = TycoonState()
        val base = state.coinValue
        state.addCoins(1_000.0)
        state.buy(Upgrade.COIN_VALUE)
        assertEquals(base * 1.25, state.coinValue, 1e-6)
    }

    @Test
    fun `upgrades widen the magnet and raise the arena capacity`() {
        val state = TycoonState()
        val radiusBefore = state.magnetRadius
        val capacityBefore = state.capacity

        state.addCoins(10_000.0)
        state.buy(Upgrade.MAGNET)
        state.buy(Upgrade.CAPACITY)

        assertTrue(state.magnetRadius > radiusBefore)
        assertTrue(state.capacity > capacityBefore)
    }
}

class CoinFieldTest {

    private val origin = Vector3(0f, 0f, 0f)

    @Test
    fun `field fills up to capacity and never exceeds it`() {
        val field = CoinField()
        field.configure(10, 0.5f, 1.0)
        field.fill()
        assertEquals(10, field.coins().size)

        repeat(100) { field.update(0.1f, origin, 0f) }
        assertEquals(10, field.coins().size)
    }

    @Test
    fun `coins inside the pickup radius are collected`() {
        val field = CoinField()
        field.configure(1, 100f, 1.0)
        field.fill()

        val coin = field.coins().first()
        coin.position.set(0.2f, CoinField.REST_HEIGHT, 0.2f)

        val collected = field.update(0.016f, origin, 0f)
        assertTrue(collected > 0.0)
        assertEquals(0, field.coins().size)
    }

    @Test
    fun `magnet pulls distant coins towards the player`() {
        val field = CoinField()
        field.configure(1, 100f, 1.0)
        field.fill()
        field.setMagnetSpeed(30f)

        val coin = field.coins().first()
        coin.position.set(5f, CoinField.REST_HEIGHT, 0f)
        val distanceBefore = coin.position.dst(origin)

        field.update(0.05f, origin, 8f)
        assertTrue(coin.position.dst(origin) < distanceBefore)
    }

    @Test
    fun `spawn rate upgrade produces more coins over the same window`() {
        val slow = CoinField().apply { configure(100, 1.0f, 1.0) }
        val fast = CoinField().apply { configure(100, 0.2f, 1.0) }
        slow.fill(); slow.clear(); fast.fill(); fast.clear()

        repeat(100) {
            slow.update(0.05f, origin, 0f)
            fast.update(0.05f, origin, 0f)
        }
        assertTrue(fast.coins().size > slow.coins().size)
    }

    @Test
    fun `player stays inside the arena bounds`() {
        val player = Player()
        repeat(200) { player.update(0.1f, com.badlogic.gdx.math.Vector2(1f, 1f)) }
        assertTrue(player.position.x <= com.tycoon.world.Arena.HALF)
        assertTrue(player.position.z <= com.tycoon.world.Arena.HALF)
    }
}
