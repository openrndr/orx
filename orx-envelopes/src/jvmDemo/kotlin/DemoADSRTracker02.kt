import org.openrndr.application
import org.openrndr.draw.loadFont
import org.openrndr.extra.envelopes.ADSRTracker
import org.openrndr.extra.noise.shapes.uniform
import org.openrndr.shape.Rectangle

fun main() {
    application {
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
}