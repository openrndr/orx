import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.events.Event
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.timer.repeat

/**
 * This demonstrates how to combine `repeat {}` with a postponed event to trigger drawing
 */

fun main() = application {
    program {
        val event = Event<Any?>()
        event.postpone = true
        event.listen {
            drawer.circle(width / 2.0, height / 2.0, 200.0)
        }
        repeat(2.0) {
            // -- we can not draw here, so we relay the repeat signal to the event
            event.trigger(null)
        }
        if (System.getProperty("takeScreenshot") == "true") {
            extend(SingleScreenshot()) {
                this.outputFile = System.getProperty("screenshotPath")
            }
        }
        extend {
            drawer.clear(ColorRGBa.PINK)
            // -- by explicitly calling deliver we know that the drawing code in the listener will be
            // -- executed exactly here
            event.deliver()
        }
    }
}
