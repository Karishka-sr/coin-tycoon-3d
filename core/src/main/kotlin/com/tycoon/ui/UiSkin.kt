package com.tycoon.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable

/**
 * Builds the Scene2D [Skin] entirely in code so the project needs no skin
 * atlas. Fonts are rasterised from the bundled DejaVu TTF, which covers the
 * Cyrillic text used by the interface.
 */
object UiSkin {

    private const val FONT_FILE = "DejaVuSans.ttf"
    private const val FONT_BOLD_FILE = "DejaVuSans-Bold.ttf"

    fun create(): Skin {
        val skin = Skin()

        val regular = generate(FONT_FILE, 20, Color.WHITE)
        val title = generate(FONT_BOLD_FILE, 30, Color.WHITE)
        val small = generate(FONT_FILE, 16, Color(0.85f, 0.85f, 0.9f, 1f))

        skin.add("default-font", regular)
        skin.add("title-font", title)
        skin.add("small-font", small)

        skin.add("white", whiteTexture())

        // Registered against Drawable.class: Skin.add(name, obj) would key these
        // by their concrete TextureRegionDrawable type, which getDrawable misses.
        skin.add("stick-base", circleDrawable(220, Color(1f, 1f, 1f, 0.12f), Color(1f, 1f, 1f, 0.4f), 5), Drawable::class.java)
        skin.add("stick-knob", circleDrawable(110, Color(1f, 0.85f, 0.3f, 0.85f), Color(1f, 1f, 1f, 0.65f), 4), Drawable::class.java)
        skin.add("crosshair", crosshairDrawable(28), Drawable::class.java)

        skin.add("checkbox-on", squareDrawable(36, Color(1f, 0.85f, 0.3f, 1f), Color(1f, 1f, 1f, 0.85f), 3), Drawable::class.java)
        skin.add("checkbox-off", squareDrawable(36, Color(0.14f, 0.16f, 0.22f, 1f), Color(1f, 1f, 1f, 0.5f), 3), Drawable::class.java)
        skin.add("slider-bg", rounded(Color(0f, 0f, 0f, 0.55f)).apply { minHeight = 14f }, Drawable::class.java)
        skin.add("slider-knob", circleDrawable(48, Color(1f, 0.85f, 0.3f, 1f), Color.WHITE, 3), Drawable::class.java)
        skin.add(
            "menu-bg",
            verticalGradient(Color(0.14f, 0.19f, 0.32f, 1f), Color(0.03f, 0.04f, 0.08f, 1f)),
            Drawable::class.java,
        )

        skin.add("default", Label.LabelStyle(regular, Color.WHITE))
        skin.add("title", Label.LabelStyle(title, Color(1f, 0.85f, 0.3f, 1f)))
        skin.add("hint", Label.LabelStyle(small, Color(0.8f, 0.85f, 0.9f, 1f)))

        skin.add("default", buttonStyle(regular))
        skin.add("shop", buttonStyle(small))
        skin.add("default", CheckBox.CheckBoxStyle(
            skin.getDrawable("checkbox-off"), skin.getDrawable("checkbox-on"), regular, Color.WHITE,
        ))
        val sliderStyle = Slider.SliderStyle(skin.getDrawable("slider-bg"), skin.getDrawable("slider-knob"))
        // A Slider looks its style up under "default-horizontal" / "default-vertical".
        skin.add("default-horizontal", sliderStyle)
        skin.add("default-vertical", sliderStyle)
        skin.add("default", TextField.TextFieldStyle().apply {
            font = regular
            fontColor = Color.WHITE
            background = rounded(Color(0f, 0f, 0f, 0.5f))
            cursor = TextureRegionDrawable(TextureRegion(whiteTexture()))
        })

        return skin
    }

    private fun buttonStyle(font: BitmapFont): TextButton.TextButtonStyle =
        TextButton.TextButtonStyle().apply {
            this.font = font
            fontColor = Color.WHITE
            downFontColor = Color(1f, 0.9f, 0.4f, 1f)
            overFontColor = Color(1f, 0.95f, 0.6f, 1f)
            disabledFontColor = Color(0.6f, 0.62f, 0.68f, 1f)
            up = rounded(Color(0.18f, 0.32f, 0.55f, 0.95f))
            over = rounded(Color(0.24f, 0.42f, 0.7f, 0.95f))
            down = rounded(Color(0.12f, 0.24f, 0.42f, 0.95f))
            disabled = rounded(Color(0.16f, 0.18f, 0.24f, 0.85f))
        }

    private fun generate(file: String, size: Int, color: Color): BitmapFont {
        val generator = FreeTypeFontGenerator(Gdx.files.internal(file))
        val parameter = FreeTypeFontParameter().apply {
            this.size = size
            this.color = color
            minFilter = Texture.TextureFilter.Linear
            magFilter = Texture.TextureFilter.Linear
            characters = FreeTypeFontGenerator.DEFAULT_CHARS + "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя№—–«»…₽"
        }
        val font = generator.generateFont(parameter)
        generator.dispose()
        font.data.markupEnabled = true
        return font
    }

    private fun whiteTexture(): Texture =
        Texture(1, 1, Pixmap.Format.RGBA8888).apply {
            val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
            pixmap.setColor(Color.WHITE)
            pixmap.fill()
            draw(pixmap, 0, 0)
            pixmap.dispose()
        }

    private fun rounded(color: Color): Drawable =
        TextureRegionDrawable(TextureRegion(whiteTexture())).tint(color)

    /** An anti-aliased circle, optionally ringed, used for the on-screen stick. */
    private fun circleDrawable(size: Int, fill: Color, ring: Color, ringWidth: Int): Drawable {
        val pixmap = Pixmap(size, size, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color.CLEAR)
        pixmap.fill()
        val r = size / 2f
        pixmap.setColor(ring)
        pixmap.fillCircle(size / 2, size / 2, (r - 1).toInt())
        pixmap.setColor(fill)
        pixmap.fillCircle(size / 2, size / 2, (r - ringWidth - 1).toInt())
        val texture = Texture(pixmap)
        pixmap.dispose()
        return TextureRegionDrawable(TextureRegion(texture))
    }

    /** A vertical two-colour gradient, stretched to fill whatever it is drawn in. */
    private fun verticalGradient(top: Color, bottom: Color): Drawable {
        val height = 256
        val pixmap = Pixmap(1, height, Pixmap.Format.RGBA8888)
        for (y in 0 until height) {
            // Pixmap rows start at the top, so blend top -> bottom as y grows.
            pixmap.setColor(top.cpy().lerp(bottom, y / (height - 1f)))
            pixmap.drawPixel(0, y)
        }
        val texture = Texture(pixmap)
        pixmap.dispose()
        return TextureRegionDrawable(TextureRegion(texture))
    }

    /** A filled square with an outline, used for the checkbox states. */
    private fun squareDrawable(size: Int, fill: Color, border: Color, borderWidth: Int): Drawable {
        val pixmap = Pixmap(size, size, Pixmap.Format.RGBA8888)
        pixmap.setColor(border)
        pixmap.fill()
        pixmap.setColor(fill)
        pixmap.fillRectangle(borderWidth, borderWidth, size - borderWidth * 2, size - borderWidth * 2)
        val texture = Texture(pixmap)
        pixmap.dispose()
        return TextureRegionDrawable(TextureRegion(texture))
    }

    /** A small plus-shaped aim marker for the centre of the screen. */
    private fun crosshairDrawable(size: Int): Drawable {
        val pixmap = Pixmap(size, size, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color.CLEAR)
        pixmap.fill()
        pixmap.setColor(1f, 1f, 1f, 0.85f)
        val t = 2
        val c = size / 2
        pixmap.fillRectangle(0, c - t / 2, size, t)
        pixmap.fillRectangle(c - t / 2, 0, t, size)
        val texture = Texture(pixmap)
        pixmap.dispose()
        return TextureRegionDrawable(TextureRegion(texture))
    }
}
