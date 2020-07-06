package org.openrndr.extra.dnk3.cubemap

import org.openrndr.draw.filterShaderFromUrl
import org.openrndr.resourceUrl

class CubemapPassthrough : CubemapFilter(filterShaderFromUrl(resourceUrl("/shaders/cubemap-filters/cubemap-passthrough.frag")))
