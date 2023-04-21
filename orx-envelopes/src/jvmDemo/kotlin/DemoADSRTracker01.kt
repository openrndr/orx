import org.openrndr.application
import org.openrndr.draw.loadFont
import org.openrndr.extra.envelopes.ADSRTracker

fun main() {
    application {
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
}