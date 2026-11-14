package com.tycoon.gfx

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array
import com.tycoon.world.Player

/**
 * Draws the player as a small stack of primitives (body, head, cap) that are
 * transformed together every frame.
 */
class PlayerRenderer {

    private val models = Array<Model>()
    private val body: ModelInstance
    private val head: ModelInstance
    private val cap: ModelInstance

    private val transform = Matrix4()
    private val offset = Vector3()

    init {
        val factory = ModelFactory()

        val bodyModel = factory.box(0.9f, 1.2f, 0.7f, MaterialFactory.solid(Color(0.2f, 0.45f, 0.9f, 1f)))
        val headModel = factory.sphere(0.42f, 16, MaterialFactory.solid(Color(0.95f, 0.8f, 0.65f, 1f)))
        val capModel = factory.cone(0.55f, 0.5f, 16, MaterialFactory.solid(Color(0.85f, 0.2f, 0.2f, 1f)))
        models.add(bodyModel)
        models.add(headModel)
        models.add(capModel)

        body = ModelInstance(bodyModel)
        head = ModelInstance(headModel)
        cap = ModelInstance(capModel)
    }

    fun render(batch: ModelBatch, environment: Environment, player: Player) {
        transform.setToTranslation(player.position.x, player.position.y + player.bobOffset, player.position.z)
        transform.rotate(Vector3.Y, player.headingDegrees)

        body.transform.set(transform).translate(0f, 0.6f, 0f)
        offset.set(0f, 1.5f, 0f)
        head.transform.set(transform).translate(offset)
        offset.set(0f, 1.95f, 0f)
        cap.transform.set(transform).translate(offset)

        batch.render(body, environment)
        batch.render(head, environment)
        batch.render(cap, environment)
    }

    fun dispose() {
        for (model in models) model.dispose()
    }
}
