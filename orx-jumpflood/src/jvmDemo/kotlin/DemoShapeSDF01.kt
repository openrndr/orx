import org.openrndr.MouseTracker
import org.openrndr.application
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.colorBuffer
import org.openrndr.extra.jumpfill.ShapeSDF
import org.openrndr.extra.svg.loadSVG

fun main() =  application {
    configure {
        width = 720
        height = 405
    }
    program {
        val sdf = ShapeSDF()
        val df = colorBuffer(width, height, format = ColorFormat.RGBa, type = ColorType.FLOAT32)

            val shapes = loadSVG("orx-jumpflood/src/jvmDemo/resources/name.svg").findShapes().map { it.shape }
            sdf.setShapes(shapes)
            sdf.apply(emptyArray(), df)

        val mouseTracker = MouseTracker(mouse)

        extend {
            if(mouseTracker.pressedButtons.isEmpty())
                drawer.image(df)
            else
                drawer.shapes(shapes)
        }
    }
}
