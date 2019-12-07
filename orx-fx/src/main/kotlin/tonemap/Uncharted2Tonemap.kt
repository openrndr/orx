package org.openrndr.extra.fx.tonemap

import org.openrndr.draw.Filter
import org.openrndr.draw.Shader
import org.openrndr.extra.fx.filterFragmentCode

/**
 * Uncharted 2 tonemap filter
 */
class Uncharted2Tonemap : Filter(Shader.createFromCode(filterVertexCode, filterFragmentCode("tonemap/uncharted2-tonemap.frag"))) {
    var exposureBias by parameters
    init {
        exposureBias = 2.0
    }
}