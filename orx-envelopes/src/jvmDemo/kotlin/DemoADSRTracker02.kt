import org.openrndr.application
import org.openrndr.draw.loadFont
import org.openrndr.extra.envelopes.ADSRTracker
import org.openrndr.extra.noise.shapes.uniform
import org.openrndr.shape.Rectangle

/**
 * Demonstrates using `ADSRTracker`. The core difference
 * with `DemoADSRTracker01` is how shapes are rendered.
 *
 * Both programs listen to key presses, but the first
 * program renders tracked shapes inside the `extend` block,
 * while this program attaches a unique rendering block
 * to each tracked shape.
 *
 * The `ADSRTracker` maintains a mutable list of trackers,
 * but they do not have a stable ID. The element with index 3
 * will have index 2 when elements with lower indices expire.
 * This is the reason why visualized elements jump left when
 * an older tracker runs through its complete cycle.
 *
 * Attaching a function to each `triggerOn` event allows
 * rendered shapes to have a stable position on the window.
 *
 * Notice how the program works with two different `triggerId`s:
 * one used when pressing the `t` key, and the other for the `r`
 * key.
 *
 * This is needed on the `triggerOff` calls: to tell the tracker
 * which type of element should wind down. If several items
 * with that same `triggerId` exist (when we repeatedly pressed
 * the same keyboard key), the most recent of them will receive
 * the event.
 */
fun main() = application {
    program {
        val tracker = ADSRTracker(this)
        tracker.attack = 1.0
        tracker.decay = 0.2
        tracker.sustain = 0.8
        tracker.release = 2.0

        keyboard.keyDown.listen {
            if (it.name == "t") {
                val center = drawer.bounds.offsetEdges(-30.0).uniform()
                tracker.triggerOn(0) { time, value, position ->
                    drawer.circle(center, value * 100.0)
                }
            }
            if (it.name == "r") {
                val center = drawer.bounds.offsetEdges(-30.0).uniform()
                tracker.triggerOn(1) { time, value, position ->
                    val r = Rectangle.fromCenter(center, width = value * 100.0, height = value * 100.0)
                    drawer.rectangle(r)
                }
            }
        }
        keyboard.keyUp.listen {
            if (it.name == "t")
                tracker.triggerOff(0)
            if (it.name == "r")
                tracker.triggerOff(1)
        }
        extend {
            tracker.values().forEach {
                it()
            }
            drawer.fontMap = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 16.0)
            drawer.text("press and hold 't' and/or 'r'", 20.0, height - 20.0)
        }
    }
}
