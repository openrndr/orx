package org.openrndr.extra.fx.distort

import org.openrndr.draw.*
import org.openrndr.extra.fx.fx_perspective_plane
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.transform

@Description("Perspective plane")
class PerspectivePlane : Filter1to1(mppFilterShader(fx_perspective_plane, "perspective-plane")) {
    //    @DoubleParameter("camera x", -1.0, 1.0, order = 0)
    var cameraX: Double = 0.0
    //    @DoubleParameter("camera y", -1.0, 1.0, order = 1)
    var cameraY: Double = 0.0
    //    @DoubleParameter("camera z", -1.0, 1.0, order = 2)
    var cameraZ: Double = 1.0


    @DoubleParameter("plane x", -1.0, 1.0, order = 3)
    var planeX: Double = 0.0
    @DoubleParameter("plane y", -1.0, 1.0, order = 4)
    var planeY: Double = 0.0
    @DoubleParameter("plane z", -1.0, 1.0, order = 5)
    var planeZ: Double = 0.5

    @DoubleParameter("plane yaw", -180.0, 180.0, order = 6)
    var planeYaw: Double = 0.0
    @DoubleParameter("plane pitch", -180.0, 180.0, order = 7)
    var planePitch: Double = 0.0
    @DoubleParameter("plane roll", -180.0, 180.0, order = 8)
    var planeRoll: Double = 0.0


    @BooleanParameter("tile input")
    var tile: Boolean by parameters

    init {
        tile = false
    }

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>) {
        source[0].generateMipmaps()
        source[0].filter(MinifyingFilter.LINEAR_MIPMAP_LINEAR, MagnifyingFilter.LINEAR)
        source[0].wrapU = WrapMode.REPEAT
        source[0].wrapV = WrapMode.REPEAT
        parameters["cameraPosition"] = Vector3(cameraX, cameraY, cameraZ)
        parameters["planePosition"] = Vector3(planeX, planeY, planeZ)
        parameters["planeMatrix"] = transform {
            rotate(Vector3.UNIT_X, planePitch)
            rotate(Vector3.UNIT_Y, planeYaw)
            rotate(Vector3.UNIT_Z, planeRoll)
        }
        super.apply(source, target)
    }
}