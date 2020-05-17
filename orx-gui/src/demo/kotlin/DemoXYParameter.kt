import org.openrndr.application
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.XYParameter
import org.openrndr.math.Vector2

fun main() = application {
    configure {
        width = 800
        height = 800
    }

    program {
        val gui = GUI()

        val settings = @Description("Settings") object {
            @XYParameter("Position", 0.0, 800.0, 0.0, 800.0,
                    precision = 2,
                    invertY = true,
                    showVector = true)
            var position: Vector2 = Vector2(0.0,0.0)
        }

        gui.add(settings)

        extend(gui)
        extend {
            drawer.circle(settings.position, 50.0)
        }
    }
}
