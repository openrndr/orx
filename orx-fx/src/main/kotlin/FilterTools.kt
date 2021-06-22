package org.openrndr.extra.fx

import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.resourceUrl

internal class FilterTools


internal fun filterFragmentUrl(resourceId: String): String {
    return resourceUrl("gl3/$resourceId", FilterTools::class)
}

internal data class ColorBufferDescription(val width: Int, val height: Int, val contentScale: Double, val format: ColorFormat, val type: ColorType)
