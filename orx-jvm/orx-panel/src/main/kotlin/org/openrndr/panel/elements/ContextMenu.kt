package org.openrndr.panel.elements

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.math.Vector2
import org.openrndr.panel.Shortcut
import org.openrndr.panel.style.*
import org.openrndr.shape.Rectangle
import org.openrndr.shape.contour

/** an entry of a menu: an item, a separator or a submenu */
sealed class MenuEntry {
    /** an item labeled [label] that calls [action], with the keyboard [shortcut] that calls it too */
    class Item(val label: String, val shortcut: String?, val action: () -> Unit) : MenuEntry()
    object Separator : MenuEntry()
    class Submenu(val label: String, val builder: ContextMenuBuilder.() -> Unit) : MenuEntry()
}

/** builds the entries of a menu, see [Element.contextMenu] and [MenuBar] */
class ContextMenuBuilder internal constructor() {
    internal val entries = mutableListOf<MenuEntry>()

    /** the items, for compatibility */
    internal val items: List<Pair<String, () -> Unit>> get() = entries.filterIsInstance<MenuEntry.Item>().map { it.label to it.action }

    /** adds an item labeled [label] that calls [action] when it is clicked */
    fun item(label: String, action: () -> Unit) {
        entries.add(MenuEntry.Item(label, null, action))
    }

    /** adds an item labeled [label] with the keyboard [shortcut], like `mod+s`, which a menu bar registers */
    fun item(label: String, shortcut: String, action: () -> Unit) {
        entries.add(MenuEntry.Item(label, shortcut, action))
    }

    /** adds a line between items */
    fun separator() {
        entries.add(MenuEntry.Separator)
    }

    /** adds a submenu labeled [label], it opens when the pointer is over it */
    fun submenu(label: String, builder: ContextMenuBuilder.() -> Unit) {
        entries.add(MenuEntry.Submenu(label, builder))
    }
}

/** an item of a [ContextMenu], with its shortcut at the right, or the arrow of its submenu */
class MenuItem internal constructor(
    val label: String, internal val action: () -> Unit,
    private val shortcut: String? = null, internal val submenu: (ContextMenuBuilder.() -> Unit)? = null
) : TextElement(ElementType("menu-item")) {
    init {
        text(label)
        mouse.pressed.listen { it.cancelPropagation() }
        mouse.clicked.listen {
            it.cancelPropagation()
            if (submenu != null) {
                (parent as? ContextMenu)?.openSubmenu(this)
                return@listen
            }
            (parent as? ContextMenu)?.dismissAll()
            action()
        }
        mouse.entered.listen {
            val menu = parent as? ContextMenu ?: return@listen
            menu.highlight(this)
            if (submenu != null) menu.openSubmenu(this) else menu.closeSubmenu()
        }
    }

    override fun draw(drawer: Drawer) {
        super.draw(drawer)
        val color = computedStyle.effectiveColor ?: ColorRGBa.WHITE
        val h = layout.screenHeight
        val w = layout.screenWidth
        if (submenu != null) {
            drawer.fill = color.opacify(0.7)
            drawer.stroke = null
            drawer.contour(contour { moveTo(w - 12.0, h / 2.0 - 4.0); lineTo(w - 7.0, h / 2.0); lineTo(w - 12.0, h / 2.0 + 4.0); close() })
        } else if (shortcut != null) {
            val text = Shortcut.parse(shortcut)?.toString() ?: shortcut
            panelText?.drawInBox(drawer, this, "shortcut", computedStyle, text, Rectangle(0.0, 0.0, w - 8.0, h), 1.0, 0.5, color.opacify(0.55))
        }
    }
}

/** a line between the items of a [ContextMenu] */
class MenuSeparator internal constructor() : Element(ElementType("menu-separator")) {
    override fun draw(drawer: Drawer) {
        drawer.stroke = null
        drawer.fill = (computedStyle.effectiveColor ?: ColorRGBa.WHITE).opacify(0.15)
        drawer.rectangle(6.0, layout.screenHeight / 2.0, layout.screenWidth - 12.0, 1.0)
    }
}

/**
 * A menu at [position] on the screen, of the entries that [builder] adds; it closes when an item is clicked, when the
 * pointer presses outside it and with escape. Opened by the control manager for elements with an
 * [Element.contextMenu], and by a [MenuBar].
 */
class ContextMenu internal constructor(position: Vector2, builder: ContextMenuBuilder.() -> Unit) : Div(ElementType("context-menu")) {
    /** the submenu that is open, null when none is */
    var submenu: ContextMenu? = null
        private set

    /** the menu this is a submenu of, null for a menu of its own */
    var parentMenu: ContextMenu? = null
        private set

    init {
        style {
            this.position = Position.FIXED
            left = position.x.px
            top = position.y.px
            zIndex = ZIndex.Value(1000)
        }
        for (entry in ContextMenuBuilder().apply(builder).entries) {
            when (entry) {
                is MenuEntry.Item -> append(MenuItem(entry.label, entry.action, entry.shortcut))
                is MenuEntry.Separator -> append(MenuSeparator())
                is MenuEntry.Submenu -> append(MenuItem(entry.label, {}, submenu = entry.builder))
            }
        }
        mouse.pressed.listen { it.cancelPropagation() }
    }

    /** opens the submenu of [item] next to it */
    internal fun openSubmenu(item: MenuItem) {
        val builder = item.submenu ?: return
        if (submenu?.let { it.opener === item } == true) return
        closeSubmenu()
        val menu = ContextMenu(Vector2(layout.screenX + layout.screenWidth - 4.0, item.layout.screenY - 4.0), builder)
        menu.parentMenu = this
        menu.opener = item
        submenu = menu
        root().append(menu)
    }

    //<editor-fold desc="the keyboard">
    /** the items of the menu, without the separators */
    val items: List<MenuItem> get() = children.filterIsInstance<MenuItem>()

    /** the item that the keyboard is at, `:selected`, null when none is */
    var highlighted: MenuItem? = null
        private set

    /** moves the keyboard to [item] */
    fun highlight(item: MenuItem?) {
        highlighted?.pseudoClasses?.remove(ElementPseudoClass("selected"))
        highlighted = item
        item?.pseudoClasses?.add(ElementPseudoClass("selected"))
        requestRedraw()
    }

    /** moves the keyboard [delta] items down, around the ends */
    internal fun moveHighlight(delta: Int) {
        val items = items
        if (items.isEmpty()) return
        val i = items.indexOf(highlighted)
        highlight(items[if (i < 0) (if (delta > 0) 0 else items.lastIndex) else Math.floorMod(i + delta, items.size)])
    }

    /** moves the keyboard to the next item that starts with [c] */
    internal fun typeAhead(c: Char) {
        val items = items
        val i = items.indexOf(highlighted)
        val next = (1..items.size).map { items[Math.floorMod(i + it, items.size)] }.firstOrNull { it.label.firstOrNull()?.equals(c, ignoreCase = true) == true }
        if (next != null) highlight(next)
    }

    /** opens the submenu of the item the keyboard is at, the keyboard at its first item; false when it has none */
    internal fun enterSubmenu(): Boolean {
        val item = highlighted?.takeIf { it.submenu != null } ?: return false
        openSubmenu(item)
        submenu?.moveHighlight(1)
        return true
    }

    /** calls the item the keyboard is at, or opens its submenu */
    internal fun activate() {
        val item = highlighted ?: return
        if (!enterSubmenu()) {
            dismissAll()
            item.action()
        }
    }

    /** the innermost menu that is open, this or a submenu of it */
    internal val innermost: ContextMenu get() = submenu?.innermost ?: this
    //</editor-fold>

    /** the element that opened the menu, the item of a submenu or the title in a menu bar */
    internal var opener: Element? = null

    /** closes the submenu, and its submenus */
    internal fun closeSubmenu() {
        submenu?.dismiss()
        submenu = null
    }

    /** true when [position] is on this menu or one of its submenus */
    fun containsMenuPoint(position: Vector2): Boolean =
        containsScreenPoint(position) || submenu?.containsMenuPoint(position) == true

    /** removes the menu and its submenus */
    fun dismiss() {
        closeSubmenu()
        val root = root()
        parent?.remove(this)
        root.requestRedraw()
    }

    /** removes this menu and the menus it is a submenu of */
    fun dismissAll() {
        var top = this
        while (true) top = top.parentMenu ?: break
        top.dismiss()
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
