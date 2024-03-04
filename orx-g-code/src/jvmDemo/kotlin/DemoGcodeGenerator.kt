import org.openrndr.application
import org.openrndr.drawComposition
import org.openrndr.extra.gcode.asCommands
import org.openrndr.extra.gcode.basicGrblSetup
import org.openrndr.extra.gcode.extensions.toCommands
import org.openrndr.extra.gcode.toGcode
import org.openrndr.extra.gcode.withoutDuplicates

fun main() = application {
    configure {
        width = 800
        height = 800
    }

    program {

        val generator = basicGrblSetup(drawRate = 500.0).copy(
            preDraw = (40..80 step 5).flatMap { listOf("M3 S$it", "G4 P0.08") },
            postDraw = "M3 S40".asCommands(),
        )

        val comp = drawComposition {
            lineSegment(10.0, 10.0, 20.0, 20.0)
        }

        // Convert composition to g-code, print to console and exit
        val commands = generator.setup + comp.toCommands(generator, 0.05) + generator.end
        val gCode = commands.withoutDuplicates().toGcode()
        println(gCode)
        application.exit()

        /* Output:
        G21
        G90
        ;begin composition
        ;begin shape: 0
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
        ;end shape: 0
        ;end composition
        G0 X0 Y0
        G90
        */
    }
}