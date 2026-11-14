package com.tycoon.tycoon

/** Plain serializable snapshot of [TycoonState]. Field names are the save format. */
class SaveData {
    var coins: Double = 0.0
    var totalEarned: Double = 0.0
    var coinsCollected: Long = 0
    var playtime: Float = 0f
    var levels: MutableMap<String, Int> = LinkedHashMap()

    /** Seed of the voxel world, so the same terrain is regenerated on load. */
    var voxelSeed: Long = 0L

    /** Player edits to the voxel world, flattened as x, y, z, block id quads. */
    var voxelEdits: IntArray = IntArray(0)

    /** Hotbar contents, in [com.tycoon.voxel.VoxelInventory.HOTBAR] order. */
    var voxelInventory: IntArray = IntArray(0)
}
