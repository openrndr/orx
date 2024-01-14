import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.osc.OSC
import kotlin.reflect.KProperty

private val logger = KotlinLogging.logger {}

open class ChataigneOSC(
        val osc: OSC
) {
    inner class DoubleChannel(key: String) {
        private var currentDouble = 0.0

        init {
            osc.listen(key) { _, message ->
                currentDouble = (message[0] as Float).toDouble()
            }
        }

        operator fun getValue(thisRef: Any?, property: KProperty<*>): Double {
            return currentDouble
        }

    }

    inner class ColorChannel(key: String) {
        private var currentColor = ColorRGBa.BLACK

        init {
            osc.listen(key) { _, message ->
                val red = message[0] as Float
                val green = message[1] as Float
                val blue = message[2] as Float
                val alpha = message[3] as Float

                currentColor = ColorRGBa(red.toDouble(), green.toDouble(), blue.toDouble(), alpha.toDouble())
            }
        }

        operator fun getValue(thisRef: Any?, property: KProperty<*>): ColorRGBa {
            return currentColor
        }

    }

    fun update(seconds: Double) {
        osc.send("/setTime", seconds.toFloat())
    }

    init {
        logger.info {
            "setup Chataigne with OSC ${osc.address} in:${osc.portIn} out:${osc.portOut}"
        }
    }
}
