import mu.KotlinLogging
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.osc.OSC
import java.awt.Color
import kotlin.reflect.KProperty

private val logger = KotlinLogging.logger {}

open class ChataigneOSC(
        val osc: OSC
) {
    inner class DoubleChannel(key: String) {
        private var currentDouble = 0.0

        init {
            osc.listen(key) {
                currentDouble = (it[0] as Float).toDouble()
            }
        }

        operator fun getValue(thisRef: Any?, property: KProperty<*>): Double {
            return currentDouble
        }

    }

    inner class ColorChannel(key: String) {
        private var currentColor = ColorRGBa.BLACK

        init {
            osc.listen(key) {
                val red = it[0] as Float
                val green = it[1] as Float
                val blue = it[2] as Float
                val alpha = it[3] as Float

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
