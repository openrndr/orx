import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.axidraw.Axidraw
import org.openrndr.extra.gui.GUI
import org.openrndr.math.Vector2

/**
 * A hello-world program demonstrating orx-axidraw.
 *
 * The program creates a fixed design with a paper size 200 x 200 mm
 * and a centered circle with a radius of 95 mm.
 *
 * The page with the circle is displayed centered in the window by default.
 *
 * A Camera2D is enabled by default. You can click and drag your
 * left / right mouse buttons to pan / rotate and use the mouse
 * wheel to zoom in and out. Use the camera to place your design
 * in the paper. Click the mouse wheel to reset the camera.
 *
 * Toggle the GUI visibility by pressing F11.
 *
 * The GUI lets you save, load and plot the design.
 *
 */
fun main() = application {
    program {
        val axi = Axidraw(this, Vector2(200.0))
        val gui = GUI()
        gui.compartmentsCollapsedByDefault = false
        gui.add(axi)

        axi.draw {
            circle(axi.bounds.center, axi.oneMm * 95)
        }

        extend(gui)
        extend {
            drawer.clear(ColorRGBa.WHITE)
            axi.display(drawer)
        }
    }
}