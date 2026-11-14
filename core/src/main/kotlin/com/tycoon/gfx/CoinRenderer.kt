package com.tycoon.gfx

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array
import com.tycoon.world.CoinField

/**
 * Draws the pickups. One [ModelInstance] is pooled per live coin, so the
 * instance count tracks [CoinField] exactly.
 */
class CoinRenderer {

    private val coinModel: Model
    private val instances = Array<ModelInstance>()
    private val rotationAxis = Vector3.Z

    init {
        val factory = ModelFactory()
        coinModel = factory.cylinder(0.45f, 0.12f, 20, MaterialFactory.shiny(Color(1f, 0.82f, 0.15f, 1f), 0.35f))
    }

    fun render(batch: ModelBatch, environment: Environment, field: CoinField) {
        val coins = field.coins()

        while (instances.size < coins.size) {
            instances.add(ModelInstance(coinModel))
        }
        while (instances.size > coins.size) {
            instances.removeIndex(instances.size - 1)
        }

        for (i in 0 until coins.size) {
            val coin = coins[i]
            val instance = instances[i]
            instance.transform.idt()
            instance.transform.translate(coin.position)
            instance.transform.rotate(Vector3.Y, coin.spin)
            instance.transform.rotate(rotationAxis, 90f)
        }

        batch.render(instances, environment)
    }

    fun dispose() {
        coinModel.dispose()
    }
}
