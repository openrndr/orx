package org.openrndr.extra.dnk3.post

import org.openrndr.draw.*
import org.openrndr.extra.dnk3.features.IrradianceSH
import org.openrndr.extra.shaderphrases.preprocessShader
import org.openrndr.math.IntVector3
import org.openrndr.math.Matrix44
import org.openrndr.resourceUrl
import java.net.URL

fun preprocessedFilterShaderFromUrl(url: String): Shader {
    return filterShaderFromCode( preprocessShader(URL(url).readText()), "filter-shader: $url")
}

fun preprocessedFilterShaderFromCode(fragmentShaderCode: String, name: String): Shader {
    return Shader.createFromCode(Filter.filterVertexCode, fragmentShaderCode, name)
}

class VolumetricIrradiance : Filter(preprocessedFilterShaderFromUrl(resourceUrl("/shaders/volumetric-irradiance.frag"))) {

    var stepLength: Double by parameters
    var irradianceSH: IrradianceSH? = null

    var viewMatrixInverse: Matrix44 by parameters
    var projectionMatrixInverse: Matrix44 by parameters

    init {
        stepLength = 0.1
        viewMatrixInverse = Matrix44.IDENTITY
        projectionMatrixInverse = Matrix44.IDENTITY
    }

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>) {
        irradianceSH?.shMap?.let {
            parameters["shMap"] = it
        }
        irradianceSH?.let {
            parameters["shMapDimensions"] = IntVector3(it.xCount, it.yCount, it.zCount)
            parameters["shMapOffset"] = it.offset
            parameters["shMapSpacing"] = it.spacing
        }
        super.apply(source, target)
    }
}

