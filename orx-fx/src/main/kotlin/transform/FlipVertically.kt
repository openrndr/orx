package org.openrndr.extra.fx.transform

import org.openrndr.draw.Filter
import org.openrndr.draw.Shader
import org.openrndr.extra.fx.filterFragmentCode

/**
 * Vertically flips in the input image
 */
class FlipVertically : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("transform/flip-vertically.frag")))