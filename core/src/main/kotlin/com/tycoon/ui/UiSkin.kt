package com.tycoon.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
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

        skin.add("default", Label.LabelStyle(regular, Color.WHITE))
        skin.add("title", Label.LabelStyle(title, Color(1f, 0.85f, 0.3f, 1f)))
        skin.add("hint", Label.LabelStyle(small, Color(0.8f, 0.85f, 0.9f, 1f)))

        skin.add("default", buttonStyle(regular))
        skin.add("shop", buttonStyle(small))
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
            up = rounded(Color(0.18f, 0.32f, 0.55f, 0.95f))
            over = rounded(Color(0.24f, 0.42f, 0.7f, 0.95f))
            down = rounded(Color(0.12f, 0.24f, 0.42f, 0.95f))
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
}
