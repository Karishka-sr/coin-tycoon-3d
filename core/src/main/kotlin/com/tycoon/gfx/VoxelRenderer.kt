package com.tycoon.gfx

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.tycoon.voxel.BlockGeometry
import com.tycoon.voxel.VoxelMesh
import com.tycoon.voxel.VoxelWorld

/**
 * Draws the voxel world.
 *
 * [BlockGeometry] hands back plain vertex and index arrays, so the meshes are
 * built by hand here rather than through `ModelBuilder.createBox`. Keeping the
 * meshing in plain arrays means it can be tested without an OpenGL context: only
 * this class needs the GL side.
 *
 * Chunks are rebuilt lazily. Mining or placing a block dirties its chunk, so only
 * that chunk is re-uploaded; a chunk is a 16x16 column of the world.
 */
class VoxelRenderer(private val world: VoxelWorld) {

    private val builder = ModelBuilder()
    private val chunks = arrayOfNulls<ChunkMesh>(BlockGeometry.CHUNK_COUNT)
    private val dirty = BooleanArray(BlockGeometry.CHUNK_COUNT) { true }

    /** Marks a chunk for rebuild; ignores coordinates outside the world. */
    fun markDirty(x: Int, z: Int) {
        val chunk = BlockGeometry.chunkOf(x, z)
        if (chunk >= 0) dirty[chunk] = true
    }

    /** Rebuilds every chunk that changed since the last call. */
    fun flush() {
        for (i in dirty.indices) {
            if (!dirty[i]) continue
            dirty[i] = false
            chunks[i]?.dispose()
            val geometry = BlockGeometry.build(world, i)
            chunks[i] = if (geometry.isEmpty) null else ChunkMesh(geometry)
        }
    }

    fun render(batch: ModelBatch, environment: Environment) {
        for (chunk in chunks) {
            chunk?.render(batch, environment)
        }
    }

    fun dispose() {
        for (chunk in chunks) chunk?.dispose()
        chunks.fill(null)
    }

    /** One chunk's GPU mesh, wrapped as a [ModelInstance] for the batch. */
    private inner class ChunkMesh(geometry: VoxelMesh) {

        private val model: Model
        private val instance: ModelInstance

        init {
            val mesh = Mesh(
                true,
                geometry.vertexCount(),
                geometry.indices.size,
                VERTEX_ATTRIBUTES,
            )
            mesh.setVertices(geometry.vertices.items, 0, geometry.vertices.size)
            mesh.setIndices(geometry.indices.items, 0, geometry.indices.size)

            builder.begin()
            builder.part("chunk", mesh, GL_TRIANGLES, 0, geometry.indices.size, material)
            model = builder.end()
            instance = ModelInstance(model)
        }

        fun render(batch: ModelBatch, environment: Environment) {
            batch.render(instance, environment)
        }

        fun dispose() {
            model.dispose()
        }
    }

    private companion object {
        const val GL_TRIANGLES = 4

        /** Per-vertex colour drives the block shading; the material passes it through. */
        val VERTEX_ATTRIBUTES = VertexAttributes(
            VertexAttribute.Position(),
            VertexAttribute.Normal(),
            VertexAttribute.ColorPacked(),
        )
        val material = Material(ColorAttribute.createDiffuse(Color.WHITE))
    }
}
