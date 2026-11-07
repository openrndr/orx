package org.openrndr.panel.document

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

    /** true when the layout has to be computed again, because elements changed in ways that move or size them */
    var layoutDirty = true

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
        if (sheetsChanged || difference > 1 || layoutDirty || adjusted) {
            // layout can mark the layout dirty again, scrolling is limited to the content
            layoutDirty = false
            layouter.layout(body)
            layoutDirty = false
            return true
        }
        return false
    }

    override fun close() {
        controlManager = null
        styleSheets.close()
        body.close()
    }
}