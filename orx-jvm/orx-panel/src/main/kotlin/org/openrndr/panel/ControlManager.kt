package org.openrndr.panel

import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.internal.Driver
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.panel.document.Document
import org.openrndr.panel.elements.*
import org.openrndr.panel.layout.Layouter
import org.openrndr.panel.style.*
import org.openrndr.panel.style.Display
import org.openrndr.shape.Rectangle
import org.w3c.dom.Node
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

private val logger = KotlinLogging.logger {}

class ControlManager : Extension {
    var document: Document? = null
        set(value) {
            if (field !== value) {
                field = value
                body = value?.body
                layouter = value?.layouter
                value?.body?.controlManager = this
            }
        }

    var body: Element? = null
    var layouter: Layouter? = null

    val fontManager = FontManager()
    lateinit var window: Window
    private val renderTargetCache = HashMap<Element, RenderTarget>()

    lateinit var program: Program
    override var enabled: Boolean = true

    var contentScale = 1.0
    var renderTarget: RenderTarget? = null

    init {
        fontManager.register("default", resourceUrl("/fonts/Roboto-Regular.ttf"))
//        layouter.styleSheets.addAll(defaultStyles().flatMap { it.flatten() })
    }

    class DropInput {
        var target: Element? = null
        fun drop(event: DropEvent) {
            target?.drop?.dropped?.trigger(event)
        }
    }

    val dropInput = DropInput()


    inner class KeyboardInput {
        private var lastTarget: Element? = null
        var target: Element? = null
            set(value) {
                if (value !== field) {
                    field?.pseudoClasses?.remove(ElementPseudoClass("active"))
                    field?.keyboard?.focusLost?.trigger(FocusEvent())
                    value?.keyboard?.focusGained?.trigger(FocusEvent())
                    field = value
                    field?.pseudoClasses?.add(ElementPseudoClass("active"))
                    value?.let {
                        lastTarget = it
                    }
                }
            }

        fun press(event: KeyEvent) {
            target?.let {
                if (it.isHidden()) {
                    return
                }
                var current: Element? = it
                while (current != null) {
                    if (!event.propagationCancelled) {
                        current.keyboard.pressed.trigger(event)
                    }
                    current = current.parent
                }
                checkForManualRedraw()
            }

            if (!event.propagationCancelled) {
                if (event.key == KEY_TAB) {
                    val focusableControls = body?.findAllVisible { it.handlesKeyboardFocus } ?: emptyList()

                    val index = target?.let { focusableControls.indexOf(it) }
                        ?: lastTarget?.let { focusableControls.indexOf(it) } ?: -1
                    if (focusableControls.isNotEmpty()) {

                        target = if (target != null) {
                            if (KeyModifier.SHIFT in event.modifiers) {
                                focusableControls[(index - 1).mod(focusableControls.size)]
                            } else {
                                focusableControls[(index + 1).mod(focusableControls.size)]
                            }
                        } else {
                            lastTarget ?: focusableControls[0]
                        }
                    }
                }
            }
        }

        fun release(event: KeyEvent) {
            if (target?.isHidden() == true) {
                return
            }

            target?.keyboard?.released?.trigger(event)
            if (target != null) {
                checkForManualRedraw()
            }
        }

        fun repeat(event: KeyEvent) {
            if (target?.isHidden() == true) {
                return
            }

            target?.keyboard?.repeated?.trigger(event)
            if (target != null) {
                checkForManualRedraw()
            }
        }

        fun character(event: CharacterEvent) {
            if (target?.isHidden() == true) {
                return
            }

            target?.keyboard?.character?.trigger(event)
            if (target != null) {
                checkForManualRedraw()
            }
        }

        fun requestFocus(element: Element) {
            target = element
        }
    }

    val keyboardInput = KeyboardInput()

    inner class MouseInput {
        var dragTarget: Element? = null
        var clickTarget: Element? = null
        var lastClick = System.currentTimeMillis()

        fun scroll(event: MouseEvent) {
            fun traverse(element: Element) {
                if (element.computedStyle.display == Display.NONE) {
                    return
                }
                for (child in element.children) {
                    traverse(child)
                }
                if (!event.propagationCancelled) {
                    if (event.position in element.screenArea && element.computedStyle.display != Display.NONE) {
                        element.mouse.scrolled.trigger(event)
                        if (event.propagationCancelled) {
                            keyboardInput.target = element
                        }
                    }
                }
            }
            body?.let(::traverse)
            checkForManualRedraw()
        }

        fun click(event: MouseEvent) {
            logger.debug { "click event: $event" }
            dragTarget = null
            val ct = System.currentTimeMillis()
            logger.debug { "click target: $clickTarget" }

            clickTarget?.let {
                if (it.isHidden()) {
                    return
                }
                if (it.handlesDoubleClick) {
                    if (ct - lastClick > 500) {
                        logger.debug { "normal click on $clickTarget" }
                        it.mouse.clicked.trigger(event)
                    } else {
                        if (clickTarget != null) {
                            logger.debug { "double-click on $clickTarget" }
                            it.mouse.doubleClicked.trigger(event)
                        }
                    }
                    lastClick = ct
                } else {
                    logger.debug { "normal click on $clickTarget" }
                    it.mouse.clicked.trigger(event)
                }
            }
            checkForManualRedraw()
        }

        fun press(event: MouseEvent) {
            logger.debug { "press event: $event" }
            val candidates = mutableListOf<Pair<Element, Int>>()
            fun traverse(element: Element, depth: Int = 0) {
                if (element.computedStyle.display == Display.NONE) {
                    return
                }
                if (element.computedStyle.overflow == Overflow.Scroll) {
                    if (event.position !in element.screenArea) {
                        return
                    }
                }

                if (element.computedStyle.display != Display.NONE) {
                    element.children.forEach { traverse(it, depth + 1) }
                }

                if (!event.propagationCancelled && event.position in element.screenArea && element.computedStyle.display != Display.NONE) {
                    candidates.add(Pair(element, depth))
                }
            }

            body?.let { traverse(it) }
            //candidates.sortByDescending { it.second }
            clickTarget = null
            candidates.sortWith(compareBy({ -it.first.layout.zIndex }, { -it.second }))
            for (c in candidates) {
                if (!event.propagationCancelled) {
                    c.first.mouse.pressed.trigger(event)
                    if (event.propagationCancelled) {
                        logger.debug { "propagation cancelled by ${c.first}" }
                        dragTarget = c.first
                        clickTarget = c.first
                        keyboardInput.target = c.first
                    }
                }
            }

            if (clickTarget == null) {
                dragTarget = null
                keyboardInput.target = null
            }

            checkForManualRedraw()
        }

        fun drag(event: MouseEvent) {
            logger.debug { "drag event $event" }
            dragTarget?.let {
                if (it.isHidden()) {
                    dragTarget = null
                    return
                }
                it.mouse.dragged.trigger(event)
            }

            if (event.propagationCancelled) {
                logger.debug { "propagation cancelled by $dragTarget setting clickTarget to null" }
                clickTarget = null
            }
            checkForManualRedraw()
        }

        val insideElements = mutableSetOf<Element>()
        fun move(event: MouseEvent) {
            val hover = ElementPseudoClass("hover")
            val toRemove = insideElements.filter { (event.position !in it.screenArea) }

            toRemove.forEach {
                it.mouse.exited.trigger(
                    MouseEvent(
                        event.position,
                        Vector2.ZERO,
                        Vector2.ZERO,
                        MouseEventType.MOVED,
                        MouseButton.NONE,
                        event.modifiers
                    )
                )
            }

            insideElements.removeAll(toRemove)

            fun traverse(element: Element) {
                if (event.position in element.screenArea) {
                    if (element !in insideElements) {
                        element.mouse.entered.trigger(event)
                    }
                    insideElements.add(element)
                    if (hover !in element.pseudoClasses) {
                        element.pseudoClasses.add(hover)
                    }
                    element.mouse.moved.trigger(event)
                } else {
                    if (hover in element.pseudoClasses) {
                        element.pseudoClasses.remove(hover)
                    }
                }
                element.children.forEach(::traverse)
            }
            body?.let(::traverse)
            checkForManualRedraw()
        }
    }

    fun checkForManualRedraw() {
        if (window.presentationMode == PresentationMode.MANUAL) {
            val redraw = body?.any {
                it.draw.dirty
            } ?: false
            if (redraw) {
                window.requestDraw()
            }
        }
    }

    val mouseInput = MouseInput()
    override fun setup(program: Program) {

        fontManager.program = program
        this.program = program

        contentScale = program.window.contentScale
        window = program.window

        fontManager.contentScale = contentScale
        program.mouse.buttonUp.listen { mouseInput.click(it) }
        program.mouse.moved.listen { mouseInput.move(it) }
        program.mouse.scrolled.listen { mouseInput.scroll(it) }
        program.mouse.dragged.listen { mouseInput.drag(it) }
        program.mouse.buttonDown.listen { mouseInput.press(it) }

        program.keyboard.keyDown.listen { keyboardInput.press(it) }
        program.keyboard.keyUp.listen { keyboardInput.release(it) }
        program.keyboard.keyRepeat.listen { keyboardInput.repeat(it) }
        program.keyboard.character.listen { keyboardInput.character(it) }

        program.window.drop.listen { dropInput.drop(it) }

        body?.draw?.dirty = true
    }

    private fun drawElement(element: Element, drawer: Drawer, zIndex: Int, zComp: Int) {
        val newZComp =
            element.computedStyle.zIndex.let {
                when (it) {
                    is ZIndex.Value -> it.value
                    else -> zComp
                }
            }

        if (element.computedStyle.display != Display.NONE) {
            if (element.computedStyle.overflow == Overflow.Visible) {
                drawer.isolated {
                    drawer.drawStyle.textSetting = TextSettingMode.PIXEL
                    drawer.translate(element.screenPosition)
                    if (newZComp == zIndex) {
                        element.draw(drawer)
                    }
                }
                element.children.forEach {
                    drawElement(it, drawer, zIndex, newZComp)
                }
            } else {
                val area = element.screenArea
                val rt = renderTargetCache.computeIfAbsent(element) {
                    renderTarget(program.width, program.height, contentScale) {
                        colorBuffer()
                        depthBuffer()
                    }
                }

                drawer.isolatedWithTarget(rt) {
                    drawer.clear(ColorRGBa.BLACK.opacify(0.0))
                    drawer.ortho(rt)
                    element.children.forEach {
                        drawElement(it, drawer, zIndex, newZComp)
                    }
                }

                drawer.isolated {
                    drawer.translate(element.screenPosition)
                    if (newZComp == zIndex) {
                        element.draw(drawer)
                    }
                }
                drawer.drawStyle.blendMode = BlendMode.OVER
                drawer.image(
                    rt.colorBuffer(0), Rectangle(Vector2(area.x, area.y), area.width, area.height),
                    Rectangle(Vector2(area.x, area.y), area.width, area.height)
                )
            }
        }
        element.draw.dirty = false
    }

    var drawCount = 0
    override fun afterDraw(drawer: Drawer, program: Program) {
        if (program.width > 0 && program.height > 0) {
            if (program.width != renderTarget?.width || program.height != renderTarget?.height) {
                body?.draw?.dirty = true

                renderTarget?.close()
                renderTarget = null
                renderTargetCache.forEach { (_, u) -> u.close() }
                renderTargetCache.clear()
            }

            if (renderTarget == null) {
                renderTarget = renderTarget(program.width, program.height, contentScale) {
                    colorBuffer()
                    depthBuffer()
                }
                program.drawer.isolatedWithTarget(renderTarget!!) {
                    program.drawer.clear(ColorRGBa.BLACK.opacify(0.0))
                }
            }

            val redraw = body?.any {
                it.draw.dirty
            } ?: false

            if (redraw) {
                drawer.ortho()
                drawer.view = Matrix44.IDENTITY
                drawer.defaults()

                if (body == null) {
                    return
                }

                drawer.isolatedWithTarget(renderTarget!!) {
                    drawer.ortho(renderTarget!!)
                    body?.style {
                        width = program.width.px
                        height = program.height.px
                    }

                    body?.let {
                        program.drawer.clear(ColorRGBa.BLACK.opacify(0.0))
                        document?.layout()
                        drawElement(it, program.drawer, 0, 0)
                        drawElement(it, program.drawer, 1, 0)
                        drawElement(it, program.drawer, 1000, 0)
                    }
                }
                Driver.instance.finish()
            }

            body?.visit {
                draw.dirty = false
            }

            drawer.defaults()
            drawer.ortho(RenderTarget.active)
            program.drawer.image(renderTarget!!.colorBuffer(0), 0.0, 0.0)

            drawCount++
        }
    }
}

private fun ControlManager.existingOrNewDocumentWithDefaultStyleSheets(): Document {
    return if (document == null) {
        val document = Document()
        document.styleSheets.addAll(defaultStyles())
        document
    } else {
        document!!
    }
}

class ControlManagerBuilder(val controlManager: ControlManager) {
    fun styleSheet(selector: CompoundSelector, init: StyleSheet.() -> Unit): StyleSheet {
        val document = controlManager.existingOrNewDocumentWithDefaultStyleSheets()
        val styleSheet = StyleSheet(selector).apply { init() }
        document.styleSheets.add(styleSheet)
        controlManager.document = document
        return styleSheet
    }

    fun styleSheets(styleSheets: List<StyleSheet>) {
        val document = controlManager.existingOrNewDocumentWithDefaultStyleSheets()
        controlManager.layouter?.styleSheets?.addAll(styleSheets)
        controlManager.document = document
    }

    @OptIn(ExperimentalContracts::class)
    fun layout(init: Body.() -> Unit) {
        contract {
            callsInPlace(init, InvocationKind.EXACTLY_ONCE)
        }

        val document = controlManager.existingOrNewDocumentWithDefaultStyleSheets()
        document.body.init()

        controlManager.document = document
    }
}


fun ControlManager.styleSheet(selector: CompoundSelector, init: StyleSheet.() -> Unit): StyleSheet {
    val document = existingOrNewDocumentWithDefaultStyleSheets()
    val styleSheet = StyleSheet(selector).apply { init() }
    document.styleSheets.add(styleSheet)
    this.document = document
    return styleSheet
}

fun ControlManager.styleSheets(styleSheets: List<StyleSheet>) {
    val document = existingOrNewDocumentWithDefaultStyleSheets()
    document.styleSheets.addAll(styleSheets)
    this.document = document
}

fun ControlManager.layout(init: Body.() -> Unit) {
    val document = existingOrNewDocumentWithDefaultStyleSheets()
    document.body.init()
    this.document = document
}

@OptIn(ExperimentalContracts::class)
fun Program.controlManager(
    defaultStyles: List<StyleSheet> = defaultStyles(),
    builder: ControlManagerBuilder.() -> Unit
): ControlManager {
    contract {
        callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
    }
    val cm = ControlManager()
    cm.program = this
    cm.fontManager.register("default", resourceUrl("/fonts/Roboto-Regular.ttf"))
    cm.layouter?.styleSheets?.addAll(defaultStyles.flatMap { it.flatten() })
    val cmb = ControlManagerBuilder(cm)
    cmb.builder()
    return cm
}

private fun Element.any(function: (Element) -> Boolean): Boolean {
    if (function(this)) {
        return true
    } else {
        children.forEach {
            if (it.any(function)) {
                return true
            }
        }
        return false
    }
}

private fun Element.anyVisible(function: (Element) -> Boolean): Boolean {
    if (computedStyle.display != Display.NONE && function(this)) {
        return true
    }

    if (computedStyle.display != Display.NONE) {
        children.forEach {
            if (it.anyVisible(function)) {
                return true
            }
        }
    }
    return false
}