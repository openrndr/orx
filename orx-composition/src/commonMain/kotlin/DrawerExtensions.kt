package org.openrndr.extra.composition

import org.openrndr.draw.Drawer
import org.openrndr.shape.*

/**
 * Draws a [Composition]
 * @param composition The composition to draw
 * @see contour
 * @see contours
 * @see shape
 * @see shapes
 */
fun Drawer.composition(composition: Composition) {
    pushModel()
    pushStyle()

    // viewBox transformation
    model *= composition.calculateViewportTransform()

    fun node(compositionNode: CompositionNode) {
        pushModel()
        pushStyle()
        model *= compositionNode.style.transform.value

        shadeStyle = (compositionNode.style.shadeStyle as Shade.Value).value

        when (compositionNode) {
            is ShapeNode -> {

                compositionNode.style.stroke.let {
                    stroke = when (it) {
                        is Paint.RGB -> it.value.copy(alpha = 1.0)
                        Paint.None -> null
                        Paint.CurrentColor -> null
                    }
                }
                compositionNode.style.strokeOpacity.let {
                    stroke = when (it) {
                        is Numeric.Rational -> stroke?.opacify(it.value)
                    }
                }
                compositionNode.style.strokeWeight.let {
                    strokeWeight = when (it) {
                        is Length.Pixels -> it.value
                        is Length.Percent -> composition.normalizedDiagonalLength() * it.value / 100.0
                    }
                }
                compositionNode.style.miterLimit.let {
                    miterLimit = when (it) {
                        is Numeric.Rational -> it.value
                    }
                }
                compositionNode.style.lineCap.let {
                    lineCap = it.value
                }
                compositionNode.style.lineJoin.let {
                    lineJoin = it.value
                }
                compositionNode.style.fill.let {
                    fill = when (it) {
                        is Paint.RGB -> it.value.copy(alpha = 1.0)
                        is Paint.None -> null
                        is Paint.CurrentColor -> null
                    }
                }
                compositionNode.style.fillOpacity.let {
                    fill = when (it) {
                        is Numeric.Rational -> fill?.opacify(it.value)
                    }
                }
                compositionNode.style.opacity.let {
                    when (it) {
                        is Numeric.Rational -> {
                            stroke = stroke?.opacify(it.value)
                            fill = fill?.opacify(it.value)
                        }
                    }
                }
                shape(compositionNode.shape)
            }
            is ImageNode -> {
                image(compositionNode.image)
            }
            is TextNode -> TODO()
            is GroupNode -> compositionNode.children.forEach { node(it) }
        }
        popModel()
        popStyle()
    }
    node(composition.root)
    popModel()
    popStyle()
}