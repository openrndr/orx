package org.openrndr.extra.color.tools

import org.openrndr.color.*
import org.openrndr.extra.color.spaces.*

inline fun <reified T : AlgebraicColor<T>> ColorRGBa.blendWith(other: ColorRGBa, steps: Int): Sequence<ColorRGBa> {
    return sequence {
        for (step in 0 until steps) {
            yield(mixedWith<T>(other, step / (steps - 1.0)))
        }
    }
}

inline fun <reified T : AlgebraicColor<T>> ColorRGBa.mixedWith(other: ColorRGBa, factor: Double): ColorRGBa {

    val mixed = when (T::class) {
        ColorHSLa::class -> this.toHSLa().mix(other.toHSLa(), factor)
        ColorHSVa::class -> this.toHSVa().mix(other.toHSVa(), factor)
        ColorRGBa::class -> this.mix(other, factor)

        ColorHSLUVa::class -> this.toHSLUVa().mix(other.toHSLUVa(), factor)
        ColorOKLABa::class -> this.toOKLABa().mix(other.toOKLABa(), factor)
        ColorOKLCHa::class -> this.toOKLCHa().mix(other.toOKLCHa(), factor)
        ColorOKHSLa::class -> this.toOKHSLa().mix(other.toOKHSLa(), factor)
        ColorOKHSVa::class -> this.toOKHSVa().mix(other.toOKHSVa(), factor)

        ColorLABa::class -> this.toLABa().mix(other.toLABa(), factor)
        ColorLUVa::class -> this.toLUVa().mix(other.toLUVa(), factor)
        ColorLCHABa::class -> this.toLCHABa().mix(other.toLCHABa(), factor)
        ColorLCHUVa::class -> this.toLCHUVa().mix(other.toLCHUVa(), factor)
        ColorOKHSLa::class -> this.toOKHSLa().mix(other.toOKHSLa(), factor)
        ColorXYZa::class -> this.toXYZa().mix(other.toXYZa(), factor)
        ColorXSLUVa::class -> this.toXSLUVa().mix(other.toXSLUVa(), factor)
        ColorXSVa::class -> this.toXSVa().mix(other.toXSVa(), factor)
        ColorXSLa::class -> this.toXSLa().mix(other.toXSLa(), factor)
        else -> error("color model ${T::class} not supported")
    }.toRGBa()

    return if (mixed.linearity.isEquivalent(linearity)) {
        mixed
    } else {
        if (linearity.isEquivalent(Linearity.LINEAR)) {
            mixed.toLinear()
        } else if (linearity.isEquivalent(Linearity.SRGB)) {
            mixed.toSRGB()
        } else {
            mixed
        }
    }

    return this
}

inline fun <reified T> ColorRGBa.saturate(factor: Double): ColorRGBa
        where T : SaturatableColor<T>,
              T : ConvertibleToColorRGBa {

    val converted = when (T::class) {
        ColorHPLUVa::class -> toHPLUVa()
        ColorHSLUVa::class -> toHSLUVa()
        ColorHSLa::class -> toHSLa()
        ColorHSVa::class -> toHSVa()
        ColorXSLa::class -> toXSLa()
        ColorXSVa::class -> toXSVa()
        ColorOKLCHa::class -> toOKLCHa()
        ColorOKHSLa::class -> toOKHSLa()
        ColorOKHSVa::class -> toOKHSVa()
        ColorXSLUVa::class -> toXSLUVa()
        ColorOKLCHa::class -> toOKLCHa()
        else -> error("Color space ${T::class} not supported")
    }
    val saturated = (converted.saturate(factor) as ConvertibleToColorRGBa).toRGBa()

    return if (saturated.linearity.isEquivalent(linearity)) {
        saturated
    } else {
        if (linearity.isEquivalent(Linearity.LINEAR)) {
            saturated.toLinear()
        } else if (linearity.isEquivalent(Linearity.SRGB)) {
            saturated.toSRGB()
        } else {
            saturated
        }
    }
}

inline fun <reified T> ColorRGBa.shiftHue(degrees: Double): ColorRGBa where
        T : HueShiftableColor<T>, T : ConvertibleToColorRGBa {
    val converted = when (T::class) {
        ColorHSLa::class -> toHSLa()
        ColorHSVa::class -> toHSVa()
        ColorXSLa::class -> toXSLa()
        ColorXSVa::class -> toXSVa()
        ColorOKLCHa::class -> toOKLCHa()
        ColorLCHABa::class -> toLCHABa()
        ColorLCHUVa::class -> toLCHABa()
        ColorOKHSLa::class -> toOKHSLa()
        ColorOKHSVa::class -> toOKHSVa()
        ColorHPLUVa::class -> toHPLUVa()
        ColorHSLUVa::class -> toHSLUVa()
        ColorXSLUVa::class -> toXSLUVa()
        else -> error("Color space ${T::class} not supported")
    }
    val shifted = (converted.shiftHue(degrees) as ConvertibleToColorRGBa).toRGBa()
    return if (shifted.linearity.isEquivalent(linearity)) {
        shifted
    } else {
        if (linearity.isEquivalent(Linearity.LINEAR)) {
            shifted.toLinear()
        } else if (linearity.isEquivalent(Linearity.SRGB)) {
            shifted.toSRGB()
        } else {
            shifted
        }
    }
}

