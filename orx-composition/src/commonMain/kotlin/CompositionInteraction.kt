package org.openrndr.extra.composition

import org.openrndr.MouseEvent
import org.openrndr.MouseEvents
import org.openrndr.events.Event
import org.openrndr.extra.shadestyles.presets.PaintUnits
import org.openrndr.extra.shadestyles.presets.boundsTransform
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour

/**
 * A node that is hit at a point: [node], and [point], the point in the coordinates of the node
 */
class CompositionHit(val node: CompositionNode, val point: Vector2)

/** the target of the link that this node is in, like the href of the SVG a element it is in, null when it is in none */
val CompositionNode.effectiveLink: String?
    get() = generateSequence(this) { it.parent }.firstNotNullOfOrNull { (it as? GroupNode)?.link }

/** the winding number of the polyline [points] around [p] */
private fun winding(points: List<Vector2>, p: Vector2): Int {
    var winding = 0
    for (i in points.indices) {
        val a = points[i]
        val b = points[(i + 1) % points.size]
        if (a.y <= p.y) {
            if (b.y > p.y && (b.x - a.x) * (p.y - a.y) - (p.x - a.x) * (b.y - a.y) > 0) winding++
        } else {
            if (b.y <= p.y && (b.x - a.x) * (p.y - a.y) - (p.x - a.x) * (b.y - a.y) < 0) winding--
        }
    }
    return winding
}

/** true when [p] is inside [shape] by [rule], every contour taken as closed */
internal fun shapeContains(shape: Shape, p: Vector2, rule: FillRule = FillRule.NonZero): Boolean {
    if (shape.empty || p !in shape.bounds) return false
    val total = shape.contours.sumOf { winding(it.adaptivePositions(0.25), p) }
    return if (rule == FillRule.EvenOdd) total % 2 != 0 else total != 0
}

/** true when [p] is on the stroke of [shape] of [weight] */
private fun strokeContains(shape: Shape, p: Vector2, weight: Double): Boolean =
    shape.contours.any { contour: ShapeContour -> contour.segments.isNotEmpty() && (contour.nearest(p).position - p).length <= weight / 2.0 }

/** true when [p], in the coordinates of [node], is inside the clip path of [node], or the node has none */
private fun insideClip(node: CompositionNode, p: Vector2): Boolean {
    val clip = node.style.clipPath as? ClipPath.Shapes ?: return true
    val point = when (clip.units) {
        PaintUnits.OBJECT -> p
        PaintUnits.BOUNDS -> {
            val bounds = localBounds(node) ?: return false
            (boundsTransform(bounds).inversed * p.xy01).xy
        }
    }
    fun hit(n: CompositionNode, q: Vector2): Boolean {
        if (n.style.display == Display.None) return false
        val local = (n.transform.inversed * q.xy01).xy
        return when (n) {
            is ShapeNode -> n.effectiveStyle.visibility.value && shapeContains(n.shape, local, n.effectiveStyle.clipRule)
            is GroupNode -> n.children.any { hit(it, local) }
            else -> false
        }
    }
    return clip.content.children.any { hit(it, point) }
}

/**
 * The nodes at [point], topmost first, like SVG hit-testing: shapes are hit on their fill and stroke as their
 * pointer-events allow, with their fill rule; text on the cells of its glyphs, images on their rectangle. Nodes that are
 * not displayed, and points outside of clip paths, are not hit. Groups are not hit themselves, the nodes they contain
 * are; [CompositionNode.parent] leads to them. The point is in the coordinates the composition is drawn in, those of
 * the drawer before the composition is drawn.
 */
fun Composition.hitTest(point: Vector2): List<CompositionHit> {
    val hits = mutableListOf<CompositionHit>()
    fun visit(node: CompositionNode, parentPoint: Vector2) {
        if (node.style.display == Display.None) return
        val p = (node.transform.inversed * parentPoint.xy01).xy
        if (!insideClip(node, p)) return
        val style = node.effectiveStyle
        val events = style.pointerEvents
        val visible = style.visibility.value || !events.visible
        when (node) {
            is GroupNode -> {
                if (node.textElement) {
                    if (events != PointerEvents.NONE && visible) {
                        val bounds = CompositionText.measure(node, Vector2.ZERO)
                        if (bounds != null && p in bounds) hits.add(CompositionHit(node, p))
                    }
                } else {
                    // the last child is drawn on top
                    for (child in node.children.asReversed()) visit(child, p)
                }
            }
            is ShapeNode -> if (visible) {
                val fillHit = events.fill && (!events.painted || style.fill !is Paint.None) &&
                        shapeContains(node.shape, p, style.fillRule)
                val strokeHit = !fillHit && events.stroke && (!events.painted || style.stroke !is Paint.None) &&
                        strokeContains(node.shape, p, (style.strokeWeight as? Length.Pixels)?.value ?: 1.0)
                if (fillHit || strokeHit) hits.add(CompositionHit(node, p))
            }
            is ImageNode -> if (visible && events != PointerEvents.NONE &&
                p.x >= node.x && p.y >= node.y && p.x <= node.x + node.width && p.y <= node.y + node.height
            ) hits.add(CompositionHit(node, p))
            is TextNode -> {}
        }
    }
    visit(root, (calculateViewportTransform().inversed * point.xy01).xy)
    return hits
}

/** the topmost node at [point], see [hitTest], null when there is none */
fun Composition.nodeAt(point: Vector2): CompositionNode? = hitTest(point).firstOrNull()?.node

/**
 * An event of [CompositionInteraction]: the [node] it is about, the [point] in the coordinates of the node, and the
 * mouse event that caused it
 */
class CompositionPointerEvent(val node: CompositionNode, val point: Vector2, val mouseEvent: MouseEvent?) {
    /** the target of the link that the node is in, null when it is in none */
    val link: String? get() = node.effectiveLink
}

/**
 * Interaction with a composition by the mouse, like the pointer events of SVG documents in browsers:
 * [entered] and [exited] when the pointer moves onto or off a node and the groups it is in, [pressed], [released]
 * and [clicked] for the topmost node under the pointer. [transform] maps the coordinates of the composition to those
 * of the mouse, the transform the composition is drawn with.
 *
 * ```
 * val interaction = CompositionInteraction(composition).apply { listenTo(mouse) }
 * interaction.clicked.listen { println("clicked ${it.node.id} ${it.link}") }
 * ```
 */
class CompositionInteraction(val composition: Composition) {
    var transform: Matrix44 = Matrix44.IDENTITY

    val entered = Event<CompositionPointerEvent>("composition-entered")
    val exited = Event<CompositionPointerEvent>("composition-exited")
    val pressed = Event<CompositionPointerEvent>("composition-pressed")
    val released = Event<CompositionPointerEvent>("composition-released")
    val clicked = Event<CompositionPointerEvent>("composition-clicked")

    /** the topmost node under the pointer and the groups it is in, the node first */
    var hovered: List<CompositionNode> = emptyList()
        private set

    /** the topmost node under the pointer, null when there is none */
    val hoveredNode: CompositionNode? get() = hovered.firstOrNull()

    private fun hit(position: Vector2): CompositionHit? =
        composition.hitTest((transform.inversed * position.xy01).xy).firstOrNull()

    /** updates [hovered] for the pointer at [position], sends [exited] and [entered] for the nodes that change */
    fun move(position: Vector2, mouseEvent: MouseEvent? = null) {
        val hit = hit(position)
        val path = hit?.node?.let { node -> generateSequence(node) { it.parent }.toList() } ?: emptyList()
        val point = hit?.point ?: position
        for (node in hovered) if (path.none { it === node }) exited.trigger(CompositionPointerEvent(node, point, mouseEvent))
        val previous = hovered
        hovered = path
        for (node in path.asReversed()) if (previous.none { it === node }) entered.trigger(CompositionPointerEvent(node, point, mouseEvent))
    }

    /** the node that was pressed, a click is a press and a release on the same node */
    private var pressedNode: CompositionNode? = null

    /** sends [pressed] for the node at the position of [mouseEvent] */
    fun press(mouseEvent: MouseEvent) {
        val hit = hit(mouseEvent.position)
        pressedNode = hit?.node
        hit?.let { pressed.trigger(CompositionPointerEvent(it.node, it.point, mouseEvent)) }
    }

    /** sends [released], and [clicked] when the node was pressed too, for the node at the position of [mouseEvent] */
    fun release(mouseEvent: MouseEvent) {
        val hit = hit(mouseEvent.position)
        val wasPressed = pressedNode
        pressedNode = null
        hit ?: return
        val event = CompositionPointerEvent(hit.node, hit.point, mouseEvent)
        released.trigger(event)
        if (wasPressed === hit.node) clicked.trigger(event)
    }

    /** listens to the events of [mouse] */
    fun listenTo(mouse: MouseEvents) {
        mouse.moved.listen { move(it.position, it) }
        mouse.dragged.listen { move(it.position, it) }
        mouse.buttonDown.listen { press(it) }
        mouse.buttonUp.listen { release(it) }
    }
}
