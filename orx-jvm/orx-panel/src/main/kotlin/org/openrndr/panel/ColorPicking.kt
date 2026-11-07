package org.openrndr.panel

import org.openrndr.CursorType
import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.color.Linearity
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Drawer
import org.openrndr.draw.RenderTarget
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.isolated
import org.openrndr.draw.renderTarget
import org.openrndr.math.Vector2
import org.openrndr.shape.IntRectangle
import org.openrndr.shape.Rectangle

/**
 * Picks colors from the window, see [ControlManager.pickColor]: the first extension of the program, so that while it
 * picks, everything the program and the other extensions draw goes into a render target of its own, which it puts in
 * the window and reads the color under the pointer from.
 */
internal class ColorPicking(private val controlManager: ControlManager) : Extension {
    override var enabled = true

    /** called with the color that is picked, null when no color is being picked */
    var onPick: ((ColorRGBa) -> Unit)? = null

    /** the color under the pointer */
    var color: ColorRGBa = ColorRGBa.BLACK
        private set

    private var target: RenderTarget? = null
    private var pixel: ColorBuffer? = null
    private var bound = false

    override fun beforeDraw(drawer: Drawer, program: Program) {
        if (onPick == null) {
            target?.destroy(); target = null
            return
        }
        val scale = program.window.contentScale
        val t = target?.takeIf { it.width == program.width && it.height == program.height && it.contentScale == scale }
            ?: renderTarget(program.width, program.height, scale) { colorBuffer(); depthBuffer() }.also { target?.destroy(); target = it }
        t.bind()
        bound = true
        drawer.clear(program.backgroundColor ?: ColorRGBa.BLACK)
    }

    override fun afterDraw(drawer: Drawer, program: Program) {
        if (!bound) return
        bound = false
        val t = target ?: return
        t.unbind()
        val image = t.colorBuffer(0)
        drawer.isolated {
            drawer.defaults()
            drawer.ortho(RenderTarget.active)
            drawer.image(image)
        }
        // the color under the pointer
        val scale = program.window.contentScale
        val p = controlManager.mousePosition * scale
        val x = p.x.toInt().coerceIn(0, image.effectiveWidth - 1)
        val y = p.y.toInt().coerceIn(0, image.effectiveHeight - 1)
        val px = pixel ?: colorBuffer(1, 1).also { pixel = it }
        image.copyTo(px, 0, 0, IntRectangle(x, y, 1, 1), IntRectangle(0, 0, 1, 1))
        px.shadow.download()
        color = px.shadow[0, 0].let { ColorRGBa(it.r, it.g, it.b, 1.0, Linearity.SRGB) }
        // a loupe of the color next to the pointer
        drawer.isolated {
            drawer.defaults()
            drawer.ortho(RenderTarget.active)
            val m = controlManager.mousePosition
            drawer.stroke = ColorRGBa.WHITE
            drawer.strokeWeight = 2.0
            drawer.fill = color
            drawer.circle(m + Vector2(22.0, -22.0), 14.0)
            drawer.stroke = ColorRGBa.BLACK.opacify(0.5)
            drawer.fill = null
            drawer.strokeWeight = 1.0
            drawer.circle(m + Vector2(22.0, -22.0), 16.0)
        }
        program.window.requestDraw()
    }

    /** takes the color under the pointer */
    fun pick() {
        val callback = onPick ?: return
        onPick = null
        callback(color)
    }

    override fun shutdown(program: Program) {
        target?.destroy()
        pixel?.destroy()
    }
}
