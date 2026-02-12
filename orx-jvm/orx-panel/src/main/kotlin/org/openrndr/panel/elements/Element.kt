package org.openrndr.panel.elements

import org.openrndr.*
import org.openrndr.draw.Drawer
import org.openrndr.events.Event
import org.openrndr.math.Vector2
import org.openrndr.panel.collections.ObservableCopyOnWriteArrayList
import org.openrndr.panel.collections.ObservableHashSet
import org.openrndr.panel.style.CompoundSelector
import org.openrndr.panel.style.Display
import org.openrndr.panel.style.StyleSheet
import org.openrndr.panel.style.display
import org.openrndr.panel.style.effectivePaddingBottom
import org.openrndr.panel.style.effectivePaddingLeft
import org.openrndr.panel.style.effectivePaddingRight
import org.openrndr.panel.style.effectivePaddingTop
import org.openrndr.shape.Rectangle

import java.util.*
import kotlin.math.max

@JvmRecord
data class ElementClass(val name: String)
@JvmRecord
data class ElementPseudoClass(val name: String)
@JvmRecord
data class ElementType(val name: String)

val disabled = ElementPseudoClass("disabled")

class FocusEvent


open class Element(val type: ElementType): AutoCloseable {

    var disposed = false
        private set

    var scrollTop = 0.0
    open val handlesDoubleClick = false
    open val handlesKeyboardFocus = false

    open val widthHint: Double?
        get() {
            return null
        }


    open val heightHint: Double?
        get() {
            return null
        }

    class MouseObservables: AutoCloseable {
        val clicked = Event<MouseEvent>("element-mouse-clicked")
        val doubleClicked = Event<MouseEvent>("element-mouse-double-clicked")
        val entered = Event<MouseEvent>("element-mouse-entered")
        val exited = Event<MouseEvent>("element-mouse-exited")
        val dragged = Event<MouseEvent>("element-mouse-dragged")
        val moved = Event<MouseEvent>("element-mouse-moved")
        val scrolled = Event<MouseEvent>("element-mouse-scrolled")
        val pressed = Event<MouseEvent>("element-mouse-pressed")

        override fun close() {
            clicked.close()
            doubleClicked.close()
            entered.close()
            exited.close()
            dragged.close()
            moved.close()
            scrolled.close()
            pressed.close()
        }
    }

    class DropObserverables: AutoCloseable {
        val dropped = Event<DropEvent>("element-dropped")

        override fun close() {
            dropped.close()
        }
    }

    val drop = DropObserverables()
    val mouse = MouseObservables()

    class KeyboardObservables : AutoCloseable {
        val pressed = Event<KeyEvent>("element-keyboard-pressed")
        val released = Event<KeyEvent>("element-keyboard-released")
        val repeated = Event<KeyEvent>("element-keyboard-repeated")
        val character = Event<CharacterEvent>("element-keyboard-character")
        val focusGained = Event<FocusEvent>("element-keyboard-focus-gained")
        val focusLost = Event<FocusEvent>("element-keyboard-focus-lost")

        override fun close() {
            pressed.close()
            released.close()
            repeated.close()
            character.close()
            focusGained.close()
            focusLost.close()
        }
    }

    val keyboard = KeyboardObservables()

    class Layout {
        var zIndex = 0
        var screenX = 0.0
        var screenY = 0.0
        var screenWidth = 0.0
        var screenHeight = 0.0
        var growWidth = 0.0
        var growHeight = 0.0
        override fun toString(): String {
            return "Layout(screenX=$screenX, screenY=$screenY, screenWidth=$screenWidth, screenHeight=$screenHeight, growWidth=$growWidth, growHeight=$growHeight)"
        }

        val bounds: Rectangle
            get() {
                return Rectangle(screenWidth, screenHeight, screenWidth, screenHeight)
            }

        val boundsAtOrigin: Rectangle
            get() {
                return Rectangle(0.0, 0.0, screenWidth, screenHeight)
            }

        fun boundsAtOriginPadded(computedStyle: StyleSheet): Rectangle {
            return Rectangle(
                computedStyle.effectivePaddingLeft,
                computedStyle.effectivePaddingTop,
                screenWidth - (computedStyle.effectivePaddingLeft + computedStyle.effectivePaddingRight),
                max(0.0, screenHeight - (computedStyle.effectivePaddingTop + computedStyle.effectivePaddingBottom))
                )
        }
    }


    class Draw {
        var dirty = true
    }

    val draw = Draw()
    val layout = Layout()

    class ClassEvent(val source: Element, val `class`: ElementClass)
    class ClassObserverables : AutoCloseable {
        val classAdded = Event<ClassEvent>("element-class-added")
        val classRemoved = Event<ClassEvent>("element-class-removed")

        override fun close() {
            classAdded.close()
            classRemoved.close()
        }
    }

    val classEvents = ClassObserverables()


    var id: String? = null
    val classes: ObservableHashSet<ElementClass> = ObservableHashSet()
    val pseudoClasses: ObservableHashSet<ElementPseudoClass> = ObservableHashSet()

    var parent: Element? = null
    val children: ObservableCopyOnWriteArrayList<Element> = ObservableCopyOnWriteArrayList()
        get() = field

    var computedStyle: StyleSheet = StyleSheet(CompoundSelector.DUMMY)
    var style: StyleSheet? = null

    init {
        pseudoClasses.changed.listen {
            draw.dirty = true
        }
        classes.changed.listen {
            draw.dirty = true
            it.added.forEach {
                classEvents.classAdded.trigger(ClassEvent(this, it))
            }
            it.removed.forEach {
                classEvents.classRemoved.trigger(ClassEvent(this, it))
            }

        }

        children.changed.listen {
            draw.dirty = true
        }
    }


    /**
     * Determines whether the current element, or any of its ancestors, has a display style of `Display.NONE`.
     *
     * @return `true` if the element or any of its ancestors is hidden (has `Display.NONE` style), `false` otherwise.
     */
    fun isHidden() : Boolean {
        var current: Element? = this

        while (current != null) {
            if (current.computedStyle.display == Display.NONE) {
                return true
            }
            current = current.parent
        }

        return false
    }

    fun root(): Element {
        return parent?.root() ?: this
    }

    open fun append(builder: Element.() -> Unit) {
        this.builder()
    }

    open fun append(element: Element) {
        if (element !in children) {
            element.parent = this
            children.add(element)
        }
    }

    fun remove(element: Element): Element? {
        return if (element in children) {
            element.parent = null
            children.remove(element)
            element
        } else {
            null
        }
    }

    open fun draw(drawer: Drawer) {

    }

    fun filter(f: (Element) -> Boolean): List<Element> {
        val result = ArrayList<Element>()
        val stack = Stack<Element>()

        stack.add(this)
        while (!stack.isEmpty()) {
            val node = stack.pop()
            if (f(node)) {
                result.add(node)
                stack.addAll(node.children)
            }
        }
        return result
    }

    fun flatten(): List<Element> {
        val result = ArrayList<Element>()
        val stack = Stack<Element>()

        stack.add(this)
        while (!stack.isEmpty()) {
            val node = stack.pop()

            result.add(node)
            stack.addAll(node.children)
        }
        return result
    }

    fun previousSibling(): Element? {
        parent?.let { p ->
            p.childIndex(this)?.let {
                if (it > 0) {
                    return p.children[it - 1]
                }
            }
        }
        return null
    }

    fun childIndex(element: Element): Int? {
        if (element in children) {
            return children.indexOf(element)
        } else {
            return null
        }
    }

    fun ancestors(): List<Element> {
        var c = this
        val result = ArrayList<Element>()

        while (c.parent != null) {
            c.parent?.let {
                result.add(it)
                c = it
            }
        }
        return result
    }

    fun previous(): Element? {
        return parent?.let { p ->
            val index = p.children.indexOf(this)
            when (index) {
                -1, 0 -> null
                else -> p.children[index - 1]
            }
        }
    }

    fun next(): Element? {
        return parent?.let { p ->
            when (val index = p.children.indexOf(this)) {
                -1, p.children.size - 1 -> null
                else -> p.children[index + 1]
            }
        }
    }

    fun findNext(premise: (Element) -> Boolean): Element? {
        return parent?.let { p ->
            val index = p.children.indexOf(this)
            val siblingCount = p.children.size
            for (i in index + 1 until siblingCount) {
                if (premise(p.children[i])) {
                    return p.children[i]
                }
            }
            return null
        }
    }

    fun findPrevious(premise: (Element) -> Boolean): Element? {
        return parent?.let { p ->
            val index = p.children.indexOf(this)
            for (i in index - 1 downTo 0) {
                if (premise(p.children[i])) {
                    return p.children[i]
                }
            }
            return null
        }
    }


    fun move(steps: Int) {
        parent?.let { p ->
            if (steps != 0) {
                val index = p.children.indexOf(this)
                p.children.add(index + steps, this)
                if (steps > 0) {
                    p.children.removeAt(index)
                } else {
                    p.children.removeAt(index + 1)
                }
            }
        }
    }

    fun findFirst(element: Element, matches: (Element) -> Boolean): Element? {
        if (matches.invoke(element)) {
            return element
        } else {
            element.children.forEach { c ->
                findFirst(c, matches)?.let { return it }
            }
            return null
        }
    }

    inline fun <reified T> elementWithId(id: String): T? {
        return findFirst(this) { e -> e.id == id && e is T } as T
    }

    val screenPosition: Vector2
        get() = Vector2(layout.screenX, layout.screenY)

    val screenArea: Rectangle
        get() = Rectangle(Vector2(layout.screenX,
                layout.screenY),
                layout.screenWidth,
                layout.screenHeight)


    override fun close() {
        disposed = true
        classes.close()
        pseudoClasses.close()
        classEvents.close()
        mouse.close()
        keyboard.close()
        drop.close()
        for (child in children) {
            child.close()
        }
        children.clear()
    }

}

fun Element.requestRedraw() {
    draw.dirty = true
}

fun Element.disable() {
    pseudoClasses.add(disabled)
    requestRedraw()
}

fun Element.enable() {
    pseudoClasses.remove(disabled)
    requestRedraw()
}

fun Element.isDisabled(): Boolean = disabled in pseudoClasses

fun Element.findAll(predicate: (Element) -> Boolean): List<Element> {
    val results = mutableListOf<Element>()
    visit {
        if (predicate(this)) {
            results.add(this)
        }
    }
    return results
}

fun Element.findAllVisible(predicate: (Element) -> Boolean): List<Element> {
    val results = mutableListOf<Element>()
    visitVisible {
        if (predicate(this)) {
            results.add(this)
        }
    }
    return results
}

fun Element.visit(function: Element.() -> Unit) {
    this.function()
    children.forEach { it.visit(function) }
}

fun Element.visitVisible(function: Element.() -> Unit) {
    if (this.computedStyle.display != Display.NONE) {
        this.function()
        children.forEach { it.visitVisible(function) }
    }
}