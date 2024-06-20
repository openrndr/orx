@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

import org.openrndr.draw.Filter1to1
import org.openrndr.draw.filterShaderFromCode
import org.openrndr.extra.fx.fx_square
import org.openrndr.extra.parameters.Description

/**
 * Square input texture values
 */
@Description("square")
class Square : Filter1to1(filterShaderFromCode(fx_square, "square")) {
}