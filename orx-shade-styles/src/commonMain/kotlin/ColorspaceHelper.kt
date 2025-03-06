package org.openrndr.extra.shadestyles

import org.openrndr.color.ColorRGBa
import org.openrndr.extra.color.spaces.ColorOKLABa
import kotlin.reflect.KClass

internal fun generateColorTransform(kClass: KClass<*>, identifier: String): String {
    return when (kClass) {
        ColorRGBa::class -> """"""
        ColorOKLABa::class -> """$identifier = oklab_to_linear_rgb($identifier);"""
        else -> error("color space not supported $kClass")
    }
}