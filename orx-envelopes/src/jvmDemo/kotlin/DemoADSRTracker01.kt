import org.openrndr.application
import org.openrndr.draw.loadFont
import org.openrndr.extra.envelopes.ADSRTracker

/**
 * Demonstrates how to use an `ADSRTracker`.
 *
 * The `ADSRTracker` keeps a collection of active ADSR envelopes.
 *
 * An ADSR envelope is commonly used in musical synthesizers to track the
 * volume of a sound produced in response to pressing, holding and releasing
 * a key in it keyboard.
 *
 * The ADSR envelope in OPENRNDR, when `triggerOn` is called, tracks the change of a
 * Double value increasing from 0.0 to 1.0 in `attack` seconds,
 * then decays to the `sustain` level in `decay` seconds, and finally when
 * `triggerOff` is called, decreases back to 0.0 in `release` seconds.
 *
 * The time in seconds is tracked by a `Clock` passed in the constructor.
 * This allows to use alternative clocks, for instance, frame-based.
 *
 * In this interactive program the `t` key can be pressed, held, and released to
 * go through the ADSR cycle. Try also pressing the `t` key repeatedly and observe
 * how multiple instances are tracked.
 *
 * The current ADSR instances are visualized as growing and shrinking circles at the
 * top of the window.
 *
 * In the center of the window one can see the added value of all current ADSR instances
 * represented as the radius of a white circle.
 *
 */
fun main() = application {
    program {
        val tracker = ADSRTracker(this)
        tracker.attack = 1.0
        tracker.decay = 0.2
        tracker.sustain = 0.8
        tracker.release = 2.0

        keyboard.keyDown.listen {
            if (it.name == "t")
                tracker.triggerOn()
        }
        keyboard.keyUp.listen {
            if (it.name == "t")
                tracker.triggerOff()
        }
        extend {
            tracker.values().forEach {
                drawer.circle(40.0, 40.0, 20.0 * it.value)
                drawer.translate(40.0, 0.0)
            }
            drawer.defaults()
            drawer.circle(drawer.bounds.center, 100.0 * tracker.value())

            drawer.fontMap = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 16.0)
            drawer.text("press and hold 't'", 20.0, height - 20.0)
        }
    }
}
