package org.openrndr.extra.color.tools

import org.openrndr.color.*
import org.openrndr.extra.color.spaces.*

inline fun <reified T> ColorRGBa.blendWith(other: ColorRGBa, steps: Int): Sequence<ColorRGBa>
        where T : AlgebraicColor<T>,
        T: ColorModel<T> {
    return sequence {
        for (step in 0 until steps) {
            yield(mixedWith<T>(other, step / (steps - 1.0)))
        }
    }
}

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

inline fun <reified T> ColorRGBa.mixedWith(other: ColorRGBa, factor: Double): ColorRGBa
        where T : AlgebraicColor<T>, T : ColorModel<T> {

    val source = convertTo<T>()
    val target = other.convertTo<T>()
    val mixed = source.mix(target, factor).toRGBa()

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
}

inline fun <reified T> ColorRGBa.saturate(factor: Double): ColorRGBa
        where T : SaturatableColor<T>,
              T : ColorModel<T>,
              T : ConvertibleToColorRGBa {

    val saturated = convertTo<T>().saturate(factor).toRGBa()
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
        T : HueShiftableColor<T>,
        T : ColorModel<T>,
        T : ConvertibleToColorRGBa {
    val converted = convertTo<T>()
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

