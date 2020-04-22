package org.openrndr.extra.fx.color

import org.openrndr.draw.Filter
import org.openrndr.draw.filterShaderFromUrl
import org.openrndr.extra.fx.filterFragmentUrl

class RgbToYCbcr : Filter(filterShaderFromUrl(filterFragmentUrl("color/rgb-to-ycbcr.frag")))
class YcbcrToRgb : Filter(filterShaderFromUrl(filterFragmentUrl("color/ycbcr-to-rgb.frag")))