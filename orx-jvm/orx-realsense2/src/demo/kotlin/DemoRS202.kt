import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.colorBuffer
import org.openrndr.extra.color.colormatrix.tint
import org.openrndr.extra.realsense2.RS2Sensor

/**
 * show how to use multiple RealSense sensors
 *
 * Tested with two sensors, only uses depth stream now
 */
fun main() = application {
    configure {
        width = 1280
        height = 720
    }
    program {
        val sensorDescriptions = RS2Sensor.listSensors()

        val sensors = sensorDescriptions.map {
            it.open()
        }

        val depthFrames = sensors.map {
            colorBuffer(640, 480, format = ColorFormat.R, type = ColorType.UINT16)
        }
        sensors.forEachIndexed { index, it ->
            it.depthFrameReceived.listen {
                it.copyTo(depthFrames[index])
            }
        }
        extend {
            drawer.drawStyle.colorMatrix = tint(ColorRGBa.WHITE.shade(20.0))
            for ((index, sensor) in sensors.withIndex()) {
                sensor.waitForFrames()
                drawer.image(depthFrames[index])
                drawer.translate(640.0, 0.0)
            }
        }
    }
}
