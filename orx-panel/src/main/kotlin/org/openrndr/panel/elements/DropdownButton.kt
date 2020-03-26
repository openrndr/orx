package org.openrndr.panel.elements

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.FontImageMap
import org.openrndr.panel.style.*
import org.openrndr.shape.Rectangle
import org.openrndr.text.Writer
import kotlinx.coroutines.yield
import org.openrndr.KEY_ARROW_DOWN
import org.openrndr.KEY_ARROW_UP
import org.openrndr.KEY_ENTER
import org.openrndr.events.Event
import org.openrndr.launch
import kotlin.math.min
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

    class Events {
        val valueChanged = Event<ValueChangedEvent>()
    }

    val events = Events()

    init {
        mouse.pressed.listen {
            it.cancelPropagation()
        }

        mouse.clicked.listen {
            val itemCount = items().size

            if (children.none { it is SlideOut }) {
                val height = min(240.0, itemCount * 24.0)
                if (screenPosition.y < root().layout.screenHeight - height) {
                    val so = SlideOut(0.0, screenArea.height, screenArea.width, height, this, value)
                    append(so)
                    (root() as Body).controlManager.keyboardInput.requestFocus(so)
                } else {
                    val so = SlideOut(0.0, screenArea.height - height, screenArea.width, height, this, value)
                    append(so)
                    (root() as Body).controlManager.keyboardInput.requestFocus(so)
                }
            } else {
                (children.first { it is SlideOut } as SlideOut?)?.dispose()
            }
        }
    }

    override val widthHint: Double?
        get() {
            computedStyle.let { style ->
                val fontUrl = (root() as? Body)?.controlManager?.fontManager?.resolve(style.fontFamily) ?: "broken"
                val fontSize = (style.fontSize as? LinearDimension.PX)?.value ?: 16.0
                val fontMap = FontImageMap.fromUrl(fontUrl, fontSize)
                val writer = Writer(null)

                writer.box = Rectangle(0.0,
                        0.0,
                        Double.POSITIVE_INFINITY,
                        Double.POSITIVE_INFINITY)

                val text = "$label  ${(value?.label) ?: "<choose>"}"
                writer.drawStyle.fontMap = fontMap
                writer.newLine()
                writer.text(text, visible = false)

                return writer.cursor.x + 10.0
            }
        }


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
        drawer.rectangle(0.0, 0.0, screenArea.width, screenArea.height)

        (root() as? Body)?.controlManager?.fontManager?.let {
            val font = it.font(computedStyle)

            val writer = Writer(drawer)
            drawer.fontMap = (font)

            val text = (value?.label) ?: "<choose>"

            val textWidth = writer.textWidth(text)
            val textHeight = font.ascenderLength

            val offset = Math.round((layout.screenWidth - textWidth))
            val yOffset = Math.round((layout.screenHeight / 2) + textHeight / 2.0) - 2.0

            drawer.fill = ((computedStyle.color as? Color.RGBa)?.color ?: ColorRGBa.WHITE)

            drawer.text(label, 5.0, 0.0 + yOffset)
            drawer.text(text, -5.0 + offset, 0.0 + yOffset)
        }
    }

    class SlideOut(val x: Double, val y: Double, val width: Double, val height: Double, parent: Element, active: Item?) : Element(ElementType("slide-out")) {
        init {

            val itemButtons = mutableMapOf<Item, Button>()

            var activeIndex =
                    if (active != null) {
                        (parent as DropdownButton).items().indexOf(active)
                    } else {
                        -1
                    }

            keyboard.pressed.listen {

                if (it.key == KEY_ENTER) {
                    it.cancelPropagation()
                    dispose()
                }

                if (it.key == KEY_ARROW_DOWN) {
                    activeIndex = (activeIndex + 1).coerceAtMost((parent as DropdownButton).items().size - 1)
                    it.cancelPropagation()
                    val newValue = parent.items()[activeIndex]

                    parent.value?.let {
                        itemButtons[it]?.pseudoClasses?.remove(ElementPseudoClass("selected"))
                    }
                    parent.value?.let {
                        itemButtons[newValue]?.pseudoClasses?.add(ElementPseudoClass("selected"))
                    }

                    parent.value = newValue
                    parent.events.valueChanged.trigger(ValueChangedEvent(parent, newValue))
                    newValue.picked()
                    draw.dirty = true

                    val ypos = 24.0 * activeIndex
                    if (ypos >= scrollTop + 10 * 24.0) {
                        scrollTop += 24.0
                    }

                }

                if (it.key == KEY_ARROW_UP) {
                    activeIndex = (activeIndex - 1).coerceAtLeast(0)


                    val newValue = (parent as DropdownButton).items()[activeIndex]

                    val ypos = 24.0 * activeIndex
                    if (ypos < scrollTop) {
                        scrollTop -= 24.0
                    }

                    parent.value?.let {
                        itemButtons[it]?.pseudoClasses?.remove(ElementPseudoClass("selected"))
                    }
                    parent.value?.let {
                        itemButtons[newValue]?.pseudoClasses?.add(ElementPseudoClass("selected"))
                    }

                    parent.value = newValue
                    parent.events.valueChanged.trigger(ValueChangedEvent(parent, newValue))
                    newValue.picked()
                    draw.dirty = true
                }
            }

            mouse.scrolled.listen {
                scrollTop -= it.rotation.y
                scrollTop = Math.max(0.0, scrollTop)
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
                        parent.value = it.source.data as Item
                        parent.events.valueChanged.trigger(ValueChangedEvent(parent, it.source.data as Item))
                        (data as Item).picked()
                        dispose()
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
            drawer.rectangle(0.0, 0.0, screenArea.width, screenArea.height)
            drawer.strokeWeight = 1.0
        }

        fun dispose() {
            parent?.remove(this)
        }
    }
}

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
        while (true) {
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
