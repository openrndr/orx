package org.openrndr.panel

import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
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


    var mousePosition = Vector2.INFINITY

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

    lateinit var program: Program
    override var enabled: Boolean = true

    var contentScale = 1.0
    var renderTarget: RenderTarget? = null

    init {
        fontManager.register("default", resourceUrl("/fonts/Roboto-Regular.ttf"))
        fontManager.register("default", resourceUrl("/fonts/Roboto-Medium.ttf"), weight = 500)
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
        /** true while the focus moves by the keyboard, the focused element is then `focus-visible` */
        private var focusByKeyboard = false

        var target: Element? = null
            set(value) {
                if (value !== field) {
                    for (pseudoClass in listOf(active, focus, focusVisible)) field?.pseudoClasses?.remove(pseudoClass)
                    field?.keyboard?.focusLost?.trigger(FocusEvent())
                    value?.keyboard?.focusGained?.trigger(FocusEvent())
                    field = value
                    field?.pseudoClasses?.add(active)
                    field?.pseudoClasses?.add(focus)
                    if (focusByKeyboard) {
                        field?.pseudoClasses?.add(focusVisible)
                        // a control that the keyboard moves to is scrolled into view
                        field?.scrollIntoView(margin = 4.0)
                    }
                    value?.let {
                        lastTarget = it
                    }
                }
            }

        fun press(event: KeyEvent) {
            // escape closes the open context menu
            if (event.key == KEY_ESCAPE && openMenu?.parent != null) {
                openMenu?.dismiss()
                openMenu = null
                event.cancelPropagation()
                checkForManualRedraw()
                return
            }
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
                        focusByKeyboard = true
                        target = if (target != null) {
                            if (KeyModifier.SHIFT in event.modifiers) {
                                focusableControls[(index - 1).mod(focusableControls.size)]
                            } else {
                                focusableControls[(index + 1).mod(focusableControls.size)]
                            }
                        } else {
                            lastTarget ?: focusableControls[0]
                        }
                        focusByKeyboard = false
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

    /**
     * The distance in pixels the mouse has to move while a button is held before it counts as a drag. Smaller
     * movements are ignored, so that a press that moves slightly still results in a click.
     */
    var dragThreshold = 3.0

    inner class MouseInput {
        var dragTarget: Element? = null
        var clickTarget: Element? = null
        var lastClick = System.currentTimeMillis()

        /** the position of the last press, the start of a possible drag */
        private var pressPosition: Vector2? = null

        /** true once the mouse moved further than [dragThreshold] since the last press */
        private var dragging = false

        /** the scrollbar whose thumb is dragged, where the drag started and the scroll position then */
        internal var scrollbarDrag: Scrollbar? = null
        private var scrollbarDragStart = 0.0
        private var scrollbarDragPosition = 0.0

        /** true while the button that was pressed on a scrollbar is down */
        private var scrollbarPressed = false

        /** the scrollbar the mouse is over */
        internal var scrollbarHover: Scrollbar? = null

        /** sets the scrollbar the mouse is over to the one at [position], the elements of the bars are drawn again */
        private fun hoverScrollbar(position: Vector2) {
            val bar = scrollbarDrag ?: body?.let { scrollbarAt(it, position) }
            if (!bar.sameAs(scrollbarHover)) {
                scrollbarHover?.element?.draw?.dirty = true
                bar?.element?.draw?.dirty = true
                scrollbarHover = bar
            }
        }

        fun scroll(event: MouseEvent) {
            tooltips.hide()
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
            // a press on a scrollbar is not a click
            if (scrollbarPressed) {
                scrollbarPressed = false
                scrollbarDrag?.element?.draw?.dirty = true
                scrollbarDrag = null
                hoverScrollbar(event.position)
                dragTarget = null
                clickTarget = null
                checkForManualRedraw()
                return
            }
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
            pressPosition = event.position
            dragging = false
            tooltips.hide()

            // a press outside the open context menu closes it
            openMenu?.let { menu ->
                if (menu.parent == null) openMenu = null
                else if (event.position !in menu.screenArea) {
                    menu.dismiss()
                    openMenu = null
                }
            }
            // the right button opens the context menu of the innermost element that has one
            if (event.button == MouseButton.RIGHT) {
                body?.let { root -> elementAt(root, event.position) { it.contextMenu != null } }?.let { element ->
                    val menu = ContextMenu(event.position, element.contextMenu!!)
                    body?.append(menu)
                    openMenu = menu
                    event.cancelPropagation()
                    dragTarget = null
                    clickTarget = null
                    checkForManualRedraw()
                    return
                }
            }

            // scrollbars are on top of the content: the thumb is dragged, the track scrolls by a page
            body?.let { scrollbarAt(it, event.position) }?.let { bar ->
                if (event.position in bar.thumb) {
                    scrollbarDrag = bar
                    scrollbarDragStart = bar.along(event.position)
                    scrollbarDragPosition = bar.position
                } else {
                    val page = bar.page * 0.9
                    bar.scrollBy(if (bar.beforeThumb(event.position)) -page else page)
                }
                scrollbarPressed = true
                event.cancelPropagation()
                dragTarget = null
                clickTarget = null
                bar.element.draw.dirty = true
                checkForManualRedraw()
                return
            }
            val candidates = mutableListOf<Pair<Element, Int>>()
            /**
             * @param clippedBelow outside an element that scrolls, only its descendants on a layer above it, like
             * popups, can be pressed: the layer of that element
             */
            fun traverse(element: Element, depth: Int = 0, clippedBelow: Int? = null) {
                if (element.computedStyle.display == Display.NONE) {
                    return
                }
                var clipped = clippedBelow
                if (element.computedStyle.clipsContent && event.position !in element.screenArea) {
                    clipped = maxOf(clipped ?: Int.MIN_VALUE, element.layout.zIndex)
                }

                element.children.forEach { traverse(it, depth + 1, clipped) }

                if (!event.propagationCancelled && event.position in element.screenArea &&
                    (clipped == null || element.layout.zIndex > clipped)
                ) {
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
            scrollbarDrag?.let { dragged ->
                // the geometry of the bar as it is now, the thumb follows the pointer
                val bar = dragged.element.scrollbars().firstOrNull { it.vertical == dragged.vertical } ?: dragged
                bar.position = scrollbarDragPosition + (bar.along(event.position) - scrollbarDragStart) * bar.scrollPerPixel
                event.cancelPropagation()
                checkForManualRedraw()
                return
            }
            if (scrollbarPressed) return
            if (!dragging) {
                val start = pressPosition
                if (start != null && event.position.distanceTo(start) < dragThreshold) {
                    return
                }
                dragging = true
            }
            dragTarget?.let {
                if (it.isHidden()) {
                    dragTarget = null
                    return
                }
                event.cancelPropagation()
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
            mousePosition = event.position
            hoverScrollbar(event.position)
            tooltips.moved(body?.let { root -> elementAt(root, event.position) { it.tooltip != null } }, event.position)
            // a tooltip that shows follows the pointer
            if (tooltips.showing || tooltipArea != null) tooltipPending = true
            cursorPending = true
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
            val redraw = stylesPending || document?.layoutDirty == true || tooltipPending || body?.any {
                it.draw.dirty
            } ?: false
            if (redraw) {
                window.requestDraw()
            }
        }
    }

    val mouseInput = MouseInput()

    /** the fading of the scrollbars */
    internal val scrollbarFades = ScrollbarFades()

    /** the smooth scrolling of elements */
    internal val smoothScrolling = SmoothScrolling()

    /** the transitions of the styles of elements */
    internal val styleTransitions = StyleTransitions()

    /** the text of the elements, drawn from the outlines of glyphs, with its caches */
    val text: PanelText by lazy { PanelText(fontManager) }

    /** the tooltips of the elements under the pointer */
    internal val tooltips = Tooltips()

    /** the area of the tooltip when it was drawn last, null when none showed */
    private var tooltipArea: Rectangle? = null

    /** true when the tooltip can have moved, shown or hidden since the last frame */
    private var tooltipPending = false

    /** true when the styles of elements have to be computed again, see [Element.invalidateStyle] */
    var stylesPending = true

    /**
     * Draws only the areas of the elements that changed, when the layout did not change; false draws all elements in
     * every redraw.
     */
    var partialRedraw = true

    /** true when everything is drawn in the next redraw */
    private var fullRedrawPending = true

    /** the size the body was given */
    private var bodySize: Pair<Int, Int>? = null

    /** true when the cursor is set again in the next frame */
    private var cursorPending = true

    /** the number of redraws of all elements and of the areas of some, for tests and measurements */
    internal var fullRedraws = 0
        private set
    internal var partialRedraws = 0
        private set

    /** the area that is drawn again, null when everything is */
    private var damage: Rectangle? = null

    /** the context menu that is open */
    private var openMenu: ContextMenu? = null

    /** the rounded boxes that clip what is drawn */
    private val roundedClips = RoundedClips()

    /** the render targets that elements that are not opaque are drawn into */
    private val opacityTargets = HashMap<Element, RenderTarget>()

    /** the elements that are drawn into their render targets */
    private val drawingFaded = HashSet<Element>()

    /** the elements whose scrollbars were shown or fading in the last frame */
    private var fadingBefore = emptyList<Element>()
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

        fullRedrawPending = true
    }

    /**
     * True when [element] is drawn in this redraw: always when everything is drawn, else when what it draws is in
     * the area that is drawn again. Records the area it draws in.
     */
    private fun paints(element: Element): Boolean {
        val area = element.paintArea
        val damage = damage
        if (damage != null && intersection(area, damage) == null) return false
        element.drawnArea = area
        return true
    }

    private fun drawElement(element: Element, drawer: Drawer, zIndex: Int, zComp: Int) {
        // an element that is not opaque is drawn with its descendants into a render target, which is drawn faded
        val opacity = element.computedStyle.opacity.coerceIn(0.0, 1.0)
        if (opacity < 1.0 && element.computedStyle.display != Display.NONE && element !in drawingFaded) {
            if (opacity <= 0.0) return
            val rt = opacityTargets.getOrPut(element) {
                renderTarget(program.width, program.height, contentScale) {
                    colorBuffer()
                    depthBuffer()
                }
            }
            drawingFaded.add(element)
            drawer.isolatedWithTarget(rt) {
                drawer.clear(ColorRGBa.TRANSPARENT)
                drawer.ortho(rt)
                drawElement(element, drawer, zIndex, zComp)
            }
            drawingFaded.remove(element)
            val outerClip = drawer.drawStyle.clip
            drawer.isolated {
                drawer.defaults()
                // within the clipping of the elements around it
                drawer.drawStyle.clip = outerClip
                drawer.shadeStyle = roundedClips.style()
                drawer.ortho(RenderTarget.active)
                // the colors are premultiplied by their alpha
                drawer.drawStyle.colorMatrix = org.openrndr.extra.color.colormatrix.tint(ColorRGBa(opacity, opacity, opacity, opacity))
                drawer.image(rt.colorBuffer(0))
            }
            return
        }

        val newZComp =
            element.computedStyle.zIndex.let {
                when (it) {
                    is ZIndex.Value -> it.value
                    else -> zComp
                }
            }

        if (element.computedStyle.display != Display.NONE) {
            if (!element.computedStyle.clipsContent) {
                drawer.isolated {
                    drawer.drawStyle.textSetting = TextSettingMode.PIXEL
                    drawer.translate(element.screenPosition)
                    if (newZComp == zIndex && paints(element)) {
                        // controls can leave the drawer moved, the outline is drawn where the element is
                        drawer.isolated { element.draw(drawer) }
                        drawOutline(drawer, element)
                    }
                }
                element.childrenInDrawingOrder().forEach {
                    drawElement(it, drawer, zIndex, newZComp)
                }
            } else if (newZComp != zIndex) {
                // the layers above the element, like popups, are not clipped by it
                element.childrenInDrawingOrder().forEach {
                    drawElement(it, drawer, zIndex, newZComp)
                }
            } else {
                // the content is clipped to the area of the element, within the clipping of the elements around it,
                // by scissoring, which needs no render target
                val area = element.screenArea
                val outer = drawer.drawStyle.clip
                val clip = if (outer == null) area else intersection(outer, area)

                drawer.isolated {
                    drawer.translate(element.screenPosition)
                    if (newZComp == zIndex && paints(element)) {
                        // controls can leave the drawer moved, the outline is drawn where the element is
                        drawer.isolated { element.draw(drawer) }
                        drawOutline(drawer, element)
                    }
                }
                if (clip != null) {
                    // an element with rounded corners clips to its rounded padding box
                    val radius = element.cornerRadius
                    val border = element.layout.borderWidth
                    if (radius > 0.0) {
                        roundedClips.push(
                            Rectangle(area.x + border, area.y + border, area.width - 2.0 * border, area.height - 2.0 * border),
                            maxOf(0.0, radius - border)
                        )
                    }
                    drawer.isolated {
                        drawer.drawStyle.clip = snapToPixels(clip, contentScale)
                        for (child in element.childrenInDrawingOrder()) {
                            // the children of deeper elements change the clipping, it is set for every child
                            drawer.shadeStyle = roundedClips.style()
                            drawElement(child, drawer, zIndex, newZComp)
                        }
                    }
                    if (radius > 0.0) roundedClips.pop()
                }
                if (newZComp == zIndex && paints(element)) {
                    val active = mouseInput.scrollbarDrag?.element === element || mouseInput.scrollbarHover?.element === element
                    drawScrollbars(drawer, element, mouseInput.scrollbarDrag, mouseInput.scrollbarHover,
                        scrollbarFades.opacity(element, active))
                }
            }
        }
        element.draw.dirty = false
    }

    var drawCount = 0
    override fun afterDraw(drawer: Drawer, program: Program) {
        if (program.width > 0 && program.height > 0) {
            if (program.width != renderTarget?.width || program.height != renderTarget?.height) {
                fullRedrawPending = true

                renderTarget?.close()
                renderTarget = null
                opacityTargets.forEach { (_, u) -> u.close() }
                opacityTargets.clear()
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
            val body = body ?: return

            // the body covers the window
            val size = program.width to program.height
            if (size != bodySize) {
                bodySize = size
                body.style {
                    width = program.width.px
                    height = program.height.px
                }
            }

            // the styles of elements whose styles transition are computed again, they are drawn again
            for (element in styleTransitions.animating) {
                element.styleDirty = true
                element.draw.dirty = true
                stylesPending = true
            }

            // elements that scroll smoothly move on
            val smooth = smoothScrolling.scrolling
            smoothScrolling.advance()

            // scrollbars that are shown or fading are drawn again, and once more when they have faded out
            val fading = scrollbarFades.animating()
            fading.forEach { it.draw.dirty = true }
            fadingBefore.forEach { it.draw.dirty = true }
            fadingBefore = fading

            // the styles that can have changed are computed, the layout when it can have changed; in every frame,
            // inline styles change without notice, finding nothing changed is a walk over the elements
            stylesPending = false
            // an element that changed its size hints, like the label of a button, is laid out again
            body.visit {
                if (draw.dirty && laidOutHints != null && laidOutHints != (widthHint to heightHint)) {
                    document?.layoutDirty = true
                }
            }
            if (fullRedrawPending) document?.layoutDirty = true
            val laidOut = document?.layout() ?: false
            if (laidOut) {
                body.visit { laidOutHints = widthHint to heightHint }
            }
            styleTransitions.apply(body)

            // the tooltip that shows, hides or moves
            val screen = Rectangle(0.0, 0.0, program.width.toDouble(), program.height.toDouble())
            tooltipPending = false
            val tooltip = tooltips.area(text, screen)
            val tooltipChanged = tooltip != tooltipArea

            // filter() does not descend into elements that do not match
            val dirty = ArrayList<Element>()
            body.visit { if (draw.dirty) dirty.add(this) }
            val redraw = fullRedrawPending || laidOut || tooltipChanged || dirty.isNotEmpty()

            if (redraw) {
                // the area to draw again: what the elements that changed draw now and drew before, and the tooltip
                damage = if (!partialRedraw || fullRedrawPending || laidOut || body.draw.dirty) null else {
                    val areas = dirty.flatMap { listOfNotNull(it.paintArea, it.drawnArea) } +
                            listOfNotNull(tooltip, tooltipArea).map { Rectangle(it.x - 2.0, it.y - 2.0, it.width + 4.0, it.height + 4.0) }
                    val union = areas.reduceOrNull { a, b ->
                        val x0 = minOf(a.x, b.x)
                        val y0 = minOf(a.y, b.y)
                        Rectangle(x0, y0, maxOf(a.x + a.width, b.x + b.width) - x0, maxOf(a.y + a.height, b.y + b.height) - y0)
                    }?.let { intersection(it, screen) }
                    // whole pixels, around the areas
                    union?.let {
                        val x0 = kotlin.math.floor(it.x)
                        val y0 = kotlin.math.floor(it.y)
                        Rectangle(x0, y0, kotlin.math.ceil(it.x + it.width) - x0, kotlin.math.ceil(it.y + it.height) - y0)
                    }?.takeIf { it.width * it.height < 0.5 * screen.width * screen.height }
                }
                val damage = damage
                if (damage == null) fullRedraws++ else partialRedraws++
                tooltipArea = tooltip

                drawer.ortho()
                drawer.view = Matrix44.IDENTITY
                drawer.defaults()

                drawer.isolatedWithTarget(renderTarget!!) {
                    drawer.ortho(renderTarget!!)
                    if (damage == null) {
                        drawer.clear(ColorRGBa.BLACK.opacify(0.0))
                    } else {
                        // clearing ignores the scissor, the area is cleared by drawing over it
                        drawer.drawStyle.clip = damage
                        drawer.isolated {
                            drawer.drawStyle.blendMode = BlendMode.REPLACE
                            drawer.fill = ColorRGBa.TRANSPARENT
                            drawer.stroke = null
                            drawer.rectangle(damage)
                        }
                    }
                    drawElement(body, drawer, 0, 0)
                    drawElement(body, drawer, 1, 0)
                    drawElement(body, drawer, 1000, 0)
                    // over everything
                    tooltips.draw(drawer, text, screen)
                    drawer.drawStyle.clip = null
                }
                this.damage = null

                // the draw commands of texts that were not drawn are released, after everything was drawn
                if (damage == null) text.releaseUnused()
                fullRedrawPending = false
                cursorPending = true
            }

            body.visit {
                draw.dirty = false
            }

            // the cursor of the element under the pointer, over a scrollbar and while dragging one that of the scrollbar
            if (cursorPending) {
                cursorPending = false
                val bar = mouseInput.scrollbarDrag ?: mouseInput.scrollbarHover
                program.application.cursorType = bar?.element?.computedStyle?.scrollbarCursor?.cursorType
                    ?: elementAt(body, mousePosition) { true }?.computedStyle?.cursor?.cursorType
                            ?: CursorType.ARROW_CURSOR
            }

            drawer.defaults()
            drawer.ortho(RenderTarget.active)
            program.drawer.image(renderTarget!!.colorBuffer(0), 0.0, 0.0)

            drawCount++

            // the next frame continues the fading also when frames are only drawn on request
            if ((fadingBefore.isNotEmpty() || smooth || styleTransitions.animating.isNotEmpty() || tooltips.waiting) &&
                window.presentationMode == PresentationMode.MANUAL
            ) {
                window.requestDraw()
            }
        }
    }
}

/** the cursor of the application for [this] */
private val Cursor.cursorType: CursorType
    get() = when (this) {
        Cursor.Default -> CursorType.ARROW_CURSOR
        Cursor.Pointer -> CursorType.HAND_CURSOR
        Cursor.Text -> CursorType.IBEAM_CURSOR
        Cursor.Crosshair -> CursorType.CROSSHAIR_CURSOR
        else -> CursorType.ARROW_CURSOR
    }

/**
 * The innermost element at [position] of [root] and its descendants that matches [predicate], skipping elements that
 * are not displayed and the parts of elements outside the elements that clip them; elements on higher layers first
 */
internal fun elementAt(root: Element, position: Vector2, predicate: (Element) -> Boolean): Element? {
    var found: Element? = null
    var foundZ = Int.MIN_VALUE
    fun visit(element: Element, clippedBelow: Int?) {
        val cs = element.computedStyle
        if (cs.display == Display.NONE) return
        var clipped = clippedBelow
        if (cs.clipsContent && position !in element.screenArea) clipped = maxOf(clipped ?: Int.MIN_VALUE, element.layout.zIndex)
        val visible = clipped == null || element.layout.zIndex > clipped
        if (visible && position in element.screenArea && predicate(element) && element.layout.zIndex >= foundZ) {
            found = element
            foundZ = element.layout.zIndex
        }
        element.children.forEach { visit(it, clipped) }
    }
    visit(root, null)
    return found
}

/** the children of [this] in the order they are drawn: sticky children over the others */
private fun Element.childrenInDrawingOrder(): List<Element> =
    children.sortedBy { if (it.computedStyle.position == Position.STICKY) 1 else 0 }

/** draws the outline of [element] around its box, with the drawer at the position of the element */
private fun drawOutline(drawer: Drawer, element: Element) {
    val cs = element.computedStyle
    val width = (cs.outlineWidth as? LinearDimension.PX)?.value ?: 0.0
    val color = (cs.outlineColor as? Color.RGBa)?.color ?: return
    if (width <= 0.0 || color.alpha <= 0.0) return
    val offset = (cs.outlineOffset as? LinearDimension.PX)?.value ?: 0.0
    val grow = offset + width / 2.0
    val box = element.layout.boundsAtOrigin.let { Rectangle(it.x - grow, it.y - grow, it.width + 2.0 * grow, it.height + 2.0 * grow) }
    drawer.isolated {
        drawer.fill = null
        drawer.stroke = color
        drawer.strokeWeight = width
        drawer.box(box, if (element.cornerRadius > 0.0) element.cornerRadius + grow else 0.0)
    }
}

/** the pseudo-classes of the element with the keyboard focus, `focus-visible` when the keyboard moved the focus to it */
private val active = ElementPseudoClass("active")
private val focus = ElementPseudoClass("focus")
private val focusVisible = ElementPseudoClass("focus-visible")

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
        controlManager.document = document
        val styleSheet = StyleSheet(selector).apply { init() }
        document.styleSheets.add(styleSheet)
        return styleSheet
    }

    fun styleSheets(styleSheets: List<StyleSheet>) {
        val document = controlManager.existingOrNewDocumentWithDefaultStyleSheets()
        controlManager.document = document
        document.styleSheets.addAll(styleSheets)
    }

    @OptIn(ExperimentalContracts::class)
    fun layout(init: Body.() -> Unit) {
        contract {
            callsInPlace(init, InvocationKind.EXACTLY_ONCE)
        }

        val document = controlManager.existingOrNewDocumentWithDefaultStyleSheets()
        controlManager.document = document
        document.body.init()
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
    cm.fontManager.register("default", resourceUrl("/fonts/Roboto-Medium.ttf"), weight = 500)
    val document = Document()
    document.styleSheets.addAll(defaultStyles)
    cm.document = document
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
