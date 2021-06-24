import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.osc.OSC

suspend fun main() = application {

    configure {
        width = 500
        height = 500
    }

   /* Find the Chataigne example project in /resources */
    class SceneVariables : ChataigneOSC(OSC(portIn = 9005, portOut = 12001)) {
        val myRadius: Double by DoubleChannel("/myRadius")
        val myOpacity: Double by DoubleChannel("/myOpacity")
        val myColor: ColorRGBa by ColorChannel("/myColor")
    }

    program {
        val animation = SceneVariables()

        extend {
            animation.update(seconds)

            drawer.fill = animation.myColor.opacify(animation.myOpacity)
            drawer.circle(width/2.0, height/2.0, animation.myRadius * 250)
        }
    }
}