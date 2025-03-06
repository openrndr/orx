package org.openrndr.extra.shadestyles.fills.gradients

import org.openrndr.color.AlgebraicColor
import org.openrndr.color.ColorRGBa
import org.openrndr.color.ConvertibleToColorRGBa
import org.openrndr.draw.ObservableHashmap
import org.openrndr.draw.ShadeStyle
import org.openrndr.draw.StyleParameters
import org.openrndr.extra.shadestyles.fills.FillFit
import org.openrndr.extra.shadestyles.fills.FillUnits
import org.openrndr.extra.shadestyles.fills.SpreadMethod
import org.openrndr.math.CastableToVector4
import org.openrndr.math.Vector4
import kotlin.reflect.KClass

class GradientBuilder<C>(val colorType: KClass<C>): StyleParameters
        where C : ConvertibleToColorRGBa, C : AlgebraicColor<C>, C : CastableToVector4 {
    override var parameterTypes: ObservableHashmap<String, String> = ObservableHashmap(mutableMapOf()) {}
    override var parameterValues: MutableMap<String, Any> = mutableMapOf()
    override var textureBaseIndex: Int = 2

    var filterWindow: Int by Parameter("filterWindow", 3)
    var filterSpread: Double by Parameter("filterSpread", 0.5)

    var stops = mutableMapOf<Double, C>()
    var fillUnits = FillUnits.BOUNDS
    var fillFit = FillFit.STRETCH
    var spreadMethod = SpreadMethod.PAD
    var levelWarpFunction = """float levelWarp(vec2 coord, float level) { return level; }"""
    var domainWarpFunction = """vec2 domainWarp(vec2 coord) { return coord; }"""
    var gradientFunction = """float gradientFunction(vec2 coord) { return 0.0; }"""
    var quantization = 0

    private fun setBaseParameters(style: GradientBase<C>) {
        style.parameterTypes.putAll(parameterTypes)
        style.parameterValues.putAll(parameterValues)
        style.quantization = quantization
        style.spreadMethod = spreadMethod.ordinal
        style.fillUnits = fillUnits.ordinal
        style.fillFit = fillFit.ordinal
    }

    @PublishedApi
    internal var shadeStyleBuilder: GradientShadeStyleBuilder<C> = LinearGradientBuilder(this)

    /**
     * Configures a linear gradient by applying the provided builder block.
     *
     * @param builder A lambda function used to define the properties of the linear gradient.
     *                The builder block allows customization of attributes such as
     *                start and end positions.
     */
    fun linear(builder: LinearGradientBuilder<C>.() -> Unit) {
        shadeStyleBuilder = LinearGradientBuilder(this).apply { builder() }
        gradientFunction = LinearGradient.gradientFunction
    }

    /**
     * Configures a radial gradient by applying the provided builder block.
     *
     * @param builder A lambda function used to define the properties of the radial gradient.
     *                The builder block allows customization of attributes such as the center,
     *                radius, focal center, and focal radius.
     */
    fun radial(builder: RadialGradientBuilder<C>.() -> Unit) {
        shadeStyleBuilder = RadialGradientBuilder(this).apply { builder() }
        gradientFunction = RadialGradient.gradientFunction
    }

    fun elliptic(builder: EllipticalGradientBuilder<C>.() -> Unit) {
        shadeStyleBuilder = EllipticalGradientBuilder(this).apply { builder() }
        gradientFunction = EllipticalGradient.gradientFunction
    }

    /**
     * Configures a conic gradient by applying the provided builder block.
     *
     * @param builder A lambda function used to define the properties of the conic gradient.
     *                The builder block allows customization of attributes such as the center,
     *                angle, start angle, and rotation.
     */
    fun conic(builder: ConicGradientBuilder<C>.() -> Unit) {
        shadeStyleBuilder = ConicGradientBuilder(this).apply { builder() }
        gradientFunction = ConicGradient.gradientFunction
    }

    /**
     * Configures a stellar gradient by applying the provided builder block.
     *
     * @param builder A lambda function used to define the properties of the stellar gradient.
     *                The builder block allows customization of attributes such as center, radius,
     *                sharpness, rotation, and the number of sides.
     */
    fun stellar(builder: StellarGradientBuilder<C>.() -> Unit) {
        shadeStyleBuilder = StellarGradientBuilder(this).apply { builder() }
        gradientFunction = StellarGradient.gradientFunction
    }

    internal fun extractSteps(): List<Pair<Double, C>> {
        return stops.entries.sortedBy { it.key }.map {
            Pair(it.key, it.value)
        }
    }

    internal fun extractStepsUnzip(): Pair<Array<Double>, Array<Vector4>> {
        val steps = extractSteps()
        val stopsArray = Array(steps.size) { steps[it].first }
        val colorsArray = Array(steps.size) {
            (steps[it].second.let { c ->
                if (c is ColorRGBa) {
                    c.toLinear()
                } else {
                    c
                }
            }).toVector4()

        }
        return Pair(stopsArray, colorsArray)
    }

    internal fun structure(): GradientBaseStructure =
        GradientBaseStructure(gradientFunction, domainWarpFunction, levelWarpFunction)

    @PublishedApi
    internal fun build(): GradientBase<C> {
        return this.shadeStyleBuilder.build().apply {
            setBaseParameters(this)
        }
    }
}

sealed interface GradientShadeStyleBuilder<C>
        where C : ConvertibleToColorRGBa, C : AlgebraicColor<C>, C : CastableToVector4 {

    /**
     * Constructs and returns a `GradientBase` object representing a gradient with the
     * desired configuration defined in the implementing class.
     *
     * @return An instance of `GradientBase` configured with the specified gradient parameters.
     */
    fun build(): GradientBase<C>
}


/**
 * Creates a gradient shade style using the specified configuration.
 *
 * The method allows for building a gradient using a DSL-like approach,
 * where different properties such as gradient stops, gradient type, and
 * other configurations can be set.
 *
 * @param builder A lambda function used to configure the gradient properties
 *                through an instance of [GradientBuilder].
 * @return A [ShadeStyle] instance representing the configured gradient.
 */
inline fun <reified C> gradient(builder: GradientBuilder<C>.() -> Unit): ShadeStyle
        where C : ConvertibleToColorRGBa, C : AlgebraicColor<C>, C : CastableToVector4 {
    val gb = GradientBuilder(C::class)
    gb.builder()
    return gb.build()
}