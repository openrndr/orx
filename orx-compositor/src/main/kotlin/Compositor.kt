package org.openrndr.extra.compositor

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.Matrix44

/**
 * A single layer representation
 */
class Layer internal constructor() {
    var drawFunc: () -> Unit = {}
    val children: MutableList<Layer> = mutableListOf()
    var blendFilter: Pair<Filter, Filter.() -> Unit>? = null
    val postFilters: MutableList<Pair<Filter, Filter.() -> Unit>> = mutableListOf()

    /**
     * draw the layer
     */
    fun draw(drawer: Drawer) {
        val rt = RenderTarget.active
        val layerTarget = renderTarget(rt.width, rt.height) {
            colorBuffer()
            depthBuffer()
        }

        drawer.isolatedWithTarget(layerTarget) {
            drawer.background(ColorRGBa.TRANSPARENT)
            children.reversed().forEach {
                it.draw(drawer)
            }
            drawFunc()
        }

        val (tmpTargets, layerPost) = postFilters.let { filters ->
            val targets = List(Math.min(filters.size, 2)) {
                colorBuffer(rt.width, rt.height)
            }
            val result = filters.foldIndexed(layerTarget.colorBuffer(0)) { i, source, filter ->
                val target = targets[i % targets.size]
                filter.first.apply(filter.second)
                filter.first.apply(source, target)
                target
            }
            Pair(targets, result)
        }

        val lblend = blendFilter
        if (lblend == null) {
            drawer.isolatedWithTarget(rt) {
                drawer.ortho(rt)
                drawer.view = Matrix44.IDENTITY
                drawer.model = Matrix44.IDENTITY
                drawer.image(layerPost, layerPost.bounds, drawer.bounds)
            }
        } else {
            lblend.first.apply(lblend.second)
            lblend.first.apply(arrayOf(rt.colorBuffer(0), layerPost), rt.colorBuffer(0))
        }

        tmpTargets.forEach {
            it.destroy()
        }

        layerTarget.colorBuffer(0).destroy()
        layerTarget.depthBuffer?.destroy()
        layerTarget.detachColorBuffers()
        layerTarget.detachDepthBuffer()
        layerTarget.destroy()
    }
}

/**
 * create a layer within the composition
 */
fun Layer.layer(function: Layer.() -> Unit) {
    children.add(Layer().apply { function() })
}

/**
 * set the draw contents of the layer
 */
fun Layer.draw(function: () -> Unit) {
    drawFunc = function
}

/**
 * add a post-processing filter to the layer
 */
fun <F : Filter> Layer.post(filter: F, configure: F.() -> Unit = {}) {
    postFilters.add(Pair(filter as Filter, configure as Filter.() -> Unit))
}

/**
 * add a blend filter to the layer
 */
fun <F : Filter> Layer.blend(filter: F, configure: F.() -> Unit = {}) {
    blendFilter = Pair(filter as Filter, configure as Filter.() -> Unit)
}

/**
 * create a layered composition
 */
fun compose(function: Layer.() -> Unit): Layer {
    val root = Layer()
    root.function()
    return root
}