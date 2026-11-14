package com.tycoon

import com.tycoon.voxel.BlockGeometry
import com.tycoon.voxel.BlockType
import com.tycoon.voxel.TerrainGenerator
import com.tycoon.voxel.VoxelInventory
import com.tycoon.voxel.VoxelMesh
import com.tycoon.voxel.VoxelPlayer
import com.tycoon.voxel.VoxelRaycast
import com.tycoon.voxel.VoxelWorld
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

/** Headless checks for the block world: terrain, raycasting, physics and meshing. */
class VoxelWorldTest {

    @Test
    fun `out of bounds reads are air and the floor below is solid`() {
        val world = VoxelWorld()
        assertSame(BlockType.AIR, world.get(-1, 5, 0))
        assertSame(BlockType.AIR, world.get(VoxelWorld.SIZE_X, 5, 0))
        assertSame(BlockType.AIR, world.get(0, VoxelWorld.SIZE_Y, 0))
        assertFalse(world.isSolid(0, VoxelWorld.SIZE_Y, 0))
        assertTrue(world.isSolid(0, -1, 0))
    }

    @Test
    fun `set and get round trip and edits are recorded`() {
        val world = VoxelWorld()
        world.setAndRecord(3, 4, 5, BlockType.GOLD_ORE)
        assertSame(BlockType.GOLD_ORE, world.get(3, 4, 5))

        val edits = world.edits()
        assertEquals(1, edits.size)
        assertEquals(listOf(3, 4, 5, BlockType.GOLD_ORE.ordinal), edits[0].toList())
    }

    @Test
    fun `highest solid y finds the surface of a column`() {
        val world = VoxelWorld()
        world.set(1, 1, 1, BlockType.STONE)
        world.set(1, 7, 1, BlockType.STONE)
        assertEquals(7, world.highestSolidY(1, 1))
    }

    @Test
    fun `box intersection detects overlap with a solid block`() {
        val world = VoxelWorld()
        world.set(2, 0, 2, BlockType.STONE)
        assertTrue(world.intersectsSolid(1.6f, 0f, 1.6f, 2.4f, 1f, 2.4f))
        assertFalse(world.intersectsSolid(3.6f, 0f, 3.6f, 4.4f, 1f, 4.4f))
    }
}

class TerrainGeneratorTest {

    @Test
    fun `generation is deterministic for a seed`() {
        val a = VoxelWorld()
        val b = VoxelWorld()
        TerrainGenerator(1234L).generate(a)
        TerrainGenerator(1234L).generate(b)

        for (y in 0 until VoxelWorld.SIZE_Y step 5) {
            for (x in 0 until VoxelWorld.SIZE_X step 7) {
                for (z in 0 until VoxelWorld.SIZE_Z step 7) {
                    assertSame(a.get(x, y, z), b.get(x, y, z), "block at $x,$y,$z")
                }
            }
        }
    }

    @Test
    fun `different seeds give different terrain`() {
        val a = VoxelWorld()
        val b = VoxelWorld()
        TerrainGenerator(1L).generate(a)
        TerrainGenerator(2L).generate(b)

        var differences = 0
        for (x in 0 until VoxelWorld.SIZE_X) {
            for (z in 0 until VoxelWorld.SIZE_Z) {
                if (a.get(x, VoxelWorld.SEA_LEVEL, z) != b.get(x, VoxelWorld.SEA_LEVEL, z)) {
                    differences++
                }
            }
        }
        assertTrue(differences > 100, "expected visibly different worlds, got $differences")
    }

    @Test
    fun `every column has bedrock at the bottom and something above it`() {
        val world = VoxelWorld()
        TerrainGenerator(42L).generate(world)

        for (x in 0 until VoxelWorld.SIZE_X step 3) {
            for (z in 0 until VoxelWorld.SIZE_Z step 3) {
                assertSame(BlockType.BEDROCK, world.get(x, 0, z))
                assertTrue(world.highestSolidY(x, z) > 0, "column $x,$z is empty")
            }
        }
    }

    @Test
    fun `surface stays inside the world and ore only appears underground`() {
        val generator = TerrainGenerator(7L)
        val world = VoxelWorld()
        generator.generate(world)

        for (x in 0 until VoxelWorld.SIZE_X) {
            for (z in 0 until VoxelWorld.SIZE_Z) {
                val height = generator.surfaceHeight(x, z)
                assertTrue(height in 4 until VoxelWorld.SIZE_Y, "height $height out of range")
            }
        }

        val ores = setOf(
            BlockType.COAL_ORE, BlockType.IRON_ORE, BlockType.GOLD_ORE, BlockType.DIAMOND_ORE,
        )
        for (x in 0 until VoxelWorld.SIZE_X step 2) {
            for (z in 0 until VoxelWorld.SIZE_Z step 2) {
                val top = generator.surfaceHeight(x, z)
                for (y in top until VoxelWorld.SIZE_Y) {
                    assertFalse(world.get(x, y, z) in ores, "ore above the surface at $x,$y,$z")
                }
            }
        }
    }

    @Test
    fun `water fills the columns below sea level`() {
        val world = VoxelWorld()
        TerrainGenerator(99L).generate(world)

        var waterBlocks = 0
        for (x in 0 until VoxelWorld.SIZE_X) {
            for (z in 0 until VoxelWorld.SIZE_Z) {
                if (world.get(x, VoxelWorld.SEA_LEVEL, z) == BlockType.WATER) {
                    waterBlocks++
                }
            }
        }
        assertTrue(waterBlocks > 0, "expected at least some water in the world")
    }
}

class VoxelRaycastTest {

    @Test
    fun `a ray down hits the top face of the block below`() {
        val world = VoxelWorld()
        world.set(5, 3, 5, BlockType.STONE)

        val ray = VoxelRaycast()
        assertTrue(ray.cast(world, 5.5f, 8f, 5.5f, 0f, -1f, 0f, 16f))
        assertEquals(5, ray.blockX)
        assertEquals(3, ray.blockY)
        assertEquals(5, ray.blockZ)
        assertEquals(BlockGeometry.TOP, ray.face)
        // The block at y=3 spans 3..4, so the ray from y=8 travels 4 blocks.
        assertEquals(4.0f, ray.distance, 1e-3f)
    }

    @Test
    fun `a horizontal ray reports the face it entered through`() {
        val world = VoxelWorld()
        world.set(10, 5, 5, BlockType.STONE)

        val ray = VoxelRaycast()
        assertTrue(ray.cast(world, 0.5f, 5.5f, 5.5f, 1f, 0f, 0f, 32f))
        assertEquals(BlockGeometry.NEG_X, ray.face)
        assertEquals(9, ray.placementX())
        assertEquals(5, ray.placementY())
        assertEquals(5, ray.placementZ())
    }

    @Test
    fun `a ray that finds nothing reports no hit`() {
        val world = VoxelWorld()
        val ray = VoxelRaycast()
        assertFalse(ray.cast(world, 0.5f, 40f, 0.5f, 1f, 0f, 0f, 5f))
        assertFalse(ray.hit)
        assertEquals(VoxelRaycast.NO_FACE, ray.face)
    }

    @Test
    fun `reach limits how far the ray searches`() {
        val world = VoxelWorld()
        world.set(30, 5, 5, BlockType.STONE)

        val ray = VoxelRaycast()
        assertFalse(ray.cast(world, 0.5f, 5.5f, 5.5f, 1f, 0f, 0f, 10f))
        assertTrue(ray.cast(world, 0.5f, 5.5f, 5.5f, 1f, 0f, 0f, 40f))
    }

    @Test
    fun `a ray straight up finds the ceiling above the eye`() {
        val world = VoxelWorld()
        world.set(2, 9, 2, BlockType.STONE)

        val ray = VoxelRaycast()
        assertTrue(ray.cast(world, 2.5f, 5.5f, 2.5f, 0f, 1f, 0f, 20f))
        assertEquals(BlockGeometry.BOTTOM, ray.face)
        assertEquals(8, ray.placementY())
    }
}

class VoxelPlayerTest {

    private fun flatWorld(surfaceY: Int): VoxelWorld {
        val world = VoxelWorld()
        for (x in 0 until VoxelWorld.SIZE_X) {
            for (z in 0 until VoxelWorld.SIZE_Z) {
                world.set(x, surfaceY, z, BlockType.STONE)
            }
        }
        return world
    }

    @Test
    fun `gravity pulls the player down onto the surface`() {
        val world = flatWorld(10)
        val player = VoxelPlayer()
        player.position.set(8f, 20f, 8f)

        repeat(240) { player.update(world, 1f / 60f, 0f, 0f, 0f) }

        assertEquals(11f, player.position.y, 0.05f)
        assertTrue(player.onGround)
    }

    @Test
    fun `a jump leaves the ground and comes back down`() {
        val world = flatWorld(10)
        val player = VoxelPlayer()
        player.spawn(world, 8f, 8f)
        repeat(30) { player.update(world, 1f / 60f, 0f, 0f, 0f) }
        assertTrue(player.onGround)

        player.jump()
        player.update(world, 1f / 60f, 0f, 0f, 0f)
        assertTrue(player.position.y > 11.05f, "player did not rise: ${player.position.y}")

        repeat(240) { player.update(world, 1f / 60f, 0f, 0f, 0f) }
        assertEquals(11f, player.position.y, 0.05f)
    }

    @Test
    fun `a wall stops horizontal movement`() {
        val world = flatWorld(10)
        for (y in 11..14) {
            world.set(12, y, 8, BlockType.STONE)
        }

        val player = VoxelPlayer()
        player.spawn(world, 8f, 8f)
        repeat(60) { player.update(world, 1f / 60f, 0f, 0f, 0f) }

        repeat(120) { player.update(world, 1f / 60f, 0.2f, 0f, 0f) }
        assertTrue(player.position.x < 12f, "player walked through the wall: ${player.position.x}")
        assertTrue(player.position.x > 10.5f, "player stopped too early: ${player.position.x}")
    }

    @Test
    fun `flying ignores gravity`() {
        val world = flatWorld(10)
        val player = VoxelPlayer()
        player.spawn(world, 8f, 8f)
        player.flying = true
        val startY = player.position.y

        repeat(60) { player.update(world, 1f / 60f, 0f, 0f, 0f) }
        assertEquals(startY, player.position.y, 1e-3f)

        repeat(60) { player.update(world, 1f / 60f, 0f, 0f, 1f) }
        assertTrue(player.position.y > startY + 5f, "flying did not ascend: ${player.position.y}")
    }

    @Test
    fun `the eye sits at head height above the feet`() {
        val player = VoxelPlayer()
        player.position.set(1f, 2f, 3f)
        val eye = player.eye(com.badlogic.gdx.math.Vector3())
        assertEquals(1f, eye.x, 1e-5f)
        assertEquals(2f + VoxelPlayer.EYE_HEIGHT, eye.y, 1e-5f)
        assertEquals(3f, eye.z, 1e-5f)
    }

    @Test
    fun `look direction follows yaw and pitch`() {
        val player = VoxelPlayer()
        val out = com.badlogic.gdx.math.Vector3()

        player.yaw = 0f
        player.pitch = 0f
        player.look(out)
        assertEquals(-1f, out.z, 1e-4f)
        assertEquals(0f, out.x, 1e-4f)

        player.pitch = 90f
        player.look(out)
        assertEquals(1f, out.y, 1e-4f)
    }
}

class BlockGeometryTest {

    private fun flatWorld(surfaceY: Int): VoxelWorld {
        val world = VoxelWorld()
        for (x in 0 until VoxelWorld.SIZE_X) {
            for (z in 0 until VoxelWorld.SIZE_Z) {
                world.set(x, surfaceY, z, BlockType.STONE)
            }
        }
        return world
    }

    @Test
    fun `a single floating block produces six faces`() {
        val world = VoxelWorld()
        world.set(8, 8, 8, BlockType.STONE)

        val mesh = BlockGeometry.build(world, BlockGeometry.chunkOf(8, 8))
        assertEquals(6 * 4, mesh.vertexCount())
        assertEquals(6 * 2, mesh.triangleCount())
    }

    @Test
    fun `hidden faces between solid neighbours are dropped`() {
        val world = VoxelWorld()
        world.set(8, 8, 8, BlockType.STONE)
        world.set(9, 8, 8, BlockType.STONE)

        val mesh = BlockGeometry.build(world, BlockGeometry.chunkOf(8, 8))
        // Ten faces instead of twelve: the two touching sides are hidden.
        assertEquals(10 * 4, mesh.vertexCount())
    }

    @Test
    fun `an air only chunk is empty`() {
        val world = VoxelWorld()
        val mesh = BlockGeometry.build(world, 0)
        assertTrue(mesh.isEmpty)
        assertEquals(0, mesh.triangleCount())
    }

    @Test
    fun `a flat layer keeps its top and bottom but hides interior sides`() {
        val world = flatWorld(5)

        // A middle chunk is surrounded by the same layer, so only its top and
        // bottom faces survive: 16x16 each, two triangles per face.
        val middle = BlockGeometry.build(world, BlockGeometry.chunkOf(32, 32))
        assertEquals(2 * 256 * 4, middle.vertexCount())

        // The corner chunk touches the edge of the world on two sides, so it also
        // keeps the two 16-face rims that border open air.
        val corner = BlockGeometry.build(world, 0)
        assertEquals((2 * 256 + 2 * 16) * 4, corner.vertexCount())
    }

    @Test
    fun `vertex data carries positions, normals and colour`() {
        val world = VoxelWorld()
        world.set(0, 0, 0, BlockType.STONE)
        val mesh: VoxelMesh = BlockGeometry.build(world, BlockGeometry.chunkOf(0, 0))

        val floats = mesh.vertices.items
        var maxNormalLengthError = 0f
        for (v in 0 until mesh.vertexCount()) {
            val base = v * VoxelMesh.FLOATS_PER_VERTEX
            val nx = floats[base + 3]
            val ny = floats[base + 4]
            val nz = floats[base + 5]
            maxNormalLengthError = maxOf(
                maxNormalLengthError,
                abs(1f - kotlin.math.sqrt(nx * nx + ny * ny + nz * nz)),
            )
        }
        assertTrue(maxNormalLengthError < 1e-4f, "normals are not unit length: $maxNormalLengthError")

        val topNormalIndex = 0
        assertEquals(0f, floats[topNormalIndex * VoxelMesh.FLOATS_PER_VERTEX + 3], 1e-5f)
        assertEquals(1f, floats[topNormalIndex * VoxelMesh.FLOATS_PER_VERTEX + 4], 1e-5f)
    }

    @Test
    fun `chunk lookup maps coordinates to their column`() {
        assertEquals(0, BlockGeometry.chunkOf(0, 0))
        assertEquals(1, BlockGeometry.chunkOf(BlockGeometry.CHUNK_SIZE, 0))
        assertEquals(-1, BlockGeometry.chunkOf(-1, 0))
        assertEquals(-1, BlockGeometry.chunkOf(0, VoxelWorld.SIZE_Z))
        assertEquals(BlockGeometry.CHUNK_COUNT - 1, BlockGeometry.chunkOf(VoxelWorld.SIZE_X - 1, VoxelWorld.SIZE_Z - 1))
    }

    @Test
    fun `no chunk exceeds the 16-bit index limit`() {
        val world = VoxelWorld()
        TerrainGenerator(2024L).generate(world)

        for (chunk in 0 until BlockGeometry.CHUNK_COUNT) {
            val mesh = BlockGeometry.build(world, chunk)
            assertTrue(
                mesh.vertexCount() <= 65535,
                "chunk $chunk has ${mesh.vertexCount()} vertices, too many for short indices",
            )
            assertNotEquals(0, mesh.vertexCount())
        }
    }
}

class VoxelInventoryTest {

    @Test
    fun `mined blocks are added and spent`() {
        val inventory = VoxelInventory()
        assertEquals(0, inventory.count(BlockType.STONE))

        inventory.add(BlockType.STONE, 3)
        assertEquals(3, inventory.count(BlockType.STONE))

        inventory.selectBlock(BlockType.STONE)
        assertSame(BlockType.STONE, inventory.selectedBlock())
        assertTrue(inventory.consumeSelected())
        assertEquals(2, inventory.count(BlockType.STONE))
    }

    @Test
    fun `spending an empty slot fails`() {
        val inventory = VoxelInventory()
        inventory.selectBlock(BlockType.BRICK)
        assertFalse(inventory.consumeSelected())
    }

    @Test
    fun `non placeable blocks never enter the bar`() {
        val inventory = VoxelInventory()
        assertEquals(0, inventory.add(BlockType.WATER, 5))
        assertEquals(0, inventory.add(BlockType.BEDROCK, 5))
        assertEquals(0, inventory.add(BlockType.AIR, 5))
    }

    @Test
    fun `selection wraps around the hotbar`() {
        val inventory = VoxelInventory()
        inventory.select(0)
        inventory.selectPrevious()
        assertEquals(VoxelInventory.HOTBAR.size - 1, inventory.selectedIndex())

        inventory.selectNext()
        assertEquals(0, inventory.selectedIndex())
    }

    @Test
    fun `snapshot and restore round trip the counts`() {
        val inventory = VoxelInventory()
        inventory.fillStartingKit()
        inventory.add(BlockType.SAND, 7)

        val other = VoxelInventory()
        other.restore(inventory.snapshot())
        assertEquals(inventory.count(BlockType.SAND), other.count(BlockType.SAND))
        assertEquals(inventory.count(BlockType.COBBLESTONE), other.count(BlockType.COBBLESTONE))
    }
}
