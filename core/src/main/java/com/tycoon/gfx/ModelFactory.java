package com.tycoon.gfx;

import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;

/**
 * Builds the game's low-poly 3D models procedurally.
 *
 * <p>Keeping the geometry in code means the project ships without any external
 * model files, which makes the first build reproducible on any machine.
 */
public final class ModelFactory {

    /** Position + normal only: enough for the per-vertex lighting used here. */
    public static final long ATTRIBUTES =
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;

    private final ModelBuilder builder = new ModelBuilder();

    public Model box(float width, float height, float depth, Material material) {
        return builder.createBox(width, height, depth, material, ATTRIBUTES);
    }

    public Model sphere(float radius, int divisions, Material material) {
        return builder.createSphere(radius * 2f, radius * 2f, radius * 2f,
                divisions, divisions, material, ATTRIBUTES);
    }

    public Model cylinder(float radius, float height, int divisions, Material material) {
        return builder.createCylinder(radius * 2f, height, radius * 2f,
                divisions, material, ATTRIBUTES);
    }

    public Model cone(float radius, float height, int divisions, Material material) {
        return builder.createCone(radius * 2f, height, radius * 2f,
                divisions, material, ATTRIBUTES);
    }
}
