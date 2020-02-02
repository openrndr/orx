import org.openrndr.draw.Filter
import org.openrndr.draw.Shader
import org.openrndr.extra.fx.filterFragmentCode
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

/**
 * Film grain filter
 */
@Description("film grain")
class FilmGrain : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("grain/film-grain.frag"))) {

    @BooleanParameter("use color")
    var useColor: Boolean by parameters

    var time: Double by parameters;

    @DoubleParameter("grain lift ratio", 0.0, 1.0)
    var grainLiftRatio: Double by parameters

    @DoubleParameter("grain strength", 0.0, 1.0)
    var grainStrength: Double by parameters

    @DoubleParameter("grain rate", 0.0, 1.0)
    var grainRate: Double by parameters

    @DoubleParameter("grain pitch", 0.0, 1.0)
    var grainPitch: Double by parameters

    @DoubleParameter("color level", 0.0, 1.0)
    var colorLevel: Double by parameters

    init {
        useColor = false
        grainLiftRatio = 0.5
        grainStrength = 1.0
        grainRate = 1.0
        grainPitch = 1.0
        colorLevel = 1.0
    }

}