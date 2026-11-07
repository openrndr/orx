package org.openrndr.panel.elements

import org.openrndr.panel.fadeIn
import org.openrndr.KEY_ARROW_LEFT
import org.openrndr.KEY_ARROW_RIGHT
import org.openrndr.KEY_ENTER
import org.openrndr.KEY_SPACEBAR
import org.openrndr.events.Event
import org.openrndr.panel.style.Display
import org.openrndr.panel.style.display

/** a tab of [Tabs], the label of a page */
class Tab internal constructor(val tabs: Tabs) : TextElement(ElementType("tab")) {
    override val handlesKeyboardFocus = true

    init {
        mouse.pressed.listen { it.cancelPropagation() }
        mouse.clicked.listen {
            tabs.select(this)
            it.cancelPropagation()
        }
        keyboard.pressed.listen {
            when (it.key) {
                KEY_ENTER, KEY_SPACEBAR -> tabs.select(this)
                KEY_ARROW_LEFT -> tabs.selected = (tabs.selected - 1).coerceAtLeast(0)
                KEY_ARROW_RIGHT -> tabs.selected = (tabs.selected + 1).coerceAtMost(tabs.pages.lastIndex)
                else -> return@listen
            }
            it.cancelPropagation()
        }
    }
}

/**
 * Pages of which one shows at a time, selected by their tabs in a bar above them. Pages are added with [tab], the
 * tabs are of type `tab` and in the `tab-bar`, the pages are of type `tab-page`; the tab of the page that shows has
 * the state `selected`.
 */
class Tabs : Div(ElementType("tabs")) {
    class TabEvent(val source: Tabs, val index: Int)

    /** the page that shows changed */
    val changed = Event<TabEvent>("tabs-changed")

    /** the bar that holds the tabs */
    val bar: Div = Div(ElementType("tab-bar")).also { append(it) }

    private val tabList = mutableListOf<Tab>()

    /** the pages, in the order of their tabs */
    val pages = mutableListOf<Div>()

    /** the index of the page that shows */
    var selected: Int = 0
        set(value) {
            if (value == field || value !in pages.indices) return
            field = value
            update()
            // the page that shows fades in
            pages[value].fadeIn()
            changed.trigger(TabEvent(this, value))
        }

    internal fun select(tab: Tab) {
        selected = tabList.indexOf(tab)
    }

    private fun update() {
        for ((i, page) in pages.withIndex()) {
            page.style { display = if (i == selected) Display.BLOCK else Display.NONE }
            if (i == selected) tabList[i].pseudoClasses.add(ElementPseudoClass("selected"))
            else tabList[i].pseudoClasses.remove(ElementPseudoClass("selected"))
        }
        requestRedraw()
    }

    /** adds a page with a tab labeled [label], [init] builds the page */
    fun tab(label: String, init: Div.() -> Unit): Div {
        val tab = Tab(this)
        bar.append(tab)
        tab.text(label)
        tabList.add(tab)
        val page = Div(ElementType("tab-page"))
        append(page)
        pages.add(page)
        page.init()
        update()
        return page
    }
}

/** adds [Tabs], [init] adds their pages with [Tabs.tab] */
fun Element.tabs(vararg classes: String, init: Tabs.() -> Unit): Tabs = initElement(classes, Tabs(), init)
