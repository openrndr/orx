package org.openrndr.extra.composition

import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shadestyles.presets.PaintUnits
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.math.pow

/*
 * Filter effects, like the filter elements of SVG: graphs of primitives that are applied to what a node draws. The
 * primitives are evaluated on the layer of the node, see CompositionFilterEvaluation.
 */

/** an input of a filter primitive */
sealed interface FilterInput {
    /** the result of the previous primitive, or [SourceGraphic] for the first primitive */
    object Previous : FilterInput

    /** what the node draws */
    object SourceGraphic : FilterInput

    /** the alpha of what the node draws */
    object SourceAlpha : FilterInput

    /** the result of an earlier primitive with [name] */
    class Result(val name: String) : FilterInput

    /** an input that is not supported, such as BackgroundImage, it is transparent */
    class Unsupported(val name: String) : FilterInput
}

/** the color space that a primitive computes in, like color-interpolation-filters */
enum class FilterColorSpace {
    LINEAR_RGB,
    SRGB
}

/**
 * A primitive of a [FilterEffect]: it computes an image from its inputs, which is the input of the next primitive
 * and, when it has a [result] name, of later primitives that refer to it. Lengths are in the units of the
 * [FilterEffect.primitiveUnits].
 */
sealed class FilterPrimitive {
    /** the name of the result, null when only the next primitive uses it */
    var result: String? = null

    var colorSpace: FilterColorSpace = FilterColorSpace.LINEAR_RGB

    /** the inputs, in the order of in and in2 */
    abstract val inputs: List<FilterInput>

    /** a Gaussian blur with standard deviations along the x and y axes */
    class GaussianBlur(var input: FilterInput = FilterInput.Previous, var stdDeviation: Vector2) : FilterPrimitive() {
        override val inputs get() = listOf(input)
    }

    /** the input moved by ([dx], [dy]) */
    class Offset(var input: FilterInput = FilterInput.Previous, var dx: Double, var dy: Double) : FilterPrimitive() {
        override val inputs get() = listOf(input)
    }

    /** an image of a single [color] */
    class Flood(var color: ColorRGBa) : FilterPrimitive() {
        override val inputs get() = emptyList<FilterInput>()
    }

    /** the inputs drawn over each other, the first at the bottom */
    class Merge(override val inputs: List<FilterInput>) : FilterPrimitive()

    /** the Porter-Duff operators of feComposite, and its arithmetic operator */
    enum class CompositeOperator { OVER, IN, OUT, ATOP, XOR, LIGHTER, ARITHMETIC }

    /** [input] combined with [input2] by [operator]; the arithmetic operator computes k1·i·i2 + k2·i + k3·i2 + k4 */
    class Composite(
        var input: FilterInput = FilterInput.Previous,
        var input2: FilterInput,
        var operator: CompositeOperator = CompositeOperator.OVER,
        var k1: Double = 0.0, var k2: Double = 0.0, var k3: Double = 0.0, var k4: Double = 0.0
    ) : FilterPrimitive() {
        override val inputs get() = listOf(input, input2)
    }

    /** [input] blended with [input2], its backdrop, by [mode] */
    class Blend(
        var input: FilterInput = FilterInput.Previous,
        var input2: FilterInput,
        var mode: MixBlendMode = MixBlendMode.NORMAL
    ) : FilterPrimitive() {
        override val inputs get() = listOf(input, input2)
    }

    /**
     * The colors of the input multiplied by a 4×5 matrix of [values], row by row, like feColorMatrix with
     * type="matrix"; use [saturate], [hueRotate] and [luminanceToAlpha] for its other types
     */
    class ColorMatrix(var input: FilterInput = FilterInput.Previous, var values: List<Double>) : FilterPrimitive() {
        override val inputs get() = listOf(input)

        companion object {
            /** the matrix of feColorMatrix with type="saturate" */
            fun saturate(s: Double) = listOf(
                0.213 + 0.787 * s, 0.715 - 0.715 * s, 0.072 - 0.072 * s, 0.0, 0.0,
                0.213 - 0.213 * s, 0.715 + 0.285 * s, 0.072 - 0.072 * s, 0.0, 0.0,
                0.213 - 0.213 * s, 0.715 - 0.715 * s, 0.072 + 0.928 * s, 0.0, 0.0,
                0.0, 0.0, 0.0, 1.0, 0.0
            )

            /** the matrix of feColorMatrix with type="hueRotate", by [degrees] */
            fun hueRotate(degrees: Double): List<Double> {
                val a = degrees * kotlin.math.PI / 180.0
                val c = kotlin.math.cos(a)
                val s = kotlin.math.sin(a)
                return listOf(
                    0.213 + c * 0.787 - s * 0.213, 0.715 - c * 0.715 - s * 0.715, 0.072 - c * 0.072 + s * 0.928, 0.0, 0.0,
                    0.213 - c * 0.213 + s * 0.143, 0.715 + c * 0.285 + s * 0.140, 0.072 - c * 0.072 - s * 0.283, 0.0, 0.0,
                    0.213 - c * 0.213 - s * 0.787, 0.715 - c * 0.715 + s * 0.715, 0.072 + c * 0.928 + s * 0.072, 0.0, 0.0,
                    0.0, 0.0, 0.0, 1.0, 0.0
                )
            }

            /** the matrix of feColorMatrix with type="luminanceToAlpha" */
            val luminanceToAlpha = listOf(
                0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0,
                0.2125, 0.7154, 0.0721, 0.0, 0.0
            )
        }
    }

    /** a transfer function of a color channel, like the feFunc elements of feComponentTransfer */
    sealed interface TransferFunction {
        fun apply(c: Double): Double

        object Identity : TransferFunction {
            override fun apply(c: Double) = c
        }

        class Table(val values: List<Double>) : TransferFunction {
            override fun apply(c: Double): Double {
                if (values.isEmpty()) return c
                if (values.size == 1) return values[0]
                val n = values.size - 1
                val k = (c * n).toInt().coerceIn(0, n - 1)
                return values[k] + (c - k.toDouble() / n) * n * (values[k + 1] - values[k])
            }
        }

        class Discrete(val values: List<Double>) : TransferFunction {
            override fun apply(c: Double): Double {
                if (values.isEmpty()) return c
                val n = values.size
                return values[(c * n).toInt().coerceIn(0, n - 1)]
            }
        }

        class Linear(val slope: Double = 1.0, val intercept: Double = 0.0) : TransferFunction {
            override fun apply(c: Double) = slope * c + intercept
        }

        class Gamma(val amplitude: Double = 1.0, val exponent: Double = 1.0, val offset: Double = 0.0) : TransferFunction {
            override fun apply(c: Double) = amplitude * c.pow(exponent) + offset
        }
    }

    /** every channel of the input mapped by its transfer function */
    class ComponentTransfer(
        var input: FilterInput = FilterInput.Previous,
        var red: TransferFunction = TransferFunction.Identity,
        var green: TransferFunction = TransferFunction.Identity,
        var blue: TransferFunction = TransferFunction.Identity,
        var alpha: TransferFunction = TransferFunction.Identity
    ) : FilterPrimitive() {
        override val inputs get() = listOf(input)
    }

    /** the input made thinner ([dilate] false) or fatter by a rectangle of [radius] */
    class Morphology(var input: FilterInput = FilterInput.Previous, var dilate: Boolean, var radius: Vector2) :
        FilterPrimitive() {
        override val inputs get() = listOf(input)
    }

    /** a shadow of the input: its alpha blurred, moved and colored, under the input */
    class DropShadow(
        var input: FilterInput = FilterInput.Previous,
        var dx: Double = 2.0,
        var dy: Double = 2.0,
        var stdDeviation: Vector2 = Vector2(2.0, 2.0),
        var color: ColorRGBa = ColorRGBa.BLACK
    ) : FilterPrimitive() {
        override val inputs get() = listOf(input)
    }

    /**
     * Perlin turbulence, like feTurbulence: [fractalNoise] sums noise, otherwise the absolute values of noise are
     * summed. It computes the same noise as browsers for the same [seed].
     */
    class Turbulence(
        var baseFrequency: Vector2,
        var octaves: Int = 1,
        var seed: Double = 0.0,
        var fractalNoise: Boolean = false
    ) : FilterPrimitive() {
        override val inputs get() = emptyList<FilterInput>()
    }

    /** a color channel of [DisplacementMap] */
    enum class Channel { R, G, B, A }

    /** the pixels of [input] moved by the channels of [input2], by [scale] */
    class DisplacementMap(
        var input: FilterInput = FilterInput.Previous,
        var input2: FilterInput,
        var scale: Double = 0.0,
        var xChannel: Channel = Channel.A,
        var yChannel: Channel = Channel.A
    ) : FilterPrimitive() {
        override val inputs get() = listOf(input, input2)
    }
}

/**
 * A filter effect, like SVG's filter element: [primitives] applied in order to what a node draws, in [region]: in the
 * coordinates of [regionUnits], or everywhere when the region is null. Lengths of primitives are in user space, or in
 * fractions of the bounds of the node when [primitiveUnits] is [PaintUnits.BOUNDS].
 */
class FilterEffect(
    val primitives: List<FilterPrimitive>,
    val region: Rectangle? = Rectangle(-0.1, -0.1, 1.2, 1.2),
    val regionUnits: PaintUnits = PaintUnits.BOUNDS,
    val primitiveUnits: PaintUnits = PaintUnits.OBJECT,
    val id: String = ""
) {
    /** the id of the filter in SVG documents: [id], or an id derived from the filter when it has none */
    val svgId: String get() = id.ifEmpty { "filter-" + hashCode().toUInt().toString(16) }
}

/**
 * The filter effects of a node, applied one after the other, like the filter property of CSS
 */
sealed interface FilterReference : AttributeOrPropertyValue {
    override val value: List<FilterEffect>

    object None : FilterReference {
        override val value: List<FilterEffect> = emptyList()
        override fun toString() = "none"
    }

    class Value(override val value: List<FilterEffect>) : FilterReference {
        override fun toString() = value.joinToString(" ") { "url(#${it.svgId})" }
    }
}
