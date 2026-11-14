package com.tycoon.voxel;

import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ShortArray;

/**
 * Raw geometry for one chunk: interleaved vertices (position, normal, packed
 * colour) plus triangle indices.
 *
 * <p>Kept as plain arrays rather than a LibGDX {@code Mesh} so the meshing code
 * can be exercised in a headless test without an OpenGL context.
 */
public class VoxelMesh {

    /** Floats per vertex: 3 position + 3 normal + 1 packed colour. */
    public static final int FLOATS_PER_VERTEX = 7;

    public final FloatArray vertices;
    public final ShortArray indices;

    public VoxelMesh(FloatArray vertices, ShortArray indices) {
        this.vertices = vertices;
        this.indices = indices;
    }

    public int vertexCount() {
        return vertices.size / FLOATS_PER_VERTEX;
    }

    public int triangleCount() {
        return indices.size / 3;
    }

    public boolean isEmpty() {
        return indices.size == 0;
    }
}
