package org.openrndr.extra.dnk3

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Cubemap
import org.openrndr.draw.RenderTarget
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.ortho
import org.openrndr.math.transforms.perspective

@JvmRecord
data class LightContext(val lights: List<NodeContent<Light>>,
                        val shadowMaps: Map<ShadowLight, RenderTarget>)

interface AttenuatedLight {
    var constantAttenuation: Double
    var linearAttenuation: Double
    var quadraticAttenuation: Double
}

class DirectionalLight(var direction: Vector3 = -Vector3.UNIT_Z, override var shadows: Shadows = Shadows.None) : Light(), ShadowLight {
    var projectionSize = 50.0

    override fun projection(renderTarget: RenderTarget): Matrix44 {
        return ortho(-projectionSize / 2.0, projectionSize / 2.0, -projectionSize / 2.0, projectionSize / 2.0, 1.0, 150.0)
    }

    override fun hashCode(): Int {
        return color.hashCode()
    }
}

class SpotLight(var direction: Vector3 = -Vector3.UNIT_Z, var innerAngle: Double = 45.0, var outerAngle: Double = 90.0) : Light(), ShadowLight, AttenuatedLight {
    override var constantAttenuation = 1.0
    override var linearAttenuation = 0.0
    override var quadraticAttenuation = 0.0
    override var shadows: Shadows = Shadows.None
    override fun projection(renderTarget: RenderTarget): Matrix44 {
        return perspective(outerAngle * 2.0, renderTarget.width * 1.0 / renderTarget.height, 1.0, 150.0)
    }

    override fun hashCode(): Int {
        var result = direction.hashCode()
        result = 31 * result + innerAngle.hashCode()
        result = 31 * result + outerAngle.hashCode()
        result = 31 * result + constantAttenuation.hashCode()
        result = 31 * result + linearAttenuation.hashCode()
        result = 31 * result + quadraticAttenuation.hashCode()
        return result
    }
}

class HemisphereLight(var direction: Vector3 = Vector3.UNIT_Y,
                      var upColor: ColorRGBa = ColorRGBa.WHITE,
                      var downColor: ColorRGBa = ColorRGBa.BLACK) : Light() {
    var irradianceMap: Cubemap? = null
    override fun hashCode(): Int {
        var result = direction.hashCode()
        result = 31 * result + upColor.hashCode()
        result = 31 * result + downColor.hashCode()
        return result
    }

}

class PointLight(var constantAttenuation: Double = 1.0,
                 var linearAttenuation: Double = 0.0,
                 var quadraticAttenuation: Double = 1.0) : Light() {
    override fun hashCode(): Int {
        var result = constantAttenuation.hashCode()
        result = 31 * result + linearAttenuation.hashCode()
        result = 31 * result + quadraticAttenuation.hashCode()
        result = 31 * result + color.hashCode()
        return result
    }
}

class AmbientLight : Light() {

    override fun hashCode(): Int {
        return color.hashCode()
    }
}
