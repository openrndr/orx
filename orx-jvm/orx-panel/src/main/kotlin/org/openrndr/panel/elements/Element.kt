package org.openrndr.panel.elements

import org.openrndr.*
import org.openrndr.draw.Drawer
import org.openrndr.events.Event
import org.openrndr.math.Vector2
import org.openrndr.panel.collections.ObservableCopyOnWriteArrayList
import org.openrndr.panel.collections.ObservableHashSet
import org.openrndr.panel.style.CompoundSelector
import org.openrndr.panel.style.Display
import org.openrndr.panel.style.LinearDimension
import org.openrndr.panel.style.outlineOffset
import org.openrndr.panel.style.outlineWidth
import org.openrndr.panel.style.boxShadow
import org.openrndr.panel.style.transform
import org.openrndr.panel.style.transformOrigin
import org.openrndr.panel.style.ScrollBehavior
import org.openrndr.panel.style.StyleSheet
import org.openrndr.panel.style.clipsContent
import org.openrndr.panel.style.display
import org.openrndr.panel.style.scrollBehavior
import org.openrndr.panel.style.scrollableX
import org.openrndr.panel.style.scrollableY
import org.openrndr.panel.style.position
import org.openrndr.panel.style.top
import org.openrndr.shape.Rectangle

import java.util.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.max


@JvmInline
value class ElementClass(val name: String)

@JvmRecord
data class ElementPseudoClass(val name: String)

@JvmRecord
data class ElementType(val name: String)

val disabled = ElementPseudoClass("disabled")

class FocusEvent


open class Element(val type: ElementType) : AutoCloseable {

    var disposed = false
        private set

    /** an offset of the element from where it is laid out, while it moves there, see [org.openrndr.panel.animateMoveFrom] */
    var layoutOffset: org.openrndr.math.Vector2 = org.openrndr.math.Vector2.ZERO

    /** the bindings of values of the element, checked every frame, see [org.openrndr.panel.binding.bindValue] */
    val valueBindings = mutableListOf<org.openrndr.panel.binding.PropertyBinding<*>>()

    var scrollTop = 0.0
        set(value) {
            if (value != field) {
                field = value
                requestLayout()
            }
        }

    /**
     * The largest [scrollTop]: how far the content of the element extends beyond its bottom, as found by the last
     * layout.
     */
    val maxScrollTop: Double
        get() = max(0.0, layout.scrollHeight - layout.screenHeight)

    /** how far the content of an element that scrolls is scrolled to the left, in pixels */
    var scrollLeft = 0.0
        set(value) {
            if (value != field) {
                field = value
                requestLayout()
            }
        }

    /** true when the computed style of this element has to be computed again */
    internal var styleDirty = true

    /** true when the computed styles of this element and its descendants have to be computed again */
    internal var subtreeStyleDirty = true

    /** the inline style and its version when the computed style was computed */
    internal var computedFromStyle: StyleSheet? = null
    internal var computedFromVersion = -1

    /** true when the computed style was computed since the transitions were applied */
    internal var styleFresh = false

    /** the styles of this element and its descendants are computed again before the next redraw */
    fun invalidateStyle() {
        subtreeStyleDirty = true
        (root() as? Body)?.controlManager?.stylesPending = true
    }

    /**
     * The layout is computed again before the next redraw, of the element and its descendants, and of the elements
     * around it as far as its size can change theirs, see [org.openrndr.panel.document.Document.layoutRequests]
     */
    fun requestLayout() {
        (root() as? Body)?.controlManager?.document?.layoutRequests?.add(this)
    }

    /** the text of the control manager of the element, null when it is not in one */
    val panelText: org.openrndr.panel.PanelText?
        get() = (root() as? Body)?.controlManager?.text

    private val attributeValues = LinkedHashMap<String, String>()

    /** the attributes of the element, which attribute selectors like `[label="OK"]` select, see [setAttribute] */
    val attributes: Map<String, String> get() = attributeValues

    /** the value of the attribute [name], null when the element does not have it */
    fun attribute(name: String): String? = attributeValues[name]

    /** sets the attribute [name] to [value], or removes it when [value] is null; the styles are computed again */
    fun setAttribute(name: String, value: String?) {
        val old = if (value == null) attributeValues.remove(name) else attributeValues.put(name, value)
        if (old != value) invalidateStyle()
    }

    /** what the element does in drag and drop, see [DragAndDrop] */
    val dnd = DragAndDrop()

    /** the text shown next to the pointer when it rests on the element, null for none */
    open var tooltip: String? = null

    /** builds the menu that opens when the element is clicked with the right button, null for none */
    var contextMenu: (ContextMenuBuilder.() -> Unit)? = null

    /**
     * Scrolls the content to [left] and [top], within the content: smoothly when the [StyleSheet.scrollBehavior] of
     * the element is smooth and [smooth] is not false, and the element is in a control manager
     */
    fun scrollTo(left: Double = scrollTarget.x, top: Double = scrollTarget.y, smooth: Boolean? = null) {
        val animate = smooth ?: (computedStyle.scrollBehavior == ScrollBehavior.Smooth)
        val scrolling = (root() as? Body)?.controlManager?.smoothScrolling
        if (animate && scrolling != null) {
            scrolling.scrollTo(this, left, top)
        } else {
            scrolling?.cancel(this)
            scrollLeft = left.coerceIn(0.0, maxScrollLeft)
            scrollTop = top.coerceIn(0.0, maxScrollTop)
        }
        draw.dirty = true
        (root() as? Body)?.controlManager?.checkForManualRedraw()
    }

    /** the sticky descendants of this element, which scrolls them, that are stuck at [viewTop], its top */
    private fun stuckAtTop(viewTop: Double): List<Element> {
        val result = mutableListOf<Element>()
        fun visit(element: Element) {
            for (child in element.children) {
                val cs = child.computedStyle
                if (cs.display == Display.NONE) continue
                if (cs.position == org.openrndr.panel.style.Position.STICKY &&
                    kotlin.math.abs(child.layout.screenY - viewTop - cs.top.let { (it as? org.openrndr.panel.style.LinearDimension.PX)?.value ?: 0.0 }) < 0.5
                ) {
                    result.add(child)
                }
                // the elements that scroll their own content have their own sticky elements
                if (!cs.clipsContent) visit(child)
            }
        }
        visit(this)
        return result
    }

    /** where the content is scrolled to: the end of smooth scrolling, or the scroll position */
    val scrollTarget: Vector2
        get() = (root() as? Body)?.controlManager?.smoothScrolling?.target(this) ?: Vector2(scrollLeft, scrollTop)

    /**
     * Scrolls the elements that scroll this element so that it is in view, with [margin] pixels around it, each by
     * its [StyleSheet.scrollBehavior]
     */
    fun scrollIntoView(margin: Double = 0.0) {
        // the area of the element when its ancestors are scrolled to where they scroll to
        var area = screenArea
        var ancestor = parent
        while (ancestor != null) {
            val cs = ancestor.computedStyle
            if (cs.clipsContent) {
                val border = ancestor.layout.borderWidth
                val padded = Rectangle(ancestor.layout.screenX + border, ancestor.layout.screenY + border,
                    ancestor.layout.screenWidth - 2.0 * border, ancestor.layout.screenHeight - 2.0 * border)
                // sticky elements that are stuck at the top of the view cover it, other than this element and the
                // elements around it
                val around = generateSequence(this) { it.parent }.toSet()
                val covered = ancestor.stuckAtTop(padded.y).filter { it !in around }
                    .maxOfOrNull { it.layout.screenY + it.layout.screenHeight - padded.y } ?: 0.0
                val view = Rectangle(padded.x, padded.y + covered, padded.width, max(0.0, padded.height - covered))
                val target = ancestor.scrollTarget
                // the area of the element when the ancestor is at its target
                val at = area.movedBy(Vector2(ancestor.scrollLeft - target.x, ancestor.scrollTop - target.y))
                fun along(start: Double, length: Double, viewStart: Double, viewLength: Double): Double = when {
                    // an element larger than the view shows its start
                    length + 2.0 * margin > viewLength || start - margin < viewStart -> start - margin - viewStart
                    start + length + margin > viewStart + viewLength -> start + length + margin - (viewStart + viewLength)
                    else -> 0.0
                }
                val dx = if (cs.scrollableX) along(at.x, at.width, view.x, view.width) else 0.0
                val dy = if (cs.scrollableY) along(at.y, at.height, view.y, view.height) else 0.0
                val left = (target.x + dx).coerceIn(0.0, ancestor.maxScrollLeft)
                val top = (target.y + dy).coerceIn(0.0, ancestor.maxScrollTop)
                if (left != target.x || top != target.y) {
                    ancestor.scrollTo(left, top)
                }
                // for the ancestors around it the element is where the ancestor scrolls it to
                area = at.movedBy(Vector2(target.x - left, target.y - top))
            }
            ancestor = ancestor.parent
        }
    }

    /**
     * The largest [scrollLeft]: how far the content of the element extends beyond its right side, as found by the
     * last layout.
     */
    val maxScrollLeft: Double
        get() = max(0.0, layout.scrollWidth - layout.screenWidth)
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

    /** the height of the content of the element when it is [width] wide, for content that wraps; null when it has none */
    open fun heightForWidth(width: Double): Double? = null

    /** false for elements that place their children themselves, like the spans of [RichText] */
    open val laysOutChildren: Boolean get() = true

    /** the size hints at the last layout; an element drawn again whose hints changed is laid out again */
    internal var laidOutHints: Pair<Double?, Double?>? = null

    /**
     * The area the element draws in, on the screen: its box and what is drawn around it, like knobs, focus rings and
     * outlines. Only the areas of the elements that changed are drawn again, a control that draws further than this
     * gives a larger area.
     */
    open val paintArea: Rectangle
        get() {
            val cs = computedStyle
            val outline = ((cs.outlineWidth as? LinearDimension.PX)?.value ?: 0.0) +
                    ((cs.outlineOffset as? LinearDimension.PX)?.value ?: 0.0).coerceAtLeast(0.0)
            val grow = 12.0 + outline + cs.boxShadow.extent
            val area = screenArea
            val grown = Rectangle(area.x - grow, area.y - grow, area.width + 2.0 * grow, area.height + 2.0 * grow)
            // transformed, by its own transform and those of its ancestors
            return screenTransform?.let { transformedBounds(grown, it) } ?: grown
        }

    /** the paint area of the element when it was last drawn, null when it was not */
    internal var drawnArea: Rectangle? = null

    class MouseObservables : AutoCloseable {
        val clicked = Event<MouseEvent>("element-mouse-clicked")
        val doubleClicked = Event<MouseEvent>("element-mouse-double-clicked")
        val entered = Event<MouseEvent>("element-mouse-entered")
        val exited = Event<MouseEvent>("element-mouse-exited")
        val dragged = Event<MouseEvent>("element-mouse-dragged")
        val moved = Event<MouseEvent>("element-mouse-moved")
        val scrolled = Event<MouseEvent>("element-mouse-scrolled")
        val pressed = Event<MouseEvent>("element-mouse-pressed")

        /** the button that was pressed on the element is released, also after a drag, where it is no click */
        val released = Event<MouseEvent>("element-mouse-released")

        override fun close() {
            clicked.close()
            doubleClicked.close()
            entered.close()
            exited.close()
            dragged.close()
            moved.close()
            scrolled.close()
            pressed.close()
            released.close()
        }
    }

    class DropObserverables : AutoCloseable {
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

    /**
     * The result of the layout of an element. Sizes are border-box sizes: they include the padding and the border.
     */
    class Layout {
        var zIndex = 0
        var screenX = 0.0
        var screenY = 0.0
        var screenWidth = 0.0
        var screenHeight = 0.0

        /** the padding in pixels, resolved by the layout */
        var paddingLeft = 0.0
        var paddingTop = 0.0
        var paddingRight = 0.0
        var paddingBottom = 0.0

        /** the width of the border in pixels, resolved by the layout */
        var borderWidth = 0.0

        var growWidth = 0.0
        var growHeight = 0.0

        /**
         * The height of the content including the padding, for elements that scroll their content
         */
        var scrollHeight = 0.0

        /**
         * The width of the content including the padding, for elements that scroll their content
         */
        var scrollWidth = 0.0
        override fun toString(): String {
            return "Layout(screenX=$screenX, screenY=$screenY, screenWidth=$screenWidth, screenHeight=$screenHeight, growWidth=$growWidth, growHeight=$growHeight)"
        }

        val bounds: Rectangle
            get() {
                return Rectangle(screenX, screenY, screenWidth, screenHeight)
            }

        val boundsAtOrigin: Rectangle
            get() {
                return Rectangle(0.0, 0.0, screenWidth, screenHeight)
            }

        @Deprecated("use contentBoundsAtOrigin", ReplaceWith("contentBoundsAtOrigin"))
        @Suppress("UNUSED_PARAMETER")
        fun boundsAtOriginPadded(computedStyle: StyleSheet): Rectangle = contentBoundsAtOrigin

        /** the area inside the border and the padding */
        val contentBounds: Rectangle
            get() = contentBoundsAtOrigin.movedBy(Vector2(screenX, screenY))

        val contentBoundsAtOrigin: Rectangle
            get() = Rectangle(
                borderWidth + paddingLeft,
                borderWidth + paddingTop,
                max(0.0, screenWidth - paddingLeft - paddingRight - 2.0 * borderWidth),
                max(0.0, screenHeight - paddingTop - paddingBottom - 2.0 * borderWidth)
            )

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
        set(value) {
            if (value != field) {
                field = value
                invalidateStyle()
            }
        }
    val classes: ObservableHashSet<ElementClass> = ObservableHashSet()
    val pseudoClasses: ObservableHashSet<ElementPseudoClass> = ObservableHashSet()

    var parent: Element? = null
    val children: ObservableCopyOnWriteArrayList<Element> = ObservableCopyOnWriteArrayList()
        get() = field

    var computedStyle: StyleSheet = StyleSheet(CompoundSelector.DUMMY)
    var style: StyleSheet? = null

    init {
        // states and classes change the styles of the element and its descendants, the element is drawn again when
        // its computed style changes
        pseudoClasses.changed.listen {
            invalidateStyle()
        }
        classes.changed.listen {
            invalidateStyle()
            it.added.forEach {
                classEvents.classAdded.trigger(ClassEvent(this, it))
            }
            it.removed.forEach {
                classEvents.classRemoved.trigger(ClassEvent(this, it))
            }

        }

        children.changed.listen {
            draw.dirty = true
            // selectors of siblings match again
            for (child in children) child.subtreeStyleDirty = true
            invalidateStyle()
            requestLayout()
        }
    }


    /**
     * Determines whether the current element, or any of its ancestors, has a display style of `Display.NONE`.
     *
     * @return `true` if the element or any of its ancestors is hidden (has `Display.NONE` style), `false` otherwise.
     */
    fun isHidden(): Boolean {
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

    open fun replace(builder: Element.() -> Unit) {
        for (child in children) {
            child.close()
        }
        children.clear()
        this.builder()
    }


    @OptIn(ExperimentalContracts::class)
    inline fun append(builder: Element.() -> Unit) {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
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

    /**
     * Called by the layouter once the size of this element is known and before its children are positioned. Can be
     * used to adjust [layout] to the element's size.
     */
    internal open fun sized() {
    }

    /**
     * Called in every frame before the styles are computed, by the control manager, so that the element can change its
     * children for what is in view, like a [VirtualList]
     */
    open fun prepare() {
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
        get() = Rectangle(
            Vector2(
                layout.screenX,
                layout.screenY
            ),
            layout.screenWidth,
            layout.screenHeight
        )


    override fun close() {
        disposed = true
        valueBindings.toList().forEach { it.close() }
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

/**
 * Draws the element again, what it shows changed. The layout is computed again when the size hints of the element
 * changed; changes to the children, the classes, states and styles of elements and to scrolling lay out again by
 * themselves, other changes that move or size elements call [Element.requestLayout].
 */
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

/**
 * Applies the provided styling function to the element's stylesheet. If the element does not
 * already have a stylesheet, a new one is created and the function is applied to it.
 *
 * @param function A lambda function defining the style rules to be applied to the element's stylesheet.
 * @return The updated or newly created stylesheet associated with the element.
 */
fun Element.style(function: StyleSheet.() -> Unit): StyleSheet {
    if (style == null) {
        style = StyleSheet(CompoundSelector.DUMMY)
    }
    style!!.function()
    return style!!
}

/**
 * A property of an element that is the attribute [name], with [initial] as its value until it is set, so that
 * selectors can select it, like the labels of controls: `button[label="Save"]`
 */
fun attribute(name: String, initial: String) = AttributeProperty(name, initial)

class AttributeProperty(val name: String, val initial: String) {
    operator fun getValue(element: Element, property: kotlin.reflect.KProperty<*>): String = element.attribute(name) ?: initial

    operator fun setValue(element: Element, property: kotlin.reflect.KProperty<*>, value: String) {
        element.setAttribute(name, value)
    }
}

/**
 * The transform of the element itself, in the coordinates of the window, around its transform origin; null when it
 * has none
 */
val Element.ownTransform: org.openrndr.math.Matrix44?
    get() {
        val t = computedStyle.transform
        val offset = layoutOffset
        // an element that moves to where it is laid out, see [org.openrndr.panel.animateMoveFrom]
        if (offset != org.openrndr.math.Vector2.ZERO) {
            val own = if (t.functions.isEmpty()) null else run {
                val size = org.openrndr.math.Vector2(layout.screenWidth, layout.screenHeight)
                val o = org.openrndr.math.Vector2(layout.screenX, layout.screenY) + computedStyle.transformOrigin * size
                org.openrndr.math.transforms.transform { translate(o.x, o.y); multiply(t.matrix(size)); translate(-o.x, -o.y) }
            }
            val move = org.openrndr.math.transforms.transform { translate(offset.x, offset.y) }
            return if (own == null) move else move * own
        }
        if (t.functions.isEmpty()) return null
        val size = org.openrndr.math.Vector2(layout.screenWidth, layout.screenHeight)
        val o = org.openrndr.math.Vector2(layout.screenX, layout.screenY) + computedStyle.transformOrigin * size
        return org.openrndr.math.transforms.transform {
            translate(o.x, o.y)
            multiply(t.matrix(size))
            translate(-o.x, -o.y)
        }
    }

/** the transform of the element by its own transform and those of its ancestors, null when there are none */
val Element.screenTransform: org.openrndr.math.Matrix44?
    get() {
        var result: org.openrndr.math.Matrix44? = null
        for (e in generateSequence(this) { it.parent }) {
            val t = e.ownTransform ?: continue
            result = if (result == null) t else t * result
        }
        return result
    }

/** the bounds of [rectangle] transformed by [transform] */
internal fun transformedBounds(rectangle: Rectangle, transform: org.openrndr.math.Matrix44): Rectangle {
    val corners = listOf(rectangle.corner, rectangle.corner + org.openrndr.math.Vector2(rectangle.width, 0.0),
        rectangle.corner + org.openrndr.math.Vector2(0.0, rectangle.height), rectangle.corner + rectangle.dimensions)
        .map { (transform * it.xy01).xy }
    val x0 = corners.minOf { it.x }
    val y0 = corners.minOf { it.y }
    return Rectangle(x0, y0, corners.maxOf { it.x } - x0, corners.maxOf { it.y } - y0)
}

/** true when the point [position] of the window is in the box of the element as it is drawn, transformed */
fun Element.containsScreenPoint(position: org.openrndr.math.Vector2): Boolean {
    val t = screenTransform ?: return position in screenArea
    return (t.inversed * position.xy01).xy in screenArea
}

/** [position], a point of the window, in the coordinates of the element before its transforms */
fun Element.localPosition(position: org.openrndr.math.Vector2): org.openrndr.math.Vector2 =
    screenTransform?.let { (it.inversed * position.xy01).xy } ?: position
