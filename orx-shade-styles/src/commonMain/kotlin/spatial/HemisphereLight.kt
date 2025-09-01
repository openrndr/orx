package org.openrndr.extra.shadestyles.spatial

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ShadeStyle
import org.openrndr.math.Vector3

class HemisphereLight: ShadeStyle() {
    var upColor: ColorRGBa by Parameter()
    var downColor: ColorRGBa by Parameter()
    var lightDirection: Vector3 by Parameter()

    init {
        lightDirection = -Vector3.UNIT_Y
        upColor = ColorRGBa.WHITE
        downColor = ColorRGBa.BLACK
        fragmentTransform = """
            vec3 n = normalize(v_worldNormal);
            float d = dot(n, p_lightDirection) * 0.5 + 0.5;
            x_fill = mix(p_upColor, p_downColor, d);
        """.trimIndent()
    }
}