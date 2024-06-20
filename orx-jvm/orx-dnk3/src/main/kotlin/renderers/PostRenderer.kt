package org.openrndr.extra.dnk3.renderers

import org.openrndr.draw.BufferMultisample
import org.openrndr.extra.dnk3.*

fun postRenderer(multisample: BufferMultisample = BufferMultisample.Disabled): SceneRenderer {
    val sr = SceneRenderer()
    sr.outputPasses.clear()
    sr.outputPasses.add(
            RenderPass(
                    listOf(HDRColorFacet(),FragmentIDFacet(), ClipDepthFacet(), ViewNormalFacet()),
                    multisample = multisample
            )
    )

    sr.drawFinalBuffer = true
    return sr
}