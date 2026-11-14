package com.tycoon.gfx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;

/** Small helpers for building the diffuse/specular materials used by the scene. */
public final class MaterialFactory {

    private MaterialFactory() {
    }

    public static Material solid(Color color) {
        return new Material(ColorAttribute.createDiffuse(color));
    }

    /** A material with a bright highlight, used for gold coins and metal parts. */
    public static Material shiny(Color color, float glow) {
        return new Material(
                ColorAttribute.createDiffuse(color),
                ColorAttribute.createSpecular(0.9f, 0.9f, 0.9f, 1f),
                ColorAttribute.createEmissive(
                        color.r * glow, color.g * glow, color.b * glow, 1f));
    }
}
