package org.openrndr.panel.elements

import kotlinx.coroutines.yield
import org.openrndr.KEY_ARROW_DOWN
import org.openrndr.KEY_ARROW_UP
import org.openrndr.KEY_END
import org.openrndr.KEY_ENTER
import org.openrndr.KEY_ESCAPE
import org.openrndr.KEY_HOME
import org.openrndr.KEY_SPACEBAR
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.events.Event
import org.openrndr.launch
import org.openrndr.panel.style.*
import org.openrndr.shape.Rectangle
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.reflect.KMutableProperty0

class Item : Element(ElementType("item")) {
    var label: String = ""
    var data: Any? = null

    class PickedEvent(val source: Item)

    class Events {
        val picked = Event<PickedEvent>()
    }

    val events = Events()

    fun picked() {
        events.picked.trigger(PickedEvent(this))
    }
}

class DropdownButton : Element(ElementType("dropdown-button")) {

    var label: String = "OK"
    var value: Item? = null

    class ValueChangedEvent(val source: DropdownButton, val value: Item)

    override fun close() {
        super.close()
        events.close()
    }

    class Events: AutoCloseable {
        val valueChanged = Event<ValueChangedEvent>()
        override fun close() {
            valueChanged.close()
        }
    }

    val events = Events()

    override val handlesKeyboardFocus = true

    /** true when the list of items is open */
    val isOpen: Boolean get() = children.any { it is SlideOut }

    /** opens the list of items below the button, or above it when it does not fit; the list takes the keys */
    fun open() {
        if (isOpen) return
        val height = min(240.0, items().size * 24.0)
        val y = if (screenPosition.y < root().layout.screenHeight - height) screenArea.height else screenArea.height - height
        val so = SlideOut(0.0, y, screenArea.width, height, this, value)
        append(so)
        (root() as? Body)?.controlManager?.keyboardInput?.requestFocus(so)
        requestRedraw()
    }

    /** closes the list of items, the button takes the keys again */
    fun closeList(focus: Boolean = true) {
        children.filterIsInstance<SlideOut>().forEach { it.dispose() }
        if (focus) (root() as? Body)?.controlManager?.keyboardInput?.requestFocus(this)
        requestRedraw()
    }

    /** selects [item] as the value */
    internal fun select(item: Item) {
        if (item === value) return
        value = item
        events.valueChanged.trigger(ValueChangedEvent(this, item))
        item.picked()
        requestRedraw()
    }

    init {
        mouse.pressed.listen {
            it.cancelPropagation()
        }

        mouse.clicked.listen {
            if (isOpen) closeList() else open()
        }

        // enter and space open the list, up and down change the value while it is closed
        keyboard.pressed.listen {
            if (isOpen) return@listen
            val items = items()
            val index = items.indexOf(value)
            when (it.key) {
                KEY_ENTER, KEY_SPACEBAR -> open()
                KEY_ARROW_DOWN -> items.getOrNull((index + 1).coerceAtMost(items.lastIndex))?.let(::select)
                KEY_ARROW_UP -> items.getOrNull((index - 1).coerceAtLeast(0))?.let(::select)
                else -> return@listen
            }
            it.cancelPropagation()
        }
    }

    override val widthHint: Double?
        get() = (panelText?.width(computedStyle, "$label  ${(value?.label) ?: "<choose>"}") ?: error("no control manager")) + 10.0


    override fun append(element: Element) {
        when (element) {
            is Item, is SlideOut -> super.append(element)
            else -> throw RuntimeException("only item and slideout")
        }
        super.append(element)
    }

    fun items(): List<Item> = children.filterIsInstance<Item>().map { it }

    override fun draw(drawer: Drawer) {

        drawer.fill = ((computedStyle.background as? Color.RGBa)?.color ?: ColorRGBa.PINK)
        drawer.stroke = null
        drawer.box(Rectangle(0.0, 0.0, screenArea.width, screenArea.height), cornerRadius)

        // the label at the left and the value at the right
        val panel = panelText ?: return
        val face = panel.face(computedStyle)
        val text = (value?.label) ?: "<choose>"
        val baseline = ((layout.screenHeight / 2) + face.ascent / 2.0).roundToInt() - 2.0
        val color = (computedStyle.color as? Color.RGBa)?.color ?: ColorRGBa.WHITE
        panel.drawLine(drawer, this, "label", computedStyle, label, 5.0, baseline, color)
        panel.drawLine(drawer, this, "value", computedStyle, text,
            -5.0 + Math.round(layout.screenWidth - panel.width(computedStyle, text)), baseline, color)
    }

    class SlideOut(val x: Double, val y: Double, val width: Double, val height: Double, parent: Element, active: Item?) : Element(ElementType("slide-out")) {
        init {

            val itemButtons = mutableMapOf<Item, Button>()

            val dropdown = parent as DropdownButton

            /** selects the item at [index] and scrolls its button into view */
            fun select(index: Int) {
                val items = dropdown.items()
                val item = items.getOrNull(index) ?: return
                dropdown.value?.let { itemButtons[it]?.pseudoClasses?.remove(ElementPseudoClass("selected")) }
                itemButtons[item]?.pseudoClasses?.add(ElementPseudoClass("selected"))
                dropdown.select(item)
                itemButtons[item]?.scrollIntoView()
                requestRedraw()
            }

            // the start of the label of an item, typed within a second
            var typed = ""
            var typedAt = 0L

            keyboard.pressed.listen {
                val items = dropdown.items()
                val index = items.indexOf(dropdown.value)
                when (it.key) {
                    KEY_ENTER, KEY_ESCAPE -> dropdown.closeList()
                    KEY_ARROW_DOWN -> select((index + 1).coerceAtMost(items.lastIndex))
                    KEY_ARROW_UP -> select((index - 1).coerceAtLeast(0))
                    KEY_HOME -> select(0)
                    KEY_END -> select(items.lastIndex)
                    else -> return@listen
                }
                it.cancelPropagation()
            }

            // typing jumps to the first item that starts with what is typed
            keyboard.character.listen {
                val now = System.currentTimeMillis()
                typed = if (now - typedAt < 1000L) typed + it.character else it.character.toString()
                typedAt = now
                val items = dropdown.items()
                val match = items.indexOfFirst { item -> item.label.startsWith(typed, ignoreCase = true) }
                if (match >= 0) select(match)
                it.cancelPropagation()
            }

            mouse.scrolled.listen {
                scrollTop = (scrollTop - it.rotation.y * 10.0).coerceIn(0.0, maxScrollTop)
                draw.dirty = true
                it.cancelPropagation()
            }

            mouse.exited.listen {
                it.cancelPropagation()
                dispose()
            }

            style = StyleSheet(CompoundSelector.DUMMY).apply {
                position = Position.ABSOLUTE
                left = LinearDimension.PX(x)
                top = LinearDimension.PX(y)
                width = LinearDimension.PX(this@SlideOut.width)
                height = LinearDimension.PX(this@SlideOut.height)
                overflow = Overflow.Scroll
                zIndex = ZIndex.Value(1000)
                background = Color.Inherit
            }

            (parent as DropdownButton).items().forEach {
                append(Button().apply {
                    data = it
                    label = it.label
                    itemButtons[it] = this
                    events.clicked.listen {
                        dropdown.select(it.source.data as Item)
                        dropdown.closeList()
                    }
                })
            }
            active?.let {
                itemButtons[active]?.pseudoClasses?.add(ElementPseudoClass("selected"))
            }
        }

        override fun draw(drawer: Drawer) {
            drawer.fill = ((computedStyle.background as? Color.RGBa)?.color ?: ColorRGBa.PINK)
            drawer.stroke = null
            drawer.strokeWeight = 0.0
            drawer.box(Rectangle(0.0, 0.0, screenArea.width, screenArea.height), cornerRadius)
            drawer.strokeWeight = 1.0
        }

        fun dispose() {
            parent?.remove(this)
        }
    }

}

/**
 * Two-way data binding between a DropdownButton and an enum property.
 * Launches a coroutine to monitor the property for external changes
 * and updates the dropdown selection to match.
 */
fun <E : Enum<E>> DropdownButton.bind(property: KMutableProperty0<E>, map: Map<E, String>) {
    val options = mutableMapOf<E, Item>()
    map.forEach { (k, v) ->
        options[k] = item {
            label = v
            events.picked.listen {
                property.set(k)
            }
        }
    }
    var currentValue = property.get()
    value = options[currentValue]
    draw.dirty = true

    (root() as? Body)?.controlManager?.program?.launch {
        while (!disposed) {
            val cval = property.get()
            if (cval != currentValue) {
                currentValue = cval
                value = options[cval]
                draw.dirty = true
            }
            yield()
        }
    }
}
