package org.openrndr.extra.fx

import org.openrndr.resourceUrl
import java.net.URL

internal class FilterTools

internal fun filterFragmentCode(resourceId: String): String {
    val urlString = resourceUrl("gl3/$resourceId", FilterTools::class.java)
    return URL(urlString).readText()
}