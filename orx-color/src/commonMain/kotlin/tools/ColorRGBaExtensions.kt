package org.openrndr.extra.color.tools

import org.openrndr.color.*
import org.openrndr.extra.color.spaces.*

/**
 * Indicates whether the color is out of the RGB gamut.
 *
 * This property evaluates if the color's red, green, or blue components are outside
 * the valid range of [0.0, 1.0], accounting for a slight tolerance in the negative range (-1E-3).
 * Additionally, it checks whether the alpha component is outside the range [0.0, 1.0].
 *
 * This property is commonly used in color manipulation functions to detect and handle
 * out-of-gamut colors, which may require adjustments (e.g., clipping or chroma adjustment)
 * to fit within a valid color space.
 */
val ColorRGBa.isOutOfGamut: Boolean
    get() {
        return (r !in -1E-3..1.0) || (g !in -1E-3..1.0) || (b !in -1E-3..1.0) || (alpha !in 0.0..1.0)
    }

/**
 * Matches the linearity of the current `ColorRGBa` instance with another `ColorRGBa` instance.
 * If the linearity of `other` matches that of the current instance, the current instance is returned.
 * Otherwise, it converts the current instance to match the linearity of `other`.
 *
 * @param other The `ColorRGBa` instance whose linearity is to be matched.
 * @return A `ColorRGBa` instance with the same linearity as the `other` color.
 */
fun ColorRGBa.matchLinearity(other: ColorRGBa): ColorRGBa {
    return if (other.linearity.isEquivalent(linearity)) {
        this
    } else {
        if (other.linearity.isEquivalent(Linearity.LINEAR)) {
            toLinear()
        } else if (other.linearity.isEquivalent(Linearity.SRGB)) {
            toSRGB()
        } else {
            this
        }
    }
}

inline fun <reified T> ColorRGBa.hue(): Double
        where T : HueShiftableColor<T>,
              T : ColorModel<T> = convertTo<T>().hue

inline fun <reified T> ColorRGBa.blendWith(other: ColorRGBa, steps: Int): Sequence<ColorRGBa>
        where T : AlgebraicColor<T>,
              T : ColorModel<T> {
    return sequence {
        for (step in 0 until steps) {
            yield(mixedWith<T>(other, step / (steps - 1.0)))
        }
    }
}


/**
 * Converts the current `ColorRGBa` instance to the specified color model type `T`.
 *
 * @return An instance of the specified color model type `T` after conversion.
 * @throws IllegalStateException if the specified color model is not supported.
 */
inline fun <reified T : ColorModel<T>> ColorRGBa.convertTo(): T {
    val converted = when (T::class) {
        ColorHSLa::class -> this.toHSLa()
        ColorHSVa::class -> this.toHSVa()
        ColorRGBa::class -> this
        ColorHPLUVa::class -> this.toHPLUVa()
        ColorHSLUVa::class -> this.toHSLUVa()
        ColorOKLABa::class -> this.toOKLABa()
        ColorOKLCHa::class -> this.toOKLCHa()
        ColorOKHSLa::class -> this.toOKHSLa()
        ColorOKHSVa::class -> this.toOKHSVa()
        ColorLABa::class -> this.toLABa()
        ColorLUVa::class -> this.toLUVa()
        ColorLCHABa::class -> this.toLCHABa()
        ColorLCHUVa::class -> this.toLCHUVa()
        ColorOKHSLa::class -> this.toOKHSLa()
        ColorXYZa::class -> this.toXYZa()
        ColorXSLUVa::class -> this.toXSLUVa()
        ColorXSVa::class -> this.toXSVa()
        ColorXSLa::class -> this.toXSLa()
        else -> error("color model ${T::class} not supported")
    }
    return converted as T
}

inline fun <reified T> ColorRGBa.withLuminosity(luminosity: Double): ColorRGBa
        where T : LuminosityColor<T>,
              T : ColorModel<T>,
              T : ConvertibleToColorRGBa = convertTo<T>().withLuminosity(luminosity).toRGBa().matchLinearity(this)

inline fun <reified T> ColorRGBa.luminosity(): Double
        where T : LuminosityColor<T>,
              T : ColorModel<T>,
              T : ConvertibleToColorRGBa = convertTo<T>().luminosity

inline fun <reified T> ColorRGBa.mixLuminosity(luminosity: Double, factor: Double): ColorRGBa
        where T : LuminosityColor<T>,
              T : ColorModel<T>,
              T : ConvertibleToColorRGBa =
    convertTo<T>().mixLuminosity(luminosity, factor).toRGBa().matchLinearity(this)


inline fun <reified T> ColorRGBa.shadeLuminosity(factor: Double): ColorRGBa
        where T : LuminosityColor<T>,
              T : ColorModel<T>,
              T : ConvertibleToColorRGBa =
    convertTo<T>().shadeLuminosity(factor).toRGBa().matchLinearity(this)


inline fun <reified T> ColorRGBa.mixHue(hue: Double, factor: Double): ColorRGBa
        where T : HueShiftableColor<T>,
              T : ColorModel<T>,
              T : ConvertibleToColorRGBa = convertTo<T>().mixHue(hue, factor).toRGBa().matchLinearity(this)

inline fun <reified T> ColorRGBa.withHue(hue: Double): ColorRGBa
        where T : HueShiftableColor<T>,
              T : ColorModel<T>,
              T : ConvertibleToColorRGBa = convertTo<T>().withHue(hue).toRGBa().matchLinearity(this)

inline fun <reified T> ColorRGBa.mixSaturation(saturation: Double, factor: Double): ColorRGBa
        where T : SaturatableColor<T>,
              T : ColorModel<T>,
              T : ConvertibleToColorRGBa =
    convertTo<T>().mixSaturation(saturation, factor).toRGBa().matchLinearity(this)


inline fun <reified T> ColorRGBa.mixedWith(other: ColorRGBa, factor: Double): ColorRGBa
        where T : AlgebraicColor<T>, T : ColorModel<T> {
    val source = convertTo<T>()
    val target = other.convertTo<T>()
    return source.mix(target, factor).toRGBa().matchLinearity(this)
}

inline fun <reified T> ColorRGBa.mixChroma(chroma: Double, factor: Double): ColorRGBa
        where T : ChromaColor<T>,
              T : ColorModel<T>,
              T : ConvertibleToColorRGBa =
    convertTo<T>().mixChroma(chroma, factor).toRGBa().matchLinearity(this)

inline fun <reified T> ColorRGBa.withChroma(chroma: Double): ColorRGBa
        where T : ChromaColor<T>,
              T : ColorModel<T>,
              T : ConvertibleToColorRGBa =
    convertTo<T>().withChroma(chroma).toRGBa().matchLinearity(this)

inline fun <reified T> ColorRGBa.chroma(): Double
        where T : ChromaColor<T>,
              T : ColorModel<T>,
              T : ConvertibleToColorRGBa =
    convertTo<T>().chroma

inline fun <reified T> ColorRGBa.modulateChroma(factor: Double): ColorRGBa
        where T : ChromaColor<T>,
              T : ColorModel<T>,
              T : ConvertibleToColorRGBa =
    convertTo<T>().modulateChroma(factor).toRGBa().matchLinearity(this)


/**
 * Adjusts the saturation of the current `ColorRGBa` based on a given factor.
 *
 * @param T The target color model type that supports saturation adjustments.
 * @param factor The saturation adjustment factor. A value of 1.0 keeps the saturation unchanged,
 *               values less than 1.0 decrease saturation, and values greater than 1.0 increase it.
 * @return A new `ColorRGBa` instance with the adjusted saturation, maintaining the linearity of
 *         the original color.
 */
inline fun <reified T> ColorRGBa.saturate(factor: Double): ColorRGBa
        where T : SaturatableColor<T>,
              T : ColorModel<T>,
              T : ConvertibleToColorRGBa = convertTo<T>().saturate(factor).toRGBa().matchLinearity(this)

/**
 * Shifts the hue of the current `ColorRGBa` by the specified number of degrees.
 * The method is only applicable to color models that support hue shifting and can be converted to `ColorRGBa`.
 *
 * @param degrees The amount of hue adjustment in degrees. Positive values shift the hue clockwise, while negative values shift it counterclockwise.
 * @return A new `ColorRGBa` instance with the hue shifted by the specified degree, maintaining the same linearity as the input color.
 */
inline fun <reified T> ColorRGBa.shiftHue(degrees: Double): ColorRGBa where
        T : HueShiftableColor<T>,
        T : ColorModel<T>,
        T : ConvertibleToColorRGBa = convertTo<T>().shiftHue(degrees).toRGBa().matchLinearity(this)

inline fun <reified T> ColorRGBa.clipChroma(): ColorRGBa
        where T : ChromaColor<T>,
              T : ColorModel<T>,
              T : ConvertibleToColorRGBa =

    if (isOutOfGamut) {
        convertTo<T>().clipChroma().toRGBa().matchLinearity(this).clip()
    } else {
        this
    }
