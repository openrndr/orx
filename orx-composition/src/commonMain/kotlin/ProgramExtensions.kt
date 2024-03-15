package org.openrndr.extra.composition

import org.openrndr.Program
import org.openrndr.shape.Rectangle
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

// Derives Composition dimensions from current Drawer
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


