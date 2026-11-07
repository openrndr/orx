package org.openrndr.panel.document

import org.openrndr.panel.style.display
import org.openrndr.panel.style.height
import org.openrndr.panel.style.width
import org.openrndr.panel.ControlManager
import org.openrndr.panel.collections.ObservableArrayList
import org.openrndr.panel.elements.Body
import org.openrndr.panel.layout.Layouter
import org.openrndr.panel.style.StyleSheet
import org.openrndr.panel.style.flatten

/**
 * Represents a document with a hierarchical structure of elements,
 * styles, and layout capabilities. The `Document` class manages
 * the layout and styling system by utilizing a `Layouter` and an
 * observable list of stylesheets.
 *
 * Features include:
 * - The ability to manage styles through a list of `StyleSheet` objects.
 * - Automatic tracking of changes to stylesheets to ensure layout updates are performed when necessary.
 * - Support for laying out the document body based on computed styles from the provided stylesheets.
 * - A controllable lifecycle to clean up resources when the document is no longer in use.
 *
 * Implements [AutoCloseable] to ensure that resources such as `controlManager`,
 * `styleSheets`, and `body` are cleaned up appropriately.
 */
class Document : AutoCloseable {
    var controlManager: ControlManager? = null
    val styleSheets = ObservableArrayList<StyleSheet>()
    var body: Body = Body(null)
    val layouter = Layouter()

    init {
        styleSheets.changed.listen {
            dirtySheets = true
        }
    }
    private var dirtySheets = true

    /**
     * What the media queries of the style sheets are evaluated against; when it changes in a way that changes which
     * style sheets apply, the styles are computed again
     */
    var media: org.openrndr.extra.style.CssMediaEnvironment?
        get() = layouter.media
        set(value) {
            if (value != layouter.media) {
                val before = layouter.styleSheets.map { it.media }.distinct().filterNotNull()
                    .associateWith { value != null && org.openrndr.extra.style.cssMediaMatches(it, value) }
                val previous = layouter.media
                layouter.media = value
                val changed = before.any { (query, now) -> previous == null || org.openrndr.extra.style.cssMediaMatches(query, previous) != now }
                // lengths relative to the window change with its size
                val resized = previous == null || previous.width != value?.width || previous.height != value.height
                if (changed || (resized && usesViewportUnits())) dirtySheets = true
            }
        }

    /** true when the style sheets or the inline styles have lengths relative to the window, like 50vw */
    private fun usesViewportUnits(): Boolean {
        fun relative(value: Any?) = when (value) {
            is org.openrndr.panel.style.LinearDimension.Relative -> value.unit.startsWith("v")
            is org.openrndr.panel.style.LinearDimension.Calc -> Regex("v(w|h|min|max)\\b").containsMatchIn(value.expression)
            else -> false
        }
        return layouter.styleSheets.any { s -> s.properties.values.any { relative(it.value) } } ||
                body.filterAll { e -> e.style?.properties?.values?.any { relative(it.value) } == true }
    }

    /** true when the whole layout has to be computed again, like when the window is resized */
    var layoutDirty = true

    /** the elements that asked for their layout to be computed again, see [Element.requestLayout] */
    val layoutRequests = LinkedHashSet<org.openrndr.panel.elements.Element>()

    /**
     * The elements that were laid out again in the last layout, with their descendants, when not everything was laid
     * out; null when everything was
     */
    var laidOut: List<org.openrndr.panel.elements.Element>? = null
        private set

    /**
     * The element whose subtree has to be laid out again for a change of [element]: the nearest of the element and
     * its ancestors with a width and a height that do not depend on its content, the body at the latest. What changes
     * inside it can not change its size, nor the layout of what is around it.
     */
    private fun boundary(element: org.openrndr.panel.elements.Element): org.openrndr.panel.elements.Element {
        fun definite(d: org.openrndr.panel.style.LinearDimension) = d is org.openrndr.panel.style.LinearDimension.PX ||
                d is org.openrndr.panel.style.LinearDimension.Percent || d is org.openrndr.panel.style.LinearDimension.Calc
        var e = element
        while (true) {
            val parent = e.parent ?: return e
            val cs = e.computedStyle
            if (cs.display != org.openrndr.panel.style.Display.NONE && definite(cs.width) && definite(cs.height)) return e
            e = parent
        }
    }

    /**
     * Computes the styles that can have changed, and the layout when it can have changed. [adjustStyles] changes the
     * computed styles before the layout, like transitions and animations do, and gives true when it changed properties
     * that lay out.
     * @return true when the layout was computed
     */
    fun layout(adjustStyles: () -> Boolean = { false }): Boolean {
        val sheetsChanged = dirtySheets
        if (dirtySheets) {
            layouter.styleSheets.clear()
            layouter.styleSheets.addAll(styleSheets.flatMap { it.flatten() })
            dirtySheets = false
        }

        val difference = layouter.updateStyles(body, forced = sheetsChanged)
        val adjusted = adjustStyles()
        // the elements whose changes can change the layout: those that asked, and the parents of those whose styles
        // changed their sizes or places
        val starts = layoutRequests.toList() + layouter.layoutChanges.map { it.parent ?: it }
        layoutRequests.clear()
        if (sheetsChanged || layoutDirty || adjusted) {
            // layout can ask for layout again, scrolling is limited to the content
            layoutDirty = false
            layouter.layout(body)
            layoutDirty = false
            layoutRequests.clear()
            laidOut = null
            return true
        }
        if (starts.isEmpty()) return false
        // the subtrees that the changes are in, those inside others are laid out with them
        val boundaries = starts.filter { it.root() === body }.map(::boundary).distinct()
        val outer = boundaries.filter { b -> boundaries.none { o -> o !== b && generateSequence(b.parent) { it.parent }.any { it === o } } }
        if (outer.any { it === body }) {
            layouter.layout(body)
            laidOut = null
        } else {
            for (b in outer) layouter.layout(b)
            laidOut = outer
        }
        layoutRequests.clear()
        return true
    }

    override fun close() {
        controlManager = null
        styleSheets.close()
        body.close()
    }
}

/** true when [predicate] holds for [this] or one of its descendants */
private fun org.openrndr.panel.elements.Element.filterAll(predicate: (org.openrndr.panel.elements.Element) -> Boolean): Boolean =
    predicate(this) || children.any { it.filterAll(predicate) }
