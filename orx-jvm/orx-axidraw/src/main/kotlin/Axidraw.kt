import offset.offset
import org.openrndr.color.ColorRGBa
import org.openrndr.dialogs.openFileDialog
import org.openrndr.dialogs.saveFileDialog
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.extra.composition.*
import org.openrndr.extra.imageFit.fit
import org.openrndr.extra.parameters.*
import org.openrndr.extra.svg.toSVG
import org.openrndr.math.IntVector2
import org.openrndr.shape.IntRectangle
import org.openrndr.shape.SegmentJoin
import org.openrndr.shape.Shape
import java.io.File
import java.util.*
import kotlin.io.path.createTempFile

/**
 * Axidraw reordering optimization types.
 * See: https://axidraw.com/doc/cli_api/#reordering
 */
@Suppress("unused")
enum class AxidrawOptimizationTypes(val id: Int) {
    /**
     * No optimization. Strictly preserve file order.
     */
    None(4),

    /**
     * Least; Only connect adjoining paths.
     */
    ConnectPaths(0),

    /**
     * Basic; Also reorder paths for speed
     */
    ReorderPaths(1),

    /**
     * Full; Also allow path reversal
     */
    ReversePaths(2)
}

@Suppress("unused")
enum class AxidrawModel(val id: Int) {
    AxiDrawV2(1),
    AxidrawV3(1),
    AxidrawSE_A4(1),
    AxiDrawV3_A3(2),
    AxidrawSE_A3(2),
    AxiDrawV3_XLX(3),
    AxiDrawMiniKit(4),
    AxiDrawSE_A1(5),
    AxiDrawSE_A2(6),
    AxiDrawV3_B6(7),
}

@Suppress("unused")
enum class AxidrawServo(val id: Int) {
    Standard(2),
    Brushless(3),
}

@Suppress("unused")
enum class PaperSize(val size: IntVector2) {
    `A-1`(IntVector2(1682, 2378)),
    `A-2`(IntVector2(1189, 1682)),
    A0(IntVector2(841, 1189)),
    A1(IntVector2(594, 841)),
    A2(IntVector2(420, 594)),
    A3(IntVector2(297, 420)),
    A4(IntVector2(210, 297)),
    A5(IntVector2(148, 210)),
    A6(IntVector2(105, 148)),
    A7(IntVector2(74, 105)),
    A8(IntVector2(52, 74)),
    A9(IntVector2(37, 52)),
    A10(IntVector2(26, 37))
}

enum class PaperOrientation {
    LANDSCAPE,
    PORTRAIT
}

/**
 * Class to talk to the axicli command line program
 *
 */
@Description("Axidraw")
class Axidraw(paperSize: PaperSize, orientation: PaperOrientation = PaperOrientation.PORTRAIT) {
    val actualPaperSize = when (orientation) {
        PaperOrientation.LANDSCAPE -> paperSize.size.yx.vector2
        PaperOrientation.PORTRAIT -> paperSize.size.vector2
    }

    /**
     * API URL to call once plotting is complete. If the string contains
     * `[filename]` it will be replaced by the name of the file being plotted.
     * This URL should be URL encoded (for instance use %20 instead of a space).
     */
    var apiURL = ""

    @OptionParameter("model", 50)
    var model = AxidrawModel.AxiDrawV3_A3

    @OptionParameter("servo", 60)
    var servo = AxidrawServo.Standard

    @IntParameter("speed pen down", 1, 110, 100)
    var speedPenDown = 25

    @IntParameter("speed pen up", 1, 110, 110)
    var speedPenUp = 70

    @IntParameter("acceleration", 1, 100, 120)
    var acceleration = 75

    /**
     * Toggle the pen up/down state by powering the pen plotter servo.
     * Useful for calibrating the pen height. Cover the paper with a
     * plastic sheet before running this command to avoid accidentally
     * leaving ink on the paper.
     */
    @ActionParameter("toggle up/down", 125)
    fun toggleUpDown() {
        runCMD(
            listOf(
                "--mode", "toggle",
                "--penlift", servo.id.toString(),
                "--model", model.id.toString(),
                "--pen_pos_down", "$penPosDown",
                "--pen_pos_up", "$penPosUp",
            ), false
        )
    }

    @IntParameter("pen pos down", 1, 100, 130)
    var penPosDown = 40

    @IntParameter("pen pos up", 1, 100, 140)
    var penPosUp = 60

    @IntParameter("pen rate lower", 1, 100, 150)
    var penRateLower = 50

    @IntParameter("pen rate raise", 1, 100, 160)
    var penRateRaise = 75

    @IntParameter("pen delay down", -500, 500, 170)
    var penDelayDown = 0

    @IntParameter("pen delay up", -500, 500, 180)
    var penDelayUp = 0

    @OptionParameter("optimization", 185)
    var optimization = AxidrawOptimizationTypes.ConnectPaths

    @BooleanParameter("random start", 190)
    var randomStart = false

    @BooleanParameter("fills occlude strokes", 200)
    var occlusion = false

    @IntParameter("margin", 0, 100, 205)
    var margin = 0

    @BooleanParameter("preview", 210)
    var preview = false

    @BooleanParameter("const speed", 220)
    var constSpeed = false

    @BooleanParameter("webhook", 230)
    var webhook = false

    private fun makeTempSVGFile(): File {
        val tmpFile = createTempFile("axi_${UUID.randomUUID()}", ".svg").toFile()
        tmpFile.deleteOnExit()
        return tmpFile
    }

    private var lastOutputFile = makeTempSVGFile()

    private val cmd = listOf(
        "xterm", "-hold", "-fullscreen",
        "-fs", "24",
        "-e", "axicli"
    )

    private fun plotArgs(plotFile: File, outputFile: File): List<String> {
        lastOutputFile = outputFile
        return listOf(
            plotFile.absolutePath,
            "--progress",
            "--report_time",
            "--reordering", optimization.id.toString(),
            if (randomStart) "--random_start" else "",
            if (occlusion) "--hiding" else "",
            if (preview) "--preview" else "",
            if (webhook && apiURL.isNotEmpty())
                "--webhook" else "",
            if (webhook && apiURL.isNotEmpty())
                "--webhook_url ${apiURL.replace("[filename]", plotFile.name)}" else "",
            "--speed_pendown", "$speedPenDown",
            "--speed_penup", "$speedPenUp",
            "--accel", "$acceleration",
            if (constSpeed) "--const_speed" else "",
            "--pen_pos_down", "$penPosDown",
            "--pen_pos_up", "$penPosUp",
            "--pen_rate_lower", "$penRateLower",
            "--pen_rate_raise", "$penRateRaise",
            "--pen_delay_down", "$penDelayDown",
            "--pen_delay_up", "$penDelayUp",
            "--penlift", servo.id.toString(),
            "--model", model.id.toString(),
            "--output_file", outputFile.absolutePath,
        ).filter { it.isNotEmpty() }
    }

    private fun compositionDimensions(): CompositionDimensions {
        return CompositionDimensions(
            0.0.pixels,
            0.0.pixels,
            Length.Pixels.fromMillimeters(actualPaperSize.x),
            Length.Pixels.fromMillimeters(actualPaperSize.y)
        )
    }

    /**
     * Main variable holding the design to save or plot.
     */
    private val design = drawComposition(compositionDimensions()) { }

    /**
     * Returns a suggested window width in pixels matching the [actualPaperSize]
     */
    fun windowWidth(ppi: Double = 96.0) = (ppi * actualPaperSize.x / 25.4).toInt()

    /**
     * Returns a suggested window height in pixels matching the [actualPaperSize]
     */
    fun windowHeight(ppi: Double = 96.0) = (ppi * actualPaperSize.y / 25.4).toInt()

    /**
     * Returns the bounds of the drawable area so user code can draw things
     * whithout leaving the paper.
     */
    val bounds = IntRectangle(
        0, 0,
        (96.0 * actualPaperSize.x / 25.4).toInt(),
        (96.0 * actualPaperSize.y / 25.4).toInt()
    ).rectangle

    /**
     * Clears the current design wiping any shapes the user might have
     * added.
     *
     */
    fun clear() = design.clear()

    /**
     * The core method that allows the user to append content to the design.
     * Use any methods and properties like contour(), segment(), fill, stroke, etc.
     */
    fun draw(f: CompositionDrawer.() -> Unit) {
        design.draw(f)
    }

    private fun runCMD(args: List<String>, hold: Boolean = true) {
        val actualCMD = (if (hold) cmd else listOf(cmd.last())) + args
        println((actualCMD).joinToString(" "))
        val pb = ProcessBuilder(actualCMD)
        pb.start()
    }

    /**
     * Display Axidraw software version
     */
    @ActionParameter("info: version", 300)
    fun version() = runCMD(listOf("--mode", "version"))

    /**
     * Display Axidraw system info
     */
    @ActionParameter("info: system", 310)
    fun sysInfo() = runCMD(listOf("--mode", "sysinfo"))

    /**
     * Save current design as SVG
     */
    @ActionParameter("save", 340)
    fun save() {
        saveFileDialog(supportedExtensions = listOf("SVG" to listOf("svg"))) { svgFile ->
            // If the user wants a frame covering the design...
            if (occlusion && margin > 0) {
                // Create a in-memory SVG representation of the frame
                val frameSVG = drawComposition {
                    fill = ColorRGBa.WHITE
                    stroke = null
                    shape(makeFrame(margin.toDouble()))
                }.toSVG()

                // Extract the actual shapes from the SVG
                val pattern = "<svg[^>]*>(.*?)</svg>".toRegex(RegexOption.DOT_MATCHES_ALL)
                val frameXML = pattern.find(frameSVG)?.groupValues?.getOrNull(1) ?: ""

                // Finally insert those shapes at the end of the file and save it
                design.saveToInkscapeFile(svgFile) {
                    it.replace("</svg>", "$frameXML</svg>")
                }
                // A simpler option would be to start a new composition, draw the design first
                // and the frame on top, but this adds a bunch of <g> tags wrapping everything,
                // which breaks the format axicli expects (layers directly on the root).
            } else {
                design.saveToInkscapeFile(svgFile)
            }
        }
    }

    /**
     * Plot [svgFile] using the current settings
     */
    @ActionParameter("plot", 350)
    fun plot() {
        openFileDialog(supportedExtensions = listOf("SVG" to listOf("svg"))) { svgFile ->
            runCMD(plotArgs(svgFile, makeTempSVGFile()))
        }
    }

    /**
     * After hitting pause, use this to move the pen home
     */
    @ActionParameter("resume to home", 320)
    fun goHome() {
        runCMD(plotArgs(lastOutputFile, makeTempSVGFile()) + listOf("--mode", "res_home"))
    }

    /**
     * After hitting pause, use this to continue plotting
     *
     */
    @ActionParameter("resume plotting", 330)
    fun resume() {
        runCMD(plotArgs(lastOutputFile, makeTempSVGFile()) + listOf("--mode", "res_plot"))
    }

    /**
     * Optimization. This can be applied to a lambda function that takes one argument
     * so it caches the calculation while the argument does not change.
     */
    private fun <A, B> ((A) -> B).lastArgMemo(): (A) -> B {
        var lastArg: A? = null
        var lastResult: B? = null

        return { arg ->
            if (arg == lastArg) {
                @Suppress("UNCHECKED_CAST")
                lastResult as B
            } else {
                val result = this(arg)
                lastArg = arg
                lastResult = result
                result
            }
        }
    }

    /**
     * Makes a white frame to cover the borders of the page, to avoid plotting
     * on the edge of papers, which may damage the pen or make a mess.
     */
    private val makeFrame = { width: Double ->
        Shape(
            listOf(
                bounds.contour.offset(1000.0, SegmentJoin.MITER),
                bounds.contour.offset(-width).reversed
            )
        )
    }.lastArgMemo()

    /**
     * Display the composition using [drawer].
     */
    fun display(drawer: Drawer) {
        val m = bounds.fit(drawer.bounds)
        drawer.isolated {
            drawer.view *= m
            composition(design)

            // Draw frame
            if (occlusion && margin > 0) {
                fill = ColorRGBa.WHITE
                stroke = null
                shape(makeFrame(margin.toDouble()))
            }
        }
    }
}
