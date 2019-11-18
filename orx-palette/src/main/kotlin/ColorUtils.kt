package org.openrndr.extra.palette

import org.openrndr.color.ColorRGBa
import org.openrndr.color.Linearity

// https://www.w3.org/TR/2008/REC-WCAG20-20081211/#relativeluminancedef
internal fun getLuminance(color: ColorRGBa): Double = 0.2126 * color.r + 0.7152 * color.g + 0.0722 * color.b

// see http://www.w3.org/TR/2008/REC-WCAG20-20081211/#contrast-ratiodef
internal fun getContrast(colorA: ColorRGBa, colorB: ColorRGBa): Double {
    val l1 = getLuminance(colorA)
    val l2 = getLuminance(colorB)

    return if (l1 > l2) (l1 + 0.05) / (l2 + 0.05) else (l2 + 0.05) / (l1 + 0.05);
}

// TODO(ricardomatias) Remove this when 0.3.36 is released
internal fun fromHex(hex: String): ColorRGBa {
    val parsedHex = hex.replace("#", "")
    val len = parsedHex.length
    val mult = len / 3

    val colors = (0..2).map { idx ->
        var c = parsedHex.substring(idx * mult, (idx + 1) * mult)

        c = if (len == 3) c + c else c

        Integer.valueOf(c, 16)
    }

    val (r, g, b) = colors

    return ColorRGBa(r / 255.0, g / 255.0, b / 255.0, 1.0, Linearity.SRGB)
}