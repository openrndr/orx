import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.RenderTarget
import org.openrndr.draw.isolated
import org.openrndr.draw.renderTarget

class NoClear : Extension {
    override var enabled: Boolean = true
    private var renderTarget: RenderTarget? = null


    override fun beforeDraw(drawer: Drawer, program: Program) {
        if (renderTarget == null || renderTarget?.width != program.width || renderTarget?.height != program.height) {
            renderTarget?.let {
                it.colorBuffer(0).destroy()
                it.detachColorBuffers()
                it.destroy()
            }
            renderTarget = renderTarget(program.width, program.height) {
                colorBuffer()
                depthBuffer()
            }

            renderTarget?.let {
                drawer.withTarget(it) {
                    background(program.backgroundColor ?: ColorRGBa.TRANSPARENT)
                }
            }
        }
        renderTarget?.bind()
    }

    override fun afterDraw(drawer: Drawer, program: Program) {
        renderTarget?.unbind()

        renderTarget?.let {
            drawer.isolated {
                drawer.ortho()
                drawer.image(it.colorBuffer(0))
            }
        }
    }
}