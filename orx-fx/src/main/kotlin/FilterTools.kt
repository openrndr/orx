package org.openrndr.extra.fx

import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.resourceUrl
import java.net.URL

internal class FilterTools

internal fun filterFragmentCode(resourceId: String): String {
    val urlString = resourceUrl("gl3/$resourceId", FilterTools::class.java)
    return URL(urlString).readText()
}

internal data class ColorBufferDescription(val width: Int, val height: Int, val contentScale: Double, val format: ColorFormat, val type: ColorType)
