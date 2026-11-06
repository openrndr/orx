package org.openrndr.panel.elements

import org.openrndr.math.Vector2
import org.openrndr.panel.style.*

/** builds the items of a context menu, see [Element.contextMenu] */
class ContextMenuBuilder internal constructor() {
    internal val items = mutableListOf<Pair<String, () -> Unit>>()

    /** adds an item labeled [label] that calls [action] when it is clicked */
    fun item(label: String, action: () -> Unit) {
        items.add(label to action)
    }
}

/** an item of a [ContextMenu] */
class MenuItem internal constructor(label: String, private val action: () -> Unit) : TextElement(ElementType("menu-item")) {
    init {
        text(label)
        mouse.pressed.listen { it.cancelPropagation() }
        mouse.clicked.listen {
            it.cancelPropagation()
            (parent as? ContextMenu)?.dismiss()
            action()
        }
    }
}

/**
 * A menu at [position] on the screen, of the items that [builder] adds; it closes when an item is clicked, when the
 * pointer presses outside it and with escape. Opened by the control manager for elements with an
 * [Element.contextMenu].
 */
class ContextMenu internal constructor(position: Vector2, builder: ContextMenuBuilder.() -> Unit) : Div(ElementType("context-menu")) {
    init {
        style {
            this.position = Position.FIXED
            left = position.x.px
            top = position.y.px
            zIndex = ZIndex.Value(1000)
        }
        ContextMenuBuilder().apply(builder).items.forEach { (label, action) -> append(MenuItem(label, action)) }
        mouse.pressed.listen { it.cancelPropagation() }
    }

    /** removes the menu */
    fun dismiss() {
        val root = root()
        parent?.remove(this)
        root.requestRedraw()
    }

    // the menu is moved into the window where it does not fit next to the pointer
    override fun sized() {
        val root = root()
        layout.screenX = layout.screenX.coerceAtMost(root.layout.screenWidth - layout.screenWidth - 4.0).coerceAtLeast(0.0)
        layout.screenY = layout.screenY.coerceAtMost(root.layout.screenHeight - layout.screenHeight - 4.0).coerceAtLeast(0.0)
    }
}

/** sets the menu that opens when this element is clicked with the right button, see [Element.contextMenu] */
fun Element.contextMenu(builder: ContextMenuBuilder.() -> Unit) {
    contextMenu = builder
}
