import org.openrndr.application
import org.openrndr.extra.keyframer.Keyframer
import org.openrndr.panel.controlManager
import org.openrndr.panel.elements.Range
import org.openrndr.panel.elements.Slider
import org.openrndr.panel.elements.slider
import org.openrndr.resourceUrl
import java.net.URL

/**
 * Sets up a simple UI with a slider. This slider visualizes the current time in seconds but
 * also allows controlling time by dragging the play head with the mouse.
 *
 * The result behaves like a typical video player where we can scrub the current time.
 */
fun main() = application {
    program {

        // -- replace the default clock with an offset clock
        var clockOffset = 0.0
        val oldClock = clock
        clock = { oldClock() - clockOffset }
        var clockSlider: Slider? = null

        // -- set up a simple UI
        val cm = controlManager {
            layout {
                clockSlider = slider {
                    range = Range(0.0, 30.0)
                    events.valueChanged.listen {
                        if (it.interactive) {
                            clockOffset = oldClock() - it.newValue
                        }
                    }
                }
            }
        }
        extend(cm)
        class Animation: Keyframer() {
            val position by Vector2Channel(arrayOf("x", "y"))
        }
        val animation = Animation()
        animation.loadFromJson(URL(resourceUrl("/demo-simple-01.json")))

        extend {
            // -- update the slider
            clockSlider?.value = seconds
            animation(seconds)
            drawer.circle(animation.position, 100.0)
        }
    }
}