@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.fx.blur

import org.openrndr.draw.Filter
import org.openrndr.draw.Filter1to1
import org.openrndr.draw.Filter2to1
import org.openrndr.draw.Filter3to1
import org.openrndr.extra.fx.fx_directional_hash_blur
import org.openrndr.extra.fx.fx_hash_blur
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter

@Description("Hash blur")
class HashBlur : Filter1to1(mppFilterShader(fx_hash_blur, "hash-blur")) {
    private var dynamic: Boolean by parameters


    /**
     * Blur radius in pixels, default is 5.0
     */
    @DoubleParameter("blur radius", 1.0, 25.0)
    var radius: Double by parameters

    /**
     * Time/seed, this should be fed with seconds, default is 0.0
     */
    var time: Double by parameters

    /**
     * Number of samples, default is 30
     */
    @IntParameter("number of samples", 1, 100)
    var samples: Int by parameters

    /**
     * Post-blur gain, default is 1.0
     */
    @DoubleParameter("image gain", 0.0, 2.0)
    var gain: Double by parameters

    init {
        dynamic = false
        radius = 5.0
        time = 0.0
        samples = 30
        gain = 1.0
    }
}

@Description("Hash blur")
class HashBlurDynamic: Filter2to1(mppFilterShader(fx_hash_blur, "hash-blur")) {

    private var dynamic: Boolean by parameters

    /**
     * Blur radius in pixels, default is 5.0
     */
    @DoubleParameter("blur radius", 1.0, 25.0)
    var radius: Double by parameters

    /**
     * Time/seed, this should be fed with seconds, default is 0.0
     */
    var time: Double by parameters

    /**
     * Number of samples, default is 30
     */
    @IntParameter("number of samples", 1, 100)
    var samples: Int by parameters

    /**
     * Post-blur gain, default is 1.0
     */
    @DoubleParameter("image gain", 0.0, 2.0)
    var gain: Double by parameters

    init {
        dynamic = true
        radius = 5.0
        time = 0.0
        samples = 30
        gain = 1.0
    }
}

@Description("Directional hash blur")
class DirectionalHashBlur : Filter2to1(mppFilterShader(fx_directional_hash_blur, "directional-hash-blur")) {

    /**
     * Blur radius in pixels, default is 5.0
     */
    @DoubleParameter("blur radius", 0.0, 25.0)
    var radius: Double by parameters

    @DoubleParameter("blur spread", 0.0, 25.0)
    var spread: Double by parameters


    /**
     * Time/seed, this should be fed with seconds, default is 0.0
     */
    var time: Double by parameters

    /**
     * Number of samples, default is 30
     */
    @IntParameter("number of samples", 1, 100)
    var samples: Int by parameters

    /**
     * Post-blur gain, default is 1.0
     */
    @DoubleParameter("image gain", 0.0, 2.0)
    var gain: Double by parameters

    init {
        radius = 5.0
        spread = 25.0
        time = 0.0
        samples = 30
        gain = 1.0
    }
}

@Description("Directional hash blur")
class DirectionalHashBlurDynamic : Filter3to1(mppFilterShader("#define RADIUS_FROM_TEXTURE\n${fx_directional_hash_blur}", "directional-hash-blur")) {

    /**
     * Blur radius in pixels, default is 5.0
     */
    @DoubleParameter("blur radius", 0.0, 25.0)
    var radius: Double by parameters

    @DoubleParameter("blur spread", 0.0, 25.0)
    var spread: Double by parameters


    /**
     * Time/seed, this should be fed with seconds, default is 0.0
     */
    var time: Double by parameters

    /**
     * Number of samples, default is 30
     */
    @IntParameter("number of samples", 1, 100)
    var samples: Int by parameters

    /**
     * Post-blur gain, default is 1.0
     */
    @DoubleParameter("image gain", 0.0, 2.0)
    var gain: Double by parameters

    init {
        radius = 5.0
        spread = 25.0
        time = 0.0
        samples = 30
        gain = 1.0
    }
}
