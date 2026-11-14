package com.tycoon.tycoon

/**
 * The tycoon upgrade tree. Each entry maps to one purchasable line in the shop;
 * the actual effect of a level lives in [TycoonState].
 */
enum class Upgrade(
    val title: String,
    val description: String,
    val baseCost: Double,
    val costGrowth: Double,
) {
    COIN_VALUE("Ценность монет", "+25% к стоимости каждой монеты", 50.0, 1.55),
    SPAWN_RATE("Скорость спавна", "Монеты появляются чаще", 40.0, 1.5),
    MAGNET("Магнит", "Шире радиус притяжения монет", 60.0, 1.6),
    CAPACITY("Вместимость арены", "Больше монет на карте", 80.0, 1.6),
    PASSIVE_INCOME("Пассивный доход", "+1 монета в секунду", 120.0, 1.7),
    MOVE_SPEED("Скорость бега", "Игрок двигается быстрее", 70.0, 1.55),
    LUCK("Удача", "+3% к шансу крупной монеты", 90.0, 1.65),
}
