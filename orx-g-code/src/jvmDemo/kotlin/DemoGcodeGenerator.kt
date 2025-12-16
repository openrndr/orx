import org.openrndr.application
import org.openrndr.extra.composition.drawComposition
import org.openrndr.extra.gcode.*

/**
 * Demonstrates how to use a [Generator], in this case the [BasicGrblGenerator] to generate G-code directly
 * from a composition.
 *
 * First, the generator is configured with a drawRate pre- and post-draw commands. These are executed before and after each contour is draw.
 *
 * The preDraw commands show how this parameter can be used to slowly enable the tool. In this case it would be lowering the pen
 * in 8 steps, waiting 0.08 seconds between each step.
 *
 * `M3 SX` sets the servo and `G4 PX` waits for X seconds.
 *
 * Then a composition is created. It consists of a single line segment.
 *
 * Finally, the composition is rendered to G-code, which is printed to the console.
 *
 * The resulting G-code will:
 * - Setting the machine to absolute coordinates and milimeters
 * - Move to the beginning of the line segment (X10 Y10)
 * - Slowly set down the pen
 * - Draw the line segment
 * - Lift the pen back up
 * - Move to the origin (X0 Y0)
 * - Set the machine to absolute coordinates again
 *
 * Using the generatr does not require the JVM only [Plot] class and can be used in any environment.
 */
fun main() = application {
    configure {
        width = 800
        height = 800
    }

    program {

        val generator = BasicGrblGenerator(
            drawRate = 500.0,
            preDraw = (0..80 step 10).flatMap { listOf("M3 S$it", "G4 P0.08") },
            postDraw = "M3 S0".asCommands(),
        )

        val comp = drawComposition {
            lineSegment(10.0, 10.0, 20.0, 20.0)
        }

        // Convert composition to g-code, print to console and exit
        val commands = generator.file {
            render("composition", comp)
        }
        val gCode = commands.withoutDuplicates().toGcode()
        println(gCode)
        application.exit()

        /* Output:
        G21
        G90
        ;begin layer: composition
        ;begin shape
        G0 X10.0 Y10.0
        M3 S40
        G4 P0.08
        M3 S45
        G4 P0.08
        M3 S50
        G4 P0.08
        M3 S55
        G4 P0.08
        M3 S60
        G4 P0.08
        M3 S65
        G4 P0.08
        M3 S70
        G4 P0.08
        M3 S75
        G4 P0.08
        M3 S80
        G4 P0.08
        G1 X20.0 Y20.0 F500.0
        M3 S40
        ;end shape
        ;end layer: composition
        G0 X0 Y0
        G90
        */
    }
}