package org.openrndr.extra.axidraw

import io.github.oshai.kotlinlogging.KotlinLogging
import offset.offset
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.dialogs.openFileDialog
import org.openrndr.dialogs.saveFileDialog
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.composition.*
import org.openrndr.extra.composition.Length.Pixels
import org.openrndr.extra.imageFit.FitMethod
import org.openrndr.extra.imageFit.fit
import org.openrndr.extra.imageFit.fitRectangle
import org.openrndr.extra.parameters.*
import org.openrndr.extra.svg.loadSVG
import org.openrndr.extra.svg.toSVG
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.SegmentJoin
import org.openrndr.shape.Shape
import org.openrndr.shape.bounds
import java.io.File
import java.util.*
import kotlin.io.path.createTempFile

private val logger = KotlinLogging.logger {}
private const val virtualEnvName = "axidraw-venv"

private const val mmPerInch = 25.4
private const val pointsPerInch = 96.0

// TODO: If plotting is paused and the user presses `plot`, show a confirmation dialog.
// One should click one of the two resume buttons first! Otherwise a mess is likely.
// TODO: Add feature to reprint specific contours
// TODO: Allow choosing pen order. Sortable GUI?
// TODO: simulate line thickness and ink overlap?

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

/**
 * Axidraw models.
 * See: https://axidraw.com/doc/cli_api/#model
 */
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

/**
 * Pen-lift mechanism.
 * See: https://axidraw.com/doc/cli_api/#penlift
 */
@Suppress("unused")
enum class AxidrawServo(val id: Int) {
    Standard(2),
    Brushless(3),
}

/**
 * Represents common paper sizes using the A-series standard.
 */
@Suppress("unused")
enum class PaperSize(val size: Vector2) {
    AMinus2(Vector2(1682.0, 2378.0)),
    AMinus1(Vector2(1189.0, 1682.0)),
    A0(Vector2(841.0, 1189.0)),
    A1(Vector2(594.0, 841.0)),
    A2(Vector2(420.0, 594.0)),
    A3(Vector2(297.0, 420.0)),
    A4(Vector2(210.0, 297.0)),
    A5(Vector2(148.0, 210.0)),
    A6(Vector2(105.0, 148.0)),
    A7(Vector2(74.0, 105.0)),
    A8(Vector2(52.0, 74.0)),
    A9(Vector2(37.0, 52.0)),
    A10(Vector2(26.0, 37.0))
}

/**
 * Data class containing the output text and error code resulting from executing command line programs.
 */
data class ExecutionResult(val errorCode: Int, val output: String)

/**
 * Class for working with Axidraw pen plotters. It communicates with the `axicli` command line program,
 * which is installed automatically using Python. Provides an extensive GUI to configure the pen plotter,
 * to save and load designs, preview them, layout designs using a Camera2D, plot, resume and more.
 *
 * @property program (often `this`)
 * @property paperSizeInMm A Vector2 specifying the paper size in mm.
 *                         For convenient a constant like `PaperSize.A5.size` can be used.
 * @property drawBounds A Rectangle specifying the window area where to draw the plot simulation.
 *                      The default is `drawer.bounds`.
 * @param fit A Vector 2 specifying where inside [drawBounds] to draw the plot simulation.
 *            The default is `Vector2.ZERO`, which means `centered`. Use values between -1.0 and 1.0
 *            for aligning to the left/top or to the right/bottom of [drawBounds].
 */
@Description("Axidraw")
class Axidraw(
    private val program: Program,
    val paperSizeInMm: Vector2,
    val drawBounds: Rectangle = program.drawer.bounds,
    fit: Vector2 = Vector2.ZERO
) {

    private val paper = Rectangle(Vector2.ZERO, paperSizeInMm.x, paperSizeInMm.y)
    private val paperStretchedInPx = fitRectangle(
        paper, drawBounds,
        fit.x, fit.y,
        FitMethod.Contain
    ).second
    private val scaleFactor = (paperSizeInMm.x * pointsPerInch / mmPerInch) / paperStretchedInPx.width

    /**
     * This method is called automatically when instantiating [org.openrndr.extra.axidraw.Axidraw] to set up
     * a Python virtual environment. Call it with `true` as an argument to manually reinstall the virtual environment.
     */
    fun setupVirtualEnv(reinstall: Boolean = false) {
        if (!File(virtualEnvName).exists() || reinstall) {
            logger.info { "setting up $virtualEnvName Python virtual environment" }
            invokePython(listOf("-m", "venv", virtualEnvName))
        }
    }

    /**
     * This method is called automatically when instantiating [org.openrndr.extra.axidraw.Axidraw] to set up
     * the `axicli` program. Call it with `true` as an argument to manually reinstall axicli.`
     */
    fun setupAxidrawCli(reinstall: Boolean = false) {
        val python = venvPython(File(virtualEnvName))
        val axicli = File(python).resolveSibling("axicli")
        if (!axicli.exists() || reinstall) {
            logger.info { "installing axidraw-cli in virtual environment $python" }
            invokePython(
                listOf("-m", "pip", "install", "https://cdn.evilmadscientist.com/dl/ad/public/AxiDraw_API.zip"),
                python
            )
        }
    }

    init {
        setupVirtualEnv()
        setupAxidrawCli()
    }

    /**
     * API URL to call once plotting is complete. If the string contains
     * `[filename]` it will be replaced by the name of the file being plotted.
     * This URL should be URL encoded (for instance, use %20 instead of a space).
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
            )
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
    var occlusion = true

    @IntParameter("margin", 0, 100, 205)
    var margin = 2

    //@BooleanParameter("auto rotate", 208)
    var autoRotate = false

    @BooleanParameter("preview", 210)
    var preview = false

    @BooleanParameter("const speed", 220)
    var constSpeed = false

    @BooleanParameter("webhook", 230)
    var webhook = false

    /**
     * Creates a temporary SVG file. Used by the AxiCLI "resume" methods. When plotting,
     * the temporary SVG file is updated to keep track of progress and allow resuming.
     */
    private fun makeTempSVGFile(): File {
        val tmpFile = createTempFile("axi_${UUID.randomUUID()}", ".svg").toFile()
        tmpFile.deleteOnExit()
        return tmpFile
    }

    /**
     * Keeps track of the most recent output file. Used to resume plotting after a pause.
     */
    private var lastOutputFile = makeTempSVGFile()

    /**
     * Constructs a list of String arguments for `axicli`, based on the current GUI settings.
     */
    private fun plotArgs(plotFile: File, outputFile: File): List<String> {
        lastOutputFile = outputFile
        return listOf(
            plotFile.absolutePath,
            "--progress",
            "--report_time",
            "--reordering", optimization.id.toString(),
            if (randomStart) "--random_start" else "",
            if (occlusion) "--hiding" else "",
            if (autoRotate) "" else "--no_rotate",
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

    /**
     * Creates a `CompositionDimensions` to be used by `drawComposition` based
     * on the `paperSizeInMm` argument received in the `Axidraw` constructor.
     * This will set the size in the SVG file.
     */
    private val compositionDimensions = CompositionDimensions(
        0.0.pixels,
        0.0.pixels,
        Pixels.fromMillimeters(paperSizeInMm.x),
        Pixels.fromMillimeters(paperSizeInMm.y)
    )

    /**
     * Main variable holding the design to save or plot.
     */
    private val design = drawComposition(compositionDimensions) { }


    /**
     * Returns the bounds of the drawable area so user code can draw things
     * without leaving the paper.
     */
    val bounds
        get() = paperStretchedInPx

    /**
     * Clears the current design wiping any shapes the user might have added.
     */
    fun clear() = design.clear()

    /**
     * The core method that allows the user to append content to the design.
     * Use any methods and properties available in `CompositionDrawer`,
     * like contour(), segment(), fill, stroke, etc.
     */
    fun draw(f: CompositionDrawer.() -> Unit) {
        design.draw(drawFunction = f)
    }

    /**
     * Private function used to run command line programs and return an [ExecutionResult].
     */
    private fun runCMD(args: List<String>): ExecutionResult {
        val python = venvPython(File(virtualEnvName))
        val result = invokePython(listOf("-m", "axicli") + args, python)
        plotPaused = result.output.contains("Plot paused programmatically.") ||
                result.output.contains("Plot paused by button press.")
        return result
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
     * Pans the camera to center items found in the current [design].
     */
    @ActionParameter("center on paper", 320)
    fun centerOnPaper() {
        val c = (camera.view * design.findShapes().map { it.bounds }.bounds.center.xy01).xy
        val c2 = paperStretchedInPx.center
        camera.pan(c2 - c)
    }

    /**
     * Show a file-selector dialog to load an SVG file, then loads the content of the selected SVG file.
     */
    @ActionParameter("load", 330)
    fun onLoad() = openFileDialog(supportedExtensions = listOf("SVG" to listOf("svg"))) {
        clear()
        camera.view = Matrix44.IDENTITY
        val loaded = loadSVG(it)
        draw {
            translate(paperStretchedInPx.corner)
            scale(1.0 / scaleFactor)
            loaded.findGroups().forEach { gn ->
                if (gn.findGroups().size == 1) {
                    val g = group {
                        gn.findShapes().forEach { shp ->
                            if (shp.attributes["type"] != "margin") {
                                stroke = shp.stroke
                                fill = shp.fill
                                shape(shp.shape)
                            }
                        }
                    }
                    g.attributes.putAll(gn.attributes)
                }
            }
        }
    }

    /**
     * Save current design as SVG
     */
    @ActionParameter("save", 340)
    fun onSave() = saveFileDialog(supportedExtensions = listOf("SVG" to listOf("svg"))) { save(it) }

    private fun save(svgFile: File) {
        // Create a new SVG with the frame and camera applied
        val designRendered = drawComposition(compositionDimensions) {
            val m = camera.view

            scale(scaleFactor)
            translate(-paperStretchedInPx.corner)

            design.findGroups().forEach { gn ->
                if (gn.findGroups().size == 1) {
                    val g = group {
                        gn.findShapes().forEach { shp ->
                            stroke = shp.stroke
                            fill = shp.fill
                            shape(shp.shape.transform(m))
                        }
                    }
                    g.attributes.putAll(gn.attributes)
                }
            }

            // If the user wants a frame covering the design...
            if (occlusion) {
                fill = ColorRGBa.WHITE
                stroke = null
                shape(makeFrame(margin.toDouble()))?.attributes?.put("type", "margin")
            }
        }
        designRendered.saveToInkscapeFile(svgFile)
    }

    /**
     * Becomes true when pressing the hardware pause button on the Axidraw, resets to false after a successful plot
     * or by calling [goHome] or [resume].
     */
    var plotPaused = false

    /**
     * Plot a design using the current settings
     */
    @ActionParameter("plot", 350)
    fun onPlot(): ExecutionResult {
        if (plotPaused) {
            val errorMsg = "The device is paused. Please resume plotting or resume to home"
            println(errorMsg)
            return ExecutionResult(1, errorMsg)
        }
        val svgFile = makeTempSVGFile()
        save(svgFile)
        return runCMD(plotArgs(svgFile, makeTempSVGFile()))
    }

    /**
     * After hitting pause, use this to move the pen home
     */
    @ActionParameter("resume to home", 360)
    fun goHome(): ExecutionResult {
        plotPaused = false
        return runCMD(plotArgs(lastOutputFile, makeTempSVGFile()) + listOf("--mode", "res_home"))
    }


    /**
     * After hitting pause, use this to continue plotting
     *
     */
    @ActionParameter("resume plotting", 370)
    fun resume(): ExecutionResult {
        plotPaused = false
        return runCMD(plotArgs(lastOutputFile, makeTempSVGFile()) + listOf("--mode", "res_plot"))
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
     * The frame is created by shifting `bounds.contour` inwards `width` pixels,
     * and outwards 1000 pixels.
     * Used when [occlusion] is true.
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
     * Display the composition using [drawer]. Call this method from inside your `extend {}` block.
     */
    fun display(drawer: Drawer) {
        drawer.isolated {
            view *= bounds.fit(drawer.bounds)

            isolated {
                view *= camera.view
                composition(design)
            }

            // Draw frame
            if (occlusion) {
                fill = ColorRGBa.WHITE
                stroke = null
                shape(makeFrame(margin.toDouble()))
            }
        }
        // Draw bounds TMP
        drawer.isolated {
            fill = null
            stroke = ColorRGBa.PINK
            rectangle(paperStretchedInPx)
        }
    }

    /**
     * An interactive camera you can use to position your design in the paper.
     * Allows panning, scaling and rotating.
     */
    val camera by lazy {
        Camera2D().also {
            it.setup(program)
        }
    }

    /**
     * Returns the length of one millimeter in the drawing assuming
     * the camera has not been zoomed in or out
     */
    val oneMm: Double
        get() = pointsPerInch / mmPerInch / scaleFactor


    /**
     * Rebuilds the design putting shapes under groups based on stroke colors and inserts a pause
     * after each group.
     *
     * Call this method after creating a draw composition that uses several stroke colors.
     * When plotting, change pens after each pause, then click "resume plotting".
     *
     * The method returns a list of colors in the order they will be plotted.
     * You can render these colors in your program so you know which pens to use.
     *
     * NOTE: this method changes line order. Therefore, avoid it if order is important,
     * for instance, with designs using fill colors to occlude.
     */
    fun groupStrokeColors(): List<ColorRGBa> {
        val colorGroups = design.findShapes().filter { it.stroke != null }.groupBy { it.stroke!! }
        val colors = mutableListOf<ColorRGBa>()
        design.clear()
        design.draw {
            var i = 0
            colorGroups.forEach { (color, nodes) ->
                val hexColor = "%06x".format(
                    ((color.r * 255).toInt() shl 16) + ((color.g * 255).toInt() shl 8) + ((color.b * 255).toInt())
                )
                group { cursor.children.addAll(nodes) }.configure(hexColor)

                // Add a pause if it's not the last layer
                if (++i < colorGroups.size) {
                    group { }.configure(layerMode = AxiLayerMode.PAUSE)
                }
                colors.add(color)
            }
        }
        return colors
    }

    /**
     * Read-only String variable to inspect the current design in SVG format for debugging purposes.
     */
    var svg: String = ""
        get() = design.toSVG()
        private set
}
