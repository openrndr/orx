package org.openrndr.extra.dnk3

import org.openrndr.draw.BufferMultisample
import org.openrndr.draw.DepthFormat
import org.openrndr.draw.RenderTarget
import org.openrndr.draw.renderTarget

class RenderPass(val combiners: List<FacetCombiner>,
                 val renderOpaque: Boolean = true,
                 val renderTransparent: Boolean = false,
                 val depthWrite: Boolean = true

)

val DefaultPass = RenderPass(listOf(LDRColorFacet()))
val DefaultOpaquePass = RenderPass(listOf(LDRColorFacet()), renderOpaque = true, renderTransparent = false)
val DefaultTransparentPass = RenderPass(listOf(LDRColorFacet()), renderOpaque = false, renderTransparent = true, depthWrite = false)
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
