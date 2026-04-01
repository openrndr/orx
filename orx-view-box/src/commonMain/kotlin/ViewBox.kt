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
    translatePointers: Boolean = true,
    translateGestures: Boolean = true,
    val colorType: ColorType? = null,
    val contentScale: Double? = null,
    val multisample: BufferMultisample? = null

) : Program by program {
    var viewBoxReconfigured: Boolean = false
        private set

    var shouldDraw: () -> Boolean = { true }

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

    val result: ColorBuffer
        get() {
            return if (resolved == null) {
                renderTarget?.colorBuffer(0) ?: error("no result available")
            } else {
                return resolved ?: error("no result available")
            }
        }

    inner class TranslatedMouseEvents : MouseEvents {
        override val buttonDown = Event<MouseEvent>()
        override val buttonUp = Event<MouseEvent>()
        override val dragged = Event<MouseEvent>()
        override val entered = Event<MouseEvent>()
        override val exited = Event<MouseEvent>()
        override val moved = Event<MouseEvent>()
        override var position: Vector2 = -clientArea.corner

        // Note: use MouseTracker() instead of pressedButtons
        //override val pressedButtons: MutableSet<MouseButton>
        //    get() = TODO("Not yet implemented")

        override val scrolled = Event<MouseEvent>()
    }

    inner class TranslatedPointerEvents : PointerEvents {
        override val cancelled = Event<PointerEvent>()
        override val moved = Event<PointerEvent>()
        override val pointerDown = Event<PointerEvent>()
        override val pointerUp = Event<PointerEvent>()

    }

    override val mouse: MouseEvents = if (translateMouse) {
        TranslatedMouseEvents()
    } else {
        program.mouse
    }

    override val keyboard: KeyEvents = if (translateKeyboard) object : KeyEvents {
        override val character: Event<CharacterEvent> = Event()
        override val keyDown: Event<KeyEvent> = Event()
        override val keyRepeat: Event<KeyEvent> = Event()
        override val keyUp: Event<KeyEvent> = Event()
    } else {
        program.keyboard
    }

    override val pointers: PointerEvents by lazy {
        if (translatePointers) {
            TranslatedPointerEvents()
        } else {
            program.pointers
        }
    }

    override val gestures by lazy {
        if (translateGestures) {
            object : GestureEvents {
                override val pinchStarted: Event<PinchEvent> = Event()
                override val pinchUpdated: Event<PinchEvent> = Event()
                override val pinchEnded: Event<PinchEvent> = Event()
            }
        } else {
            program.gestures
        }
    }
    var hasInputFocus = false

    init {

        val previousProgram = Program.active
        Program.active = this

        if (translateMouse) {
            program.mouse.moved.listen {
                val previousProgram = Program.active
                Program.active = this
                try {
                    mouse as TranslatedMouseEvents
                    mouse.position = it.position - clientArea.corner
                    if (it.position in clientArea && !it.propagationCancelled) {
                        hasInputFocus = true
                        mouse.moved.trigger(it.copy(position = it.position - clientArea.corner))
                        it.cancelPropagation()
                    } else if (it.position !in clientArea) {
                        hasInputFocus = false
                    }
                } finally {
                    Program.active = previousProgram
                }
            }

            program.mouse.buttonUp.listen {
                val previousProgram = Program.active
                Program.active = this
                try {
                    if (it.position in clientArea && !it.propagationCancelled) {
                        mouse.buttonUp.trigger(it.copy(position = it.position - clientArea.corner))
                        it.cancelPropagation()
                    }
                } finally {
                    Program.active = previousProgram
                }
            }
            program.mouse.dragged.listen {
                val previousProgram = Program.active
                Program.active = this
                try {
                    if (it.position in clientArea && !it.propagationCancelled) {
                        mouse.dragged.trigger(it.copy(position = it.position - clientArea.corner))
                        it.cancelPropagation()
                    }
                } finally {
                    Program.active = previousProgram
                }
            }
            program.mouse.buttonDown.listen {
                val previousProgram = Program.active
                Program.active = this
                try {
                    if (it.position in clientArea && !it.propagationCancelled) {
                        mouse.buttonDown.trigger(it.copy(position = it.position - clientArea.corner))
                        it.cancelPropagation()
                    }
                } finally {
                    Program.active = previousProgram
                }
            }

            program.mouse.scrolled.listen {
                val previousProgram = Program.active
                Program.active = this
                try {
                    if (it.position in clientArea && !it.propagationCancelled) {
                        mouse.scrolled.trigger(it.copy(position = it.position - clientArea.corner))
                        it.cancelPropagation()
                    }
                } finally {
                    Program.active = previousProgram
                }
            }
        }
        if (translateKeyboard) {
            program.keyboard.keyDown.listen {
                val previousProgram = Program.active
                Program.active = this
                try {
                    if (hasInputFocus && !it.propagationCancelled) {
                        keyboard.keyDown.trigger(it)
                        it.cancelPropagation()
                    }
                } finally {
                    Program.active = previousProgram
                }
            }

            program.keyboard.keyUp.listen {
                val previousProgram = Program.active
                Program.active = this
                try {
                    if (hasInputFocus && !it.propagationCancelled) {
                        keyboard.keyUp.trigger(it)
                        it.cancelPropagation()
                    }
                } finally {
                    Program.active = previousProgram
                }
            }

            program.keyboard.keyRepeat.listen {
                val previousProgram = Program.active
                Program.active = this
                try {
                    if (hasInputFocus && !it.propagationCancelled) {
                        keyboard.keyRepeat.trigger(it)
                        it.cancelPropagation()
                    }
                } finally {
                    Program.active = previousProgram
                }
            }

            program.keyboard.character.listen {
                val previousProgram = Program.active
                Program.active = this
                try {
                    if (hasInputFocus && !it.propagationCancelled) {
                        keyboard.character.trigger(it)
                        it.cancelPropagation()
                    }
                } finally {
                    Program.active = previousProgram
                }
            }
        }

        if (translatePointers) {
            program.pointers.pointerDown.listen {
                val previousProgram = Program.active
                Program.active = this
                try {
                    if (hasInputFocus && !it.propagationCancelled) {
                        pointers.pointerDown.trigger(it.copy(position = it.position - clientArea.corner))
                        it.cancelPropagation()
                    }
                } finally {
                    Program.active = previousProgram
                }
            }
            program.pointers.pointerUp.listen {
                val previousProgram = Program.active
                Program.active = this
                try {
                    pointers.pointerUp.trigger(it.copy(position = it.position - clientArea.corner))
                    it.cancelPropagation()
                } finally {
                    Program.active = previousProgram
                }
            }
            program.pointers.cancelled.listen {
                val previousProgram = Program.active
                Program.active = this
                try {
                    pointers.cancelled.trigger(it.copy(position = it.position - clientArea.corner))
                    it.cancelPropagation()
                } finally {
                    Program.active = previousProgram
                }
            }
            program.pointers.moved.listen {
                val previousProgram = Program.active
                Program.active = this
                try {
                    pointers.moved.trigger(it.copy(position = it.position - clientArea.corner))
                } finally {
                    Program.active = previousProgram
                }
            }
        }

        if (translateGestures) {
            program.gestures.pinchStarted.listen {
                val previousProgram = Program.active
                Program.active = this
                try {
                    if (hasInputFocus && !it.propagationCancelled) {
                        gestures.pinchStarted.trigger(it)
                        it.cancelPropagation()
                    }
                } finally {
                    Program.active = previousProgram
                }
            }
            program.gestures.pinchUpdated.listen {
                val previousProgram = Program.active
                Program.active = this
                try {
                    if (hasInputFocus && !it.propagationCancelled) {
                        gestures.pinchUpdated.trigger(it)
                        it.cancelPropagation()
                    }
                } finally {
                    Program.active = previousProgram
                }
            }

            program.gestures.pinchEnded.listen {
                val previousProgram = Program.active
                Program.active = this
                try {
                    if (!it.propagationCancelled) {
                        gestures.pinchEnded.trigger(it)
                        it.cancelPropagation()
                    }
                } finally {
                    Program.active = previousProgram
                }
            }
        }
        Program.active = previousProgram
    }

    override fun <T : Extension> extend(extension: T): T {
        extensions.add(extension)
        val previousProgram = Program.active
        Program.active = this
        try {
            extension.setup(this)
        } finally {
            Program.active = previousProgram
        }
        return extension
    }

    override fun <T : Extension> extend(extension: T, configure: T.() -> Unit): T {
        extensions.add(extension)
        val previousProgram = Program.active
        Program.active = this
        try {
            extension.configure()
            extension.setup(this)
        } finally {
            Program.active = previousProgram
        }
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

    fun configureRenderTarget(): RenderTarget {
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
            viewBoxReconfigured = true
            val art = RenderTarget.active
            renderTarget =
                renderTarget(widthCeil, heightCeil, contentScale ?: art.contentScale, multisample ?: art.multisample) {
                    colorBuffer(
                        type = colorType ?: if (art !is ProgramRenderTarget) {
                            art.colorBuffer(0).type
                        } else {
                            ColorType.UINT8
                        }
                    )
                    depthBuffer()
                }
            if ((multisample ?: art.multisample) != BufferMultisample.Disabled) {
                resolved = colorBuffer(
                    widthCeil,
                    heightCeil,
                    contentScale ?: art.contentScale
                )
            }
        }
        return renderTarget ?: error("could not create a render target")
    }

    override fun draw() {
        update()
        val previousProgram = Program.active

        try {
            program.drawer.isolated {
                if (resolved == null) {
                    program.drawer.image(renderTarget!!.colorBuffer(0), clientArea.corner)
                } else {
                    program.drawer.image(resolved!!, clientArea.corner)
                }
            }
        } finally {
            Program.active = previousProgram
        }
    }

    /**
     * Updates the view box by executing all the extension draw stages. [Update] will not visualize the results
     */
    fun update() {

        val previousProgram = Program.active
        Program.active = this

        try {
            configureRenderTarget()
            if (viewBoxReconfigured || shouldDraw()) {
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

                    viewBoxReconfigured = false
                }
                if (resolved != null) {
                    renderTarget!!.colorBuffer(0).copyTo(resolved!!)
                }
            }
        } finally {
            Program.active = previousProgram
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
fun Program.viewBox(
    area: Rectangle,
    translateMouse: Boolean = true,
    translateKeyboard: Boolean = true,
    translatePointers: Boolean = true,
    translateGestures: Boolean = true,
    colorType: ColorType? = null,
    contentScale: Double? = null,
    multisample: BufferMultisample? = null,
    f: ViewBox.() -> Unit = {}
): ViewBox {
    val viewBox = ViewBox(
        this,
        area,
        translateMouse,
        translateKeyboard,
        translatePointers,
        translateGestures,
        colorType,
        contentScale,
        multisample
    )
    val previousProgram = Program.active
    Program.active = viewBox
    try {
        val rt = viewBox.configureRenderTarget()
        drawer.isolatedWithTarget(rt) {
            viewBox.f()
        }
    } finally {
        Program.active = previousProgram
    }
    return viewBox
}
