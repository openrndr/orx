@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

import org.openrndr.draw.Filter1to1
import org.openrndr.draw.filterShaderFromCode
import org.openrndr.extra.fx.fx_multiply_u
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

/**
 * Multiply by u coordinate
 */
@Description("multiply u")
class MultiplyU : Filter1to1(filterShaderFromCode(fx_multiply_u, "multiply-u")) {
    @DoubleParameter("multiplication bias", 0.0, 2.0)
    var bias: Double by parameters
    init {
        bias = 0.0
    }
}