import org.openrndr.color.ColorRGBa
import org.openrndr.extra.osc.OSC
import java.awt.Color
import kotlin.reflect.KProperty

open class ChataigneOSC(
        val osc: OSC
) {
    inner class DoubleChannel(key:String) {
        private var currentDouble = 0.0

        init {
            osc.listen(key) {
                currentDouble = (it[0] as Float).toDouble()
            }
        }

        operator fun getValue(thisRef: Any?, property: KProperty<*>):Double {
            return currentDouble
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Double) {
            print("$value")
        }
    }

    inner class ColorChannel(key:String) {
        private var currentColor = ColorRGBa.BLACK

        init {
            println(key)
            osc.listen(key) {
                val c = it[0] as Color
                currentColor = ColorRGBa(c.red/255.0, c.green/255.0, c.blue/255.0, c.alpha/255.0)
            }
        }

        operator fun getValue(thisRef: Any?, property: KProperty<*>):ColorRGBa {
            return currentColor
        }
        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Double) {
            print("$value")
        }
    }

    fun update(seconds: Double) {
        osc.send("/example", seconds.toFloat())
    }

    init {
        println("setup Chataigne with OSC ${osc.address} in:${osc.portIn} out:${osc.portOut}")

        println("send")


    }
}