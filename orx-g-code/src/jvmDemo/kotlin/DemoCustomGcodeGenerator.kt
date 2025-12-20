import org.openrndr.application
import org.openrndr.extra.composition.drawComposition
import org.openrndr.extra.gcode.*
import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.Triangle


/**
 * This demo shows how to implement and use a custom [GeneratorContext] to generate G-code.
 * Here it is used to render a composition to a g-code file, but it can als be passed as argument to [Plot].
 *
 * A generator can be created for a context using:
 * ```kotlin
 * Generator { MyContext() }
 * ```
 *
 * [CustomGcodeGeneratorContext] shows how to implement a custom context.
 *
 * Running this demo outputs:
 * G1 Z10
 * G0 X10.0 Y10.0
 * G1 Z0
 * G1 X20.0 Y20.0
 * G1 Z10
 */
fun main() = application {
    configure {
        width = 800
        height = 800
    }

    program {

        val comp = drawComposition {
            lineSegment(10.0, 10.0, 20.0, 20.0)
        }

        // Instantiate a generator that creates CustomGcodeGeneratorContext
        val generator = Generator { CustomGcodeGeneratorContext() }

        // Convert composition to g-code, print to console and exit
        val commands = generator.file {
            render("composition", comp)
        }
        val gCode = commands.withoutDuplicates().toGcode()
        println(gCode)
        application.exit()
    }
}

/**
 * This is a minimal implementation of a custom generator context.
 *
 * **Only for demonstration purposes.**
 *
 * It draws on the XY plane, moving down and up on the Z axis to draw
 * using G0 and G1 commands, omitting the feed rate.
 * This could be adapted to generate g-code for a XYZ 3D-Printer with a pen plotter conversion.
 *
 * There are no setup and finishing commands.
 *
 * It has no parameters and overrides only required methods.
 *
 * These additional hooks are available:
 * `beginLayer`, `beginShape`, `endShape` and `endLayer`.
 *
 */
class CustomGcodeGeneratorContext()
    : BaseGeneratorContext(
    deduplicateCommands = false // Allow identical consecutive commands, set true to remove duplicates
) {
    override val distanceTolerance: Double = 1.0
    override val minSquaredDistance: Double = 1.0

    override fun beginFile() {
        // Linear move, Z-Axis to moving height
        add("G1 Z10")
    }

    override fun beginContour(start: Vector2, contour: ShapeContour) {
        // Rapid move to start of contour
        add("G0 X${start.x} Y${start.y}")

        // Linear move to working height
        add("G1 Z0")
    }

    override fun drawTo(p: Vector2) {
        // Linear move to next position
        add("G1 X${p.x} Y${p.y}")
    }

    override fun endContour(contour: ShapeContour) {
        // Linear move, Z-Axis to moving height
        add("G1 Z10")
    }

    override fun endFile() {
        // Noop
    }

}