import org.openrndr.Program
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.configuration
import org.openrndr.draw.colorBuffer
import org.openrndr.extra.jumpfill.contourPoints
import org.openrndr.extra.jumpfill.jumpFlood
import org.openrndr.extra.jumpfill.threshold

class JumpFill002 : Program() {

    var drawFunc = {}
    override fun setup() {

        val input = colorBuffer(512, 1024)

        for (i in 0 until 3) {
            input.shadow[(Math.random() * input.width).toInt(), (Math.random() * input.height).toInt()] =
                    ColorRGBa.WHITE
        }
        input.shadow.upload()

        val result = jumpFlood(drawer, input)

//        threshold.apply(result, result)
 //       contourPoints.apply(result, result)

        drawFunc = {
            drawer.image(result)
        }
    }

    override fun draw() {
        drawFunc()
    }
}

fun main(args: Array<String>) {
    application(JumpFill002(), configuration {

        width = 1024
        height = 1024

    })
}