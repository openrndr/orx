package org.openrndr.extra.composition

import org.openrndr.Program
import org.openrndr.shape.Rectangle
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Draws a composition within the specified document bounds or an existing composition.
 * This function utilizes a customizable draw function to define the drawing behavior.
 *
 * @param documentBounds Specifies the dimensions for the drawing area. Defaults to the full drawable area of the program.
 * @param composition An optional existing composition to draw onto. If not provided, a new composition is created.
 * @param cursor An optional cursor representing the current position in the composition hierarchy. Defaults to the root of the provided composition.
 * @param drawFunction A lambda function defining the drawing operations to be performed using the `CompositionDrawer`.
 * @return The resulting composition after applying the draw function.
 */
@OptIn(ExperimentalContracts::class)
fun Program.drawComposition(
    documentBounds: CompositionDimensions = CompositionDimensions(0.0.pixels, 0.0.pixels, this.drawer.width.toDouble().pixels, this.drawer.height.toDouble().pixels),
    composition: Composition? = null,
    cursor: GroupNode? = composition?.root as? GroupNode,
    drawFunction: CompositionDrawer.() -> Unit
): Composition {
    contract {
        callsInPlace(drawFunction, InvocationKind.EXACTLY_ONCE)
    }
    return CompositionDrawer(documentBounds, composition, cursor).apply { drawFunction() }.composition
}

/**
 * Draws a composition using the specified document bounds and drawing logic.
 * Optionally, an existing composition and cursor can be passed to update or build upon them.
 *
 * @param documentBounds The bounding rectangle representing the area to be drawn.
 * @param composition An optional existing composition to update. If null, a new composition will be created.
 * @param cursor An optional cursor `GroupNode` used as the starting position for appending new elements. Defaults to the root of the provided composition if available.
 * @param drawFunction A lambda function containing the drawing operations to be applied.
 * @return The resulting `Composition` object after performing the drawing operations.
 */
@OptIn(ExperimentalContracts::class)
fun Program.drawComposition(
    documentBounds: Rectangle,
    composition: Composition? = null,
    cursor: GroupNode? = composition?.root as? GroupNode,
    drawFunction: CompositionDrawer.() -> Unit
): Composition {
    contract {
        callsInPlace(drawFunction, InvocationKind.EXACTLY_ONCE)
    }
    return CompositionDrawer(CompositionDimensions(documentBounds), composition, cursor).apply { drawFunction() }.composition
}