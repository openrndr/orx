package difference

import org.openrndr.Clock
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0

class DoublePropertyDifferencer(
    private val clock: Clock,
    private val property: KProperty0<Double>,
) {
    private var lastValue: Double? = null
    private var output: Double? = null
    private var lastTime: Double? = null
    operator fun getValue(any: Any?, property: KProperty<*>): Double {
        if (lastTime != null) {
            val dt = clock.seconds - lastTime!!
            if (dt > 1E-10) {
                output = this.property.get() - lastValue!!
                lastValue = this.property.get()
            }
        } else {
            lastValue = this.property.get()
            output = lastValue!! - lastValue!!
        }
        lastTime = clock.seconds
        return output ?: error("no value")
    }
}

fun Clock.differencing(property: KProperty0<Double>) = DoublePropertyDifferencer(this, property)
