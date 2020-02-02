package org.openrndr.extra.fx.color

import org.openrndr.draw.Filter
import org.openrndr.draw.Shader
import org.openrndr.extra.fx.filterFragmentCode

class ColorMix : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("color/color-mix.frag")))