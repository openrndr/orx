package org.openrndr.extra.shadestyles

import org.openrndr.color.ColorRGBa
import org.openrndr.extras.color.spaces.ColorOKLABa
import kotlin.reflect.KClass

internal fun generateColorTransform(kClass: KClass<*>): String {
    return when (kClass) {
        ColorRGBa::class -> """"""
        ColorOKLABa::class -> """gradient = linear_rgb_to_srgb(oklab_to_linear_rgb(gradient));"""
        else -> error("color space not supported $kClass")
    }
}