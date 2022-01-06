import org.openrndr.draw.Filter
import org.openrndr.draw.filterShaderFromCode
import org.openrndr.extra.fx.fx_square
import org.openrndr.extra.parameters.Description
/**
 * Square input texture values
 */
@Description("square")
class Square : Filter(filterShaderFromCode(fx_square, "square")) {
}