
package org.openrndr.extra.fluidsim

import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter

@Description("Fluid solver params")
class FluidParams {
    @DoubleParameter("vorticity", 0.0, 1.0)
    var vorticity = 0.0

    @DoubleParameter("velocity dissipation", 0.0, 1.0)
    var velocityDissipation = 1.0

    @DoubleParameter("density dissipation", 0.0, 1.0)
    var densityDissipation = 1.0

    @IntParameter("jacobi projection iterations", 0, 100)
    var jacobiIterations = 40

    @DoubleParameter("gradient scale", 0.0, 1.0)
    var gradientScale = 1.0

    @DoubleParameter("timestemp", 0.0, 1.0)
    var timestep = 0.125

    var gridScale = 1.0
}