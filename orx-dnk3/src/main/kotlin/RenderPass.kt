package org.openrndr.extra.dnk3

import org.openrndr.draw.BufferMultisample
import org.openrndr.draw.DepthFormat
import org.openrndr.draw.RenderTarget
import org.openrndr.draw.renderTarget

class RenderPass(val combiners: List<FacetCombiner>, val renderOpaque: Boolean = true, val renderTransparent: Boolean = false)

val DefaultPass = RenderPass(listOf(LDRColorFacet()))
val LightPass = RenderPass(emptyList())
val VSMLightPass = RenderPass(listOf(MomentsFacet()))

fun RenderPass.createPassTarget(width: Int, height: Int, depthFormat: DepthFormat = DepthFormat.DEPTH24, multisample: BufferMultisample = BufferMultisample.Disabled): RenderTarget {
    return renderTarget(width, height, multisample = multisample) {
        for (combiner in combiners) {
            when (combiner) {
                is ColorBufferFacetCombiner ->
                    colorBuffer(combiner.targetOutput, combiner.format, combiner.type)
            }
        }
        depthBuffer(depthFormat)
    }
}
