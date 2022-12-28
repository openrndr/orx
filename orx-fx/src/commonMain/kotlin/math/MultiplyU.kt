import org.openrndr.draw.Filter
import org.openrndr.draw.Filter1to1
import org.openrndr.draw.filterShaderFromCode
import org.openrndr.extra.fx.fx_film_grain
import org.openrndr.extra.fx.fx_multiply_u
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.BooleanParameter
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