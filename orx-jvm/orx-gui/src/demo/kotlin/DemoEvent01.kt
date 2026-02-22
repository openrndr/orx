import org.openrndr.KEY_ENTER
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.BlendMode
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.parameters.*
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4

fun main() = application {
    configure {
        width = 720
        height = 450
    }
    program {
        val gui = GUI()
        gui.compartmentsCollapsedByDefault = false

        val settings = @Description("Settings") object {
            @DoubleParameter("radius", 0.0, 100.0)
            var radius = 50.0

            @IntParameter("count", 0, 10)
            var count = 3

            @Vector2Parameter("position2", 0.0, 1.0)
            var position2 = Vector2.ZERO

            @Vector3Parameter("position3", 0.0, 1.0)
            var position3 = Vector3.ZERO

            @Vector4Parameter("position4", 0.0, 1.0)
            var position4 = Vector4.ZERO

            @ColorParameter("color")
            var color = ColorRGBa.PINK

            @DoubleListParameter("radii", 5.0, 30.0)
            var radii = mutableListOf(5.0, 6.0, 8.0, 14.0, 20.0, 30.0)

            @TextParameter("text")
            var text = "hello"

            @OptionParameter("blend mode")
            var blend = BlendMode.BLEND

            @BooleanParameter("active")
            var active = false

            @XYParameter("xy", 0.0, 10.0, 0.0, 10.0)
            var xy = Vector2.ZERO

            @PathParameter("path", true, extensions = ["jpg"])
            var path = ""
        }
        gui.add(settings)
        gui.onChange { name, value ->
            println("gui change $name -> $value")
        }
        extend(gui)
        extend {}
        keyboard.keyDown.listen {
            if(it.key == KEY_ENTER) {
                val blendModes = BlendMode.entries.toTypedArray()
                settings.radius += 1.0
                settings.count = (settings.count + 1) % 10
                settings.color = ColorRGBa.fromVector(Vector4.uniform(0.0, 1.0))
                settings.position2 = Vector2.uniform(0.0, 1.0)
                settings.position3 = Vector3.uniform(0.0, 1.0)
                settings.position4 = Vector4.uniform(0.0, 1.0)
                settings.blend = blendModes[(settings.blend.ordinal + 1) % blendModes.size]
                settings.xy = Vector2.uniform(0.0, 1.0)
                settings.text = seconds.toString().take(5)
                settings.active = !settings.active
                settings.radii = settings.radii.map { Double.uniform(5.0, 30.0) }.toMutableList()
                settings.path = "/tmp/${seconds}.jpg" // NOEVENT
            }
        }
    }
}