package org.openrndr.extra.dnk3.cubemap

import org.openrndr.draw.filterShaderFromUrl
import org.openrndr.resourceUrl

class IrradianceConvolution : CubemapFilter(filterShaderFromUrl(resourceUrl("/shaders/cubemap-filters/irradiance-convolution.frag")))
