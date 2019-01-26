package org.openrndr.extra.compositor

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.Matrix44


private val postBufferCache = mutableListOf<ColorBuffer>()

fun RenderTarget.deepDestroy() {
    val cbcopy = colorBuffers.map { it}
    val dbcopy = depthBuffer
    detachDepthBuffer()
    detachColorBuffers()
    cbcopy.forEach {
        it.destroy()
    }
    dbcopy?.destroy()
    destroy()
}

/**
 * A single layer representation
 */
class Layer internal constructor() {
    var drawFunc: () -> Unit = {}
    val children: MutableList<Layer> = mutableListOf()
    var blendFilter: Pair<Filter, Filter.() -> Unit>? = null
    val postFilters: MutableList<Pair<Filter, Filter.() -> Unit>> = mutableListOf()

    private var layerTarget:RenderTarget? = null

    /**
     * draw the layer
     */
    fun draw(drawer: Drawer) {
        val rt = RenderTarget.active

        val llt = layerTarget
        if (llt == null || (llt.width != rt.width || llt.height != rt.height)) {
            layerTarget?.deepDestroy()
            layerTarget = renderTarget(rt.width, rt.height) {
                colorBuffer()
                depthBuffer()
            }
        }

        layerTarget?.let { target ->
            drawer.isolatedWithTarget(target) {
                drawer.background(ColorRGBa.TRANSPARENT)
                drawFunc()
                children.forEach {
                    it.draw(drawer)
                }
            }

            if (postFilters.size > 0) {
                val sizeMismatch = if (postBufferCache.isNotEmpty()) {
                    postBufferCache[0].width != rt.width || postBufferCache[0].height != rt.height
                } else {
                    false
                }

                if (sizeMismatch) {
                    postBufferCache.forEach { it.destroy() }
                    postBufferCache.clear()
                }

                if (postBufferCache.isEmpty()) {
                    postBufferCache += colorBuffer(rt.width, rt.height)
                    postBufferCache += colorBuffer(rt.width, rt.height)
                }
            }

            val layerPost = postFilters.let { filters ->
                val targets = postBufferCache
                val result = filters.foldIndexed(target.colorBuffer(0)) { i, source, filter ->
                    val target = targets[i % targets.size]
                    filter.first.apply(filter.second)
                    filter.first.apply(source, target)
                    target
                }
                result
            }

            val lblend = blendFilter
            if (lblend == null) {
                drawer.isolatedWithTarget(rt) {
                    //drawer.ortho(rt)
                    drawer.ortho()
                    drawer.view = Matrix44.IDENTITY
                    drawer.model = Matrix44.IDENTITY
                    drawer.image(layerPost, layerPost.bounds, drawer.bounds)
                }
            } else {
                lblend.first.apply(lblend.second)
                lblend.first.apply(arrayOf(rt.colorBuffer(0), layerPost), rt.colorBuffer(0))
            }
        }
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