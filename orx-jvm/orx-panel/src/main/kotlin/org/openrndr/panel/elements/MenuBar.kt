package org.openrndr.panel.elements

import org.openrndr.math.Vector2

/**
 * A bar of menus, like the menu bar of an application: [menu] adds a menu with a title, a click on the title opens
 * it, and while a menu is open the pointer opens the others. The items with shortcuts are added to the commands of the
 * control manager, see [org.openrndr.panel.Commands], so that their keys call them; all items can be found in the
 * command palette. The titles are `menu-title`s, `:active` while their menu is open.
 */
class MenuBar : Div(ElementType("menu-bar")) {
    private val menus = mutableListOf<Pair<String, ContextMenuBuilder.() -> Unit>>()
    private var registered = false

    /** adds the menu [title] of the entries that [builder] adds */
    fun menu(title: String, builder: ContextMenuBuilder.() -> Unit) {
        menus.add(title to builder)
        append(MenuTitle(this, title, builder))
        registered = false
    }

    // the items are added to the commands once the bar is in a control manager
    override fun prepare() {
        // a title is :active while its menu is open
        if (!anyOpen) children.forEach { it.pseudoClasses.remove(ElementPseudoClass("active")) }
        if (registered) return
        val commands = (root() as? Body)?.controlManager?.commands ?: return
        fun register(group: String, builder: ContextMenuBuilder.() -> Unit) {
            for (entry in ContextMenuBuilder().apply(builder).entries) {
                when (entry) {
                    is MenuEntry.Item -> commands.add(entry.label, entry.shortcut, group, entry.action)
                    is MenuEntry.Submenu -> register("$group: ${entry.label}", entry.builder)
                    MenuEntry.Separator -> {}
                }
            }
        }
        for ((title, builder) in menus) register(title, builder)
        registered = true
    }

    internal fun open(title: MenuTitle) {
        val cm = (root() as? Body)?.controlManager ?: return
        if (cm.currentMenu?.let { it.opener === title } == true) return
        val menu = ContextMenu(Vector2(title.layout.screenX, title.layout.screenY + title.layout.screenHeight), title.builder)
        menu.opener = title
        cm.showMenu(menu, title)
        children.forEach { it.pseudoClasses.remove(ElementPseudoClass("active")) }
        title.pseudoClasses.add(ElementPseudoClass("active"))
    }

    internal val anyOpen: Boolean
        get() = (root() as? Body)?.controlManager?.currentMenu?.let { m -> children.any { it === m.opener } } == true

    internal fun closeAll() {
        (root() as? Body)?.controlManager?.currentMenu?.takeIf { m -> children.any { it === m.opener } }?.dismiss()
        children.forEach { it.pseudoClasses.remove(ElementPseudoClass("active")) }
    }
}

/** a title of a [MenuBar] */
class MenuTitle internal constructor(private val bar: MenuBar, title: String, internal val builder: ContextMenuBuilder.() -> Unit) :
    TextElement(ElementType("menu-title")) {
    init {
        text(title)
        mouse.pressed.listen {
            if (bar.anyOpen && ElementPseudoClass("active") in pseudoClasses) bar.closeAll() else bar.open(this)
            it.cancelPropagation()
        }
        mouse.clicked.listen { it.cancelPropagation() }
        mouse.entered.listen { if (bar.anyOpen) bar.open(this) }
    }
}

/** a [MenuBar] that [init] adds menus to */
fun Element.menuBar(vararg classes: String, init: MenuBar.() -> Unit): MenuBar = initElement(classes, MenuBar(), init)
