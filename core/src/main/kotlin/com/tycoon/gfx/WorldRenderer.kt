package com.tycoon.gfx

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array
import com.tycoon.world.Arena

/**
 * Builds and draws the static parts of the scene: the ground, the path, the
 * perimeter fence and the little city block. Every model is created once from
 * [ModelFactory] and reused through [ModelInstance] transforms.
 */
class WorldRenderer {

    private val models = Array<Model>()
    private val props = Array<ModelInstance>()
    private val ground: ModelInstance

    val environment = Environment().apply {
        set(ColorAttribute(ColorAttribute.AmbientLight, 0.55f, 0.58f, 0.62f, 1f))
        add(DirectionalLight().apply {
            color.set(1f, 0.96f, 0.88f, 1f)
            direction.set(-0.6f, -1f, -0.35f).nor()
        })
    }

    init {
        val factory = ModelFactory()

        val groundModel = ModelFactory().box(
            Arena.GROUND_SIZE, 0.5f, Arena.GROUND_SIZE,
            MaterialFactory.solid(Color(0.26f, 0.45f, 0.24f, 1f)),
        )
        models.add(groundModel)
        ground = ModelInstance(groundModel, Vector3(0f, -0.25f, 0f))

        val pathModel = factory.box(Arena.GROUND_SIZE, 0.06f, 6f, MaterialFactory.solid(Color(0.55f, 0.5f, 0.42f, 1f)))
        models.add(pathModel)
        props.add(ModelInstance(pathModel, Vector3(0f, 0.03f, 0f)))

        buildPerimeter(factory)
        buildCity(factory)
    }

    private fun buildPerimeter(factory: ModelFactory) {
        val fenceMaterial = MaterialFactory.solid(Color(0.45f, 0.32f, 0.2f, 1f))

        val alongZ = factory.box(0.4f, 1.2f, Arena.GROUND_SIZE, fenceMaterial)
        val alongX = factory.box(Arena.GROUND_SIZE, 1.2f, 0.4f, fenceMaterial)
        models.add(alongZ)
        models.add(alongX)

        props.add(ModelInstance(alongZ, Vector3(-Arena.HALF, 0.6f, 0f)))
        props.add(ModelInstance(alongZ, Vector3(Arena.HALF, 0.6f, 0f)))
        props.add(ModelInstance(alongX, Vector3(0f, 0.6f, -Arena.HALF)))
        props.add(ModelInstance(alongX, Vector3(0f, 0.6f, Arena.HALF)))
    }

    private fun buildCity(factory: ModelFactory) {
        val palette = arrayOf(
            Color(0.75f, 0.3f, 0.3f, 1f),
            Color(0.3f, 0.55f, 0.75f, 1f),
            Color(0.85f, 0.7f, 0.3f, 1f),
            Color(0.5f, 0.4f, 0.65f, 1f),
        )

        var i = 0
        for (x in -2..2) {
            for (z in -2..2) {
                if (Math.abs(x) <= 1 && Math.abs(z) <= 1) continue

                val width = 2.2f + (i % 3)
                val height = 2.5f + (i % 4) * 1.4f
                val depth = 2.2f + ((i + 1) % 3)
                val model = factory.box(width, height, depth, MaterialFactory.solid(palette[i % palette.size]))
                models.add(model)
                props.add(
                    ModelInstance(
                        model,
                        Vector3(x * 9f + MathUtils.sin(i.toFloat()) * 0.5f, height / 2f, z * 9f),
                    ),
                )
                i++
            }
        }
    }

    fun render(batch: ModelBatch) {
        batch.render(ground, environment)
        batch.render(props, environment)
    }


    fun dispose() {
        for (model in models) model.dispose()
    }
}
