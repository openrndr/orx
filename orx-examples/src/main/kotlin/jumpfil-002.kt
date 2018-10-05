import org.openrndr.Program
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.configuration
import org.openrndr.draw.ColorType
import org.openrndr.draw.colorBuffer
import org.openrndr.extra.jumpfill.EncodePoints
import org.openrndr.extra.jumpfill.JumpFlood

class JumpFill001 : Program() {


    var drawFunc = {}
    override fun setup() {

        val encodePoints = EncodePoints()
        val jumpFill = JumpFlood()

        val input = colorBuffer(512, 1024)
        val coordinates =
                listOf(colorBuffer(input.width, input.height, type = ColorType.FLOAT32),
                        colorBuffer(input.width, input.height, type = ColorType.FLOAT32))

        for (i in 0 until 100) {
            input.shadow[(Math.random() * input.width).toInt(), (Math.random() * input.height).toInt()] =
                    ColorRGBa.WHITE
        }
        input.shadow.upload()

        drawFunc = {
            encodePoints.apply(input, coordinates[0])
            drawer.image(coordinates[0])
            jumpFill.maxSteps = 10
            for (i in 0 until 10) {
                jumpFill.step = i
                jumpFill.apply(coordinates[i % 2], coordinates[(i + 1) % 2])
            }

            drawer.image(coordinates[0])

        }
    }

    override fun draw() {
        drawFunc()
    }
}

fun main(args: Array<String>) {
    application(JumpFill001(), configuration {

        width = 1024
        height = 1024

    })
}