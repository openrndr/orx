package org.openrndr.extra.axidraw
import org.openrndr.extra.composition.Composition
import org.openrndr.extra.composition.GroupNode
import org.openrndr.extra.composition.findGroups
import org.openrndr.extra.svg.toSVG
import java.io.File

/**
 * Axidraw layer mode. The [command] argument will be prepended to the layer name.
 */
@Suppress("unused")
enum class AxiLayerMode(val command: String) {
    /**
     * The default mode prepends nothing.
     */
    DEFAULT(""),

    /**
     * Layer names starting with `%` are not plotted.
     */
    IGNORE("%"),

    /**
     * Layer names starting with `!` trigger a pause.
     */
    PAUSE("!")
}

/**
 * Configure an SVG layer name. Certain character sequences are used
 * by the Axidraw software to control layer speed, height and delay.
 * Other characters make the layer be ignored, or trigger a pause.
 * The arguments in this function provide a typed approach to construct
 * the layer name.
 * See https://wiki.evilmadscientist.com/AxiDraw_Layer_Control
 *
 * @param layerName Human-readable layer name. Multiple layer can use the same name.
 * @param penSpeed Pen down speed (1..100)
 * @param penHeight Pen down height (0..100)
 * @param plotDelay Delay before plotting this layer, in milliseconds
 * @param layerMode The plotting mode for this layer. See [AxiLayerMode].
 */
fun GroupNode.configure(
    layerName: String = "layer",
    penSpeed: Int? = null,
    penHeight: Int? = null,
    plotDelay: Int? = null,
    layerMode: AxiLayerMode = AxiLayerMode.DEFAULT
) {
    val layerNumber = (parent?.findGroups()?.size ?: 2) - 1

    require(penSpeed == null || penSpeed in 1..100) { "Speed out of 1 .. 100 range" }
    val actualSpeed = penSpeed?.let { "+S$it" } ?: ""

    require(penHeight == null || penHeight in 0..100) { "Height out of 0 .. 100 range" }
    val actualHeight = penHeight?.let { "+H$it" } ?: ""

    require(plotDelay == null || plotDelay > 0) { "Delay value should null or above 0" }
    val actualDelay = plotDelay?.let { "+D$it" } ?: ""

    attributes["inkscape:groupmode"] = "layer"

    attributes["inkscape:label"] = layerMode.command + layerNumber +
            actualSpeed + actualHeight + actualDelay + " " + layerName
}

/**
 * Save a [Composition] to an Inkscape file. Includes expected XML namespaces
 * and sets an XML header with the view window size. Strips an extra wrapping `<g>` tag to
 * make special layer names work with the Axidraw pen plotter.
 *
 * @param file Should point to the desired file name and path.
 * @param postProcess Optional function to do post-processing on the SVG XML before saving it.
 */
fun Composition.saveToInkscapeFile(
    file: File,
    postProcess: (String) -> String = { xml -> xml }
) {
    namespaces["xmlns:inkscape"] = "http://www.inkscape.org/namespaces/inkscape"
    namespaces["xmlns:sodipodi"] = "http://sodipodi.sourceforge.net/DTD/sodipodi-0.dtd"
    namespaces["xmlns:svg"] = "http://www.w3.org/2000/svg"

    val svg = StringBuilder(toSVG())

    val header = """
        <sodipodi:namedview
            id="namedview7112"
            pagecolor="#ffffff"
            bordercolor="#eeeeee"
            borderopacity="1"
            inkscape:showpageshadow="0"
            inkscape:pageopacity="0"
            inkscape:pagecheckerboard="0"
            inkscape:deskcolor="#d1d1d1"
            showgrid="false"
            inkscape:zoom="1.0"
            inkscape:cx="${bounds.width.value / 2}"
            inkscape:cy="${bounds.height.value / 2}"
            inkscape:window-width="${bounds.width}"
            inkscape:window-height="${bounds.height}"
            inkscape:window-x="0"
            inkscape:window-y="0"
            inkscape:window-maximized="1"
            inkscape:current-layer="openrndr-svg" />        
    """.trimIndent()

    // Remove the wrapping <g>, otherwise layers don't work.
    // Also remove duplicated <g><g> and </g></g> which show up when
    // drawing a composition into another composition.
    val updated = svg.replace(
        Regex("""(<g\s?>(.*)</g>)""", RegexOption.DOT_MATCHES_ALL), "$2"
    ).replace(
        "(<g >\\W?)+<g ".toRegex(setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL)),
        "<g "
    ).replace(
        "(\\W?</g>)+".toRegex(setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL)),
        "\n</g>"
    ).replace(
        Regex("""(<svg.*?>)""", RegexOption.DOT_MATCHES_ALL), "$1$header"
    )
    file.writeText(postProcess(updated))
}
