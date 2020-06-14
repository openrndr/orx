package org.openrndr.extra.dnk3.renderers

import org.openrndr.draw.BufferMultisample
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.extra.dnk3.*
import org.openrndr.extra.dnk3.post.SegmentContours
import org.openrndr.extra.dnk3.post.SegmentContoursMSAA8

fun segmentContourRenderer(multisample: BufferMultisample = BufferMultisample.Disabled): SceneRenderer {
    val sr = SceneRenderer()
    sr.outputPasses.clear()
    sr.outputPasses.add(
            RenderPass(
                    listOf(FragmentIDFacet()),
                    multisample = multisample
            )
    )
    sr.postSteps.add(
            FilterPostStep(1.0,
                    when (multisample) {
                        BufferMultisample.Disabled -> SegmentContours()
                        BufferMultisample.SampleCount(8) -> SegmentContoursMSAA8()
                        else -> error("unsupported multisampling mode $multisample")
                    },
                    listOf("fragmentID"),
                    "segments",
                    ColorFormat.RGB,
                    ColorType.UINT8
            )
    )
    sr.drawFinalBuffer = true
    return sr
}