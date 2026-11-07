package org.openrndr.panel

import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.panel.document.Document
import org.openrndr.panel.style.CompoundSelector
import org.openrndr.panel.style.StyleSheet
import org.openrndr.panel.style.styleSheets
import java.io.File

private val logger = KotlinLogging.logger {}

/**
 * A CSS file whose style sheets are in a document, see [ControlManager.cssFile]. When it is [watched], the style sheets
 * are read again when the file changes, in the place of the style sheets that were read before, so that they keep
 * their place in the cascade.
 */
class CssFile internal constructor(val file: File, val watched: Boolean, private val document: Document) {
    /** marks the place of the style sheets of the file in the document, it selects nothing */
    private val anchor = StyleSheet(CompoundSelector.parse(":not(*)")!!)

    /** the style sheets that were read last */
    var styleSheets: List<StyleSheet> = emptyList()
        private set

    /** the number of times the file was read, for tests */
    var reads = 0
        private set

    private var modified = Long.MIN_VALUE
    private var checked = Double.NEGATIVE_INFINITY

    init {
        document.styleSheets.add(anchor)
        reload()
    }

    /** reads the file again when it changed since it was read, at most every [interval] seconds */
    internal fun check(now: Double, interval: Double = 0.25): Boolean {
        if (!watched || now - checked < interval) return false
        checked = now
        return reload()
    }

    /** reads the file when it changed, or was never read; true when the style sheets changed */
    fun reload(): Boolean {
        val lastModified = if (file.exists()) file.lastModified() else -1L
        if (lastModified == modified) return false
        modified = lastModified
        val sheets = if (lastModified < 0L) {
            logger.warn { "the CSS file '${file.path}' does not exist${if (watched) ", its style sheets are read once it does" else ""}" }
            emptyList()
        } else try {
            styleSheets(file.readText())
        } catch (e: Exception) {
            logger.warn { "could not read the CSS file '${file.path}': ${e.message}" }
            return false
        }
        // the style sheets of the file replace those it had, after the anchor
        val list = document.styleSheets
        val old = styleSheets.toSet()
        val kept = list.filter { it !in old }
        val index = kept.indexOf(anchor) + 1
        list.clear()
        list.addAll(kept.subList(0, index) + sheets + kept.subList(index, kept.size))
        styleSheets = sheets
        reads++
        logger.info { "read ${sheets.size} style sheets from '${file.path}'" }
        return true
    }

    /** removes the style sheets of the file from the document */
    fun remove() {
        val old = styleSheets.toSet() + anchor
        document.styleSheets.removeIf { it in old }
        styleSheets = emptyList()
    }
}
