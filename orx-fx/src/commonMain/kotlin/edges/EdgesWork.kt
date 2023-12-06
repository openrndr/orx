@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.fx.edges

import org.openrndr.draw.*
import org.openrndr.extra.fx.ColorBufferDescription
import org.openrndr.extra.fx.fx_edges_work_1
import org.openrndr.extra.fx.fx_edges_work_2
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.math.Vector2

internal class EdgesWork1 : Filter(mppFilterShader(fx_edges_work_1, "edges-work-1")) {
    var delta: Vector2 by parameters

    init {
        delta = Vector2.ZERO
    }
}

@Description("Edges Work")
open class EdgesWork : Filter1to1(mppFilterShader(fx_edges_work_2, "edges-work-2")) {
    /**
     * radius, default value is 1.0
     */
    @IntParameter("radius", 1, 400)
    var radius: Int by parameters

    private var delta: Vector2 by parameters

    private val work1 = EdgesWork1()

    private var intermediateCache = mutableMapOf<ColorBufferDescription, ColorBuffer>()

    init {
        radius = 1
        delta = Vector2.ZERO
    }

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>) {
        val intermediateDescription = ColorBufferDescription(
            target[0].width,
            target[0].height,
            target[0].contentScale,
            target[0].format,
            target[0].type
        )
        val intermediate = intermediateCache.getOrPut(intermediateDescription) {
            colorBuffer(target[0].width, target[0].height, target[0].contentScale, target[0].format, target[0].type)
        }

        intermediate.let {
            work1.delta = Vector2(radius / it.effectiveWidth.toDouble(), 0.0)
            work1.apply(source, arrayOf(it))

            parameters["delta"] = Vector2(0.0, radius / it.effectiveHeight.toDouble())
            super.apply(arrayOf(it), target)
        }
    }
}

