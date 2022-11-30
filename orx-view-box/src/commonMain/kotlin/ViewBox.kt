package org.openrndr.extra.viewbox


import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.events.Event
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.math.ceil

class ViewBox(
    override val program: Program, var clientArea: Rectangle,
    translateMouse: Boolean = true,
    translateKeyboard: Boolean = true,

    ) : Program by program {

    override var width: Int
        get() {
            return renderTarget?.width ?: clientArea.width.toInt()
        }
        set(value) {}

    override var height: Int
        get() = renderTarget?.height ?: clientArea.height.toInt()
        set(value) {}

    private var renderTarget: RenderTarget? = null
    private var resolved: ColorBuffer? = null

    override val extensions: MutableList<Extension> = mutableListOf<Extension>()

    inner class TranslatedMouseEvents: MouseEvents {
        override val buttonDown = Event<MouseEvent>()
        override val buttonUp = Event<MouseEvent>()
        override val dragged = Event<MouseEvent>()
        override val entered = Event<MouseEvent>()
        override val exited = Event<MouseEvent>()
        override val moved = Event<MouseEvent>()
        override var position: Vector2 = -clientArea.corner

        override val pressedButtons: MutableSet<MouseButton>
            get() = TODO("Not yet implemented")
        override val scrolled = Event<MouseEvent>()
    }

    override val mouse: MouseEvents = if (translateMouse)  {
        TranslatedMouseEvents()
    } else {
        program.mouse
    }

    override val keyboard: KeyEvents = if (translateKeyboard) object : KeyEvents {
        override val character: Event<CharacterEvent> = Event<CharacterEvent>()
        override val keyDown: Event<KeyEvent> = Event<KeyEvent>()
        override val keyRepeat: Event<KeyEvent> = Event<KeyEvent>()
        override val keyUp: Event<KeyEvent> = Event<KeyEvent>()
    } else {
        program.keyboard
    }

    override val pointers: Pointers by lazy { program.pointers }

    var hasInputFocus = false

    init {
        if (translateMouse) {
            program.mouse.moved.listen {
                mouse as TranslatedMouseEvents
                mouse.position = it.position - clientArea.corner
                if (it.position in clientArea && !it.propagationCancelled) {
                    hasInputFocus = true
                    mouse.moved.trigger(it.copy(position = it.position - clientArea.corner))
                    it.cancelPropagation()
                } else if (it.position !in clientArea) {
                    hasInputFocus = false
                }
            }

            program.mouse.buttonUp.listen {
                if (it.position in clientArea && !it.propagationCancelled) {
                    mouse.buttonUp.trigger(it.copy(position = it.position - clientArea.corner))
                    it.cancelPropagation()
                }
            }
            program.mouse.dragged.listen {
                if (it.position in clientArea && !it.propagationCancelled) {
                    mouse.dragged.trigger(it.copy(position = it.position - clientArea.corner))
                    it.cancelPropagation()
                }
            }
            program.mouse.buttonDown.listen {
                if (it.position in clientArea && !it.propagationCancelled) {
                    mouse.buttonDown.trigger(it.copy(position = it.position - clientArea.corner))
                    it.cancelPropagation()
                }
            }

            program.mouse.scrolled.listen {
                if (it.position in clientArea && !it.propagationCancelled) {
                    mouse.scrolled.trigger(it.copy(position = it.position - clientArea.corner))
                    it.cancelPropagation()
                }
            }
        }
        if (translateKeyboard) {
            program.keyboard.keyDown.listen {
                if (hasInputFocus && !it.propagationCancelled) {
                    keyboard.keyDown.trigger(it)
                    it.cancelPropagation()
                }
            }

            program.keyboard.keyUp.listen {
                if (hasInputFocus && !it.propagationCancelled) {
                    keyboard.keyUp.trigger(it)
                    it.cancelPropagation()
                }
            }

            program.keyboard.keyRepeat.listen {
                if (hasInputFocus && !it.propagationCancelled) {
                    keyboard.keyRepeat.trigger(it)
                    it.cancelPropagation()
                }
            }

            program.keyboard.character.listen {
                if (hasInputFocus && !it.propagationCancelled) {
                    keyboard.character.trigger(it)
                    it.cancelPropagation()
                }
            }
        }
    }

    override fun <T : Extension> extend(extension: T): T {
        extensions.add(extension)
        extension.setup(this)
        return extension
    }

    override fun <T : Extension> extend(extension: T, configure: T.() -> Unit): T {
        extensions.add(extension)
        extension.configure()
        extension.setup(this)
        return extension
    }

    override fun extend(stage: ExtensionStage, userDraw: Program.() -> Unit) {
        val functionExtension = when (stage) {
            ExtensionStage.SETUP ->
                object : Extension {
                    override var enabled: Boolean = true
                    override fun setup(program: Program) {
                        program.userDraw()
                    }
                }

            ExtensionStage.BEFORE_DRAW ->
                object : Extension {
                    override var enabled: Boolean = true
                    override fun beforeDraw(drawer: Drawer, program: Program) {
                        program.userDraw()
                    }
                }

            ExtensionStage.AFTER_DRAW ->
                object : Extension {
                    override var enabled: Boolean = true
                    override fun afterDraw(drawer: Drawer, program: Program) {
                        program.userDraw()
                    }
                }
        }
        extensions.add(functionExtension)
    }

    override fun draw() {

        val widthCeil = ceil(clientArea.width).toInt()
        val heightCeil = ceil(clientArea.height).toInt()

        val lrt = renderTarget
        if (lrt != null) {
            if (lrt.width != widthCeil || lrt.height != heightCeil) {
                lrt.colorBuffer(0).destroy()
                lrt.depthBuffer?.destroy()
                lrt.detachColorAttachments()
                lrt.detachDepthBuffer()
                lrt.destroy()
                renderTarget = null

                resolved?.destroy()
                resolved = null
            }

        }

        if (renderTarget == null) {
            val art = RenderTarget.active
            renderTarget = renderTarget(widthCeil, heightCeil, art.contentScale, art.multisample) {
                colorBuffer()
                depthBuffer()
            }
            if (art.multisample != BufferMultisample.Disabled) {
                resolved = colorBuffer(widthCeil, heightCeil, art.contentScale, multisample = art.multisample)
            }
        }

        program.drawer.isolatedWithTarget(renderTarget!!) {
            drawer.clear(ColorRGBa.BLACK)
            drawer.defaults()
            drawer.ortho(renderTarget!!)
            for (extension in extensions) {
                extension.beforeDraw(program.drawer, this@ViewBox)
            }
            for (extension in extensions.reversed()) {
                extension.afterDraw(program.drawer, this@ViewBox)
            }
            program.drawer.defaults()
        }
        program.drawer.isolated {
            program.drawer.image(renderTarget!!.colorBuffer(0), clientArea.corner)
        }
    }
}

/**
 * Create a [ViewBox]
 * @param area a [Rectangle] that indicates the position and size of the view box
 * @param translateMouse should the view box translate mouse events? default is true
 * @param translateKeyboard should the view box translate keyboard events? default is true
 * @param f [ViewBox] configuration function
 * @return a newly created [ViewBox]
 */
fun Program.viewBox(area: Rectangle,
                    translateMouse: Boolean = true,
                    translateKeyboard: Boolean = true,
                    f: ViewBox.() -> Unit): ViewBox {
    val viewBox = ViewBox(this, area, translateMouse, translateKeyboard)
    viewBox.f()
    return viewBox
}