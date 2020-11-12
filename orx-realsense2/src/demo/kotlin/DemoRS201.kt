import org.openrndr.application
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.colorBuffer
import org.openrndr.extra.realsense2.RS2Sensor

fun main() {
    application {
        program {
            val sensors = RS2Sensor.listSensors()
            val depthFrame = colorBuffer(640, 480, format = ColorFormat.R, type = ColorType.UINT16)
            for (sensor in sensors) {
                println(sensor)
            }
            val sensor = RS2Sensor.openFirstOrDummy()
            println(sensor)
            sensor.depthFrameReceived.listen {
                it.copyTo(depthFrame)
            }
            extend {
                sensor.waitForFrames()
                drawer.image(depthFrame)
            }
        }
    }
}