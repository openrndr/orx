import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.gui.WindowedGUI
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.extra.parameters.ColorParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import kotlin.math.cos
import kotlin.system.exitProcess

/**
 * Live-coding with [oliveProgram] and [WindowedGUI]
 */
fun main() {
    // skip this demo on CI
    if (System.getProperty("takeScreenshot") == "true") {
        exitProcess(0)
    }
    application {
        configure {
            width = 720
            height = 720
        }
        oliveProgram() {
            val gui = WindowedGUI()

            val settings = @Description("Settings") object {
                @DoubleParameter("radius", 0.0, 80.0)
                var radius = 30.0

                @ColorParameter("color")
                var fill = ColorRGBa.RED

                @ColorParameter("background")
                var background = ColorRGBa.BLACK

                @DoubleParameter("speed", 0.1, 10.0)
                var speed = 1.0

                @IntParameter("count", 1, 400)
                var count = 100
            }
            gui.add(settings)

            extend(gui)
            extend {
                drawer.clear(settings.background)
                drawer.fill = settings.fill
                for (i in 0 until settings.count) {
                    drawer.circle(
                        width / 2.0 + cos(settings. speed * seconds + i) * 320.0,
                        i * 7.2,
                        (cos(i + seconds * 0.5) * 1.0 + 1.0) * settings.radius
                    )
                }
            }
        }
    }
}