package org.openrndr.extra.palette

import org.openrndr.color.ColorRGBa

// https://www.w3.org/TR/2008/REC-WCAG20-20081211/#relativeluminancedef
fun getLuminance(color: ColorRGBa): Double = 0.2126 * color.r + 0.7152 * color.g + 0.0722 * color.b

// see http://www.w3.org/TR/2008/REC-WCAG20-20081211/#contrast-ratiodef
fun getContrast(colorA: ColorRGBa, colorB: ColorRGBa): Double {
    val l1 = getLuminance(colorA)
    val l2 = getLuminance(colorB)

    return if (l1 > l2) (l1 + 0.05) / (l2 + 0.05) else (l2 + 0.05) / (l1 + 0.05)
}
