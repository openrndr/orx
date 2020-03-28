package org.openrndr.extra.fx.transform

import org.openrndr.draw.Filter
import org.openrndr.draw.Shader
import org.openrndr.draw.filterShaderFromUrl
import org.openrndr.extra.fx.filterFragmentUrl

/**
 * Vertically flips in the input image
 */
class FlipVertically : Filter(filterShaderFromUrl(filterFragmentUrl("transform/flip-vertically.frag")))