package com.tycoon.tycoon

/** Plain serializable snapshot of [TycoonState]. Field names are the save format. */
class SaveData {
    var coins: Double = 0.0
    var totalEarned: Double = 0.0
    var coinsCollected: Long = 0
    var playtime: Float = 0f
    var levels: MutableMap<String, Int> = LinkedHashMap()
}
