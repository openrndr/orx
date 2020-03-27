package org.openrndr.extra.fx.color

import org.openrndr.draw.*
import org.openrndr.extra.fx.filterFragmentUrl

class ColorLookup(lookup: ColorBuffer) : Filter(filterShaderFromUrl(filterFragmentUrl("color/color-lookup.frag"))) {
    /** a color look-up texture */
    var lookup: ColorBuffer by parameters

    /**
     * noise gain in look-up, default value is 0.0
     */
    var noiseGain: Double by parameters

    /**
     * noise seed, default value is 0.0
     */
    var seed: Double by parameters

    init {
        this.lookup = lookup
        this.noiseGain = 0.0
        this.seed = 0.0
    }

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>) {
        lookup.filter(MinifyingFilter.LINEAR, MagnifyingFilter.LINEAR)
        super.apply(source, target)
    }
}