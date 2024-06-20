@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

import org.openrndr.draw.Filter1to1
import org.openrndr.draw.filterShaderFromCode
import org.openrndr.extra.fx.fx_multiply_v
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

/**
 * Multiply by v coordinate
 */
@Description("multiply v")
class MultiplyV : Filter1to1(filterShaderFromCode(fx_multiply_v, "multiply-v")) {
    @DoubleParameter("multiplication bias", 0.0, 2.0)
    var bias: Double by parameters

    @BooleanParameter("invert v")
    var invertV: Boolean by parameters

    init {
        bias = 0.0
        invertV = false
    }
}