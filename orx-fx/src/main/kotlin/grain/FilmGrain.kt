import org.openrndr.draw.Filter
import org.openrndr.draw.Shader
import org.openrndr.extra.fx.filterFragmentCode

/**
 * Film grain filter
 */
class FilmGrain : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("grain/film-grain.frag"))) {
    var useColor: Boolean by parameters
    var time: Double by parameters;
    var grainLiftRatio: Double by parameters
    var grainStrength: Double by parameters
    var grainRate: Double by parameters
    var grainPitch: Double by parameters
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