package org.openrndr.extra.camera

import org.openrndr.events.Event

interface ChangeEvents {
    val changed : Event<Unit>
    val hasChanged: Boolean
}