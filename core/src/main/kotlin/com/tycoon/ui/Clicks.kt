package com.tycoon.ui

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.tycoon.audio.SfxId
import com.tycoon.audio.SoundBank

/**
 * Runs [action] on a completed click. [ClickListener] is an abstract class, so
 * it cannot be used as a SAM lambda the way [com.badlogic.gdx.scenes.scene2d.EventListener]
 * can; this keeps call sites short.
 */
fun Actor.onClick(action: () -> Unit) {
    addListener(object : ClickListener() {
        override fun clicked(event: InputEvent?, x: Float, y: Float) = action()
    })
}

/** As [onClick], but also plays [id] so navigation buttons give audible feedback. */
fun Actor.onClick(sound: SoundBank, id: SfxId = SfxId.UI_CLICK, action: () -> Unit) {
    addListener(object : ClickListener() {
        override fun clicked(event: InputEvent?, x: Float, y: Float) {
            sound.play(id)
            action()
        }
    })
}
