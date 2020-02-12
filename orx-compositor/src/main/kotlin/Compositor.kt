package org.openrndr.extra.compositor

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.Description
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
@Description("Layer")
class Layer internal constructor() {
    var drawFunc: () -> Unit = {}
    val children: MutableList<Layer> = mutableListOf()
    var blendFilter: Pair<Filter, Filter.() -> Unit>? = null
    val postFilters: MutableList<Pair<Filter, Filter.() -> Unit>> = mutableListOf()

    @BooleanParameter("enabled")
    var enabled = true
    var clearColor: ColorRGBa? = ColorRGBa.TRANSPARENT
    private var layerTarget:RenderTarget? = null

    /**
     * draw the layer
     */
    fun draw(drawer: Drawer) {

        if (!enabled) {
            return
        }

        val rt = RenderTarget.active

        val llt = layerTarget
        if (llt == null || (llt.width != rt.width || llt.height != rt.height)) {
            layerTarget?.deepDestroy()
            layerTarget = renderTarget(rt.width, rt.height) {
                colorBuffer()
                depthBuffer()
            }
            layerTarget?.let {
                drawer.withTarget(it) {
                    drawer.background(ColorRGBa.TRANSPARENT)
                }
            }
        }

        layerTarget?.let { target ->
            drawer.isolatedWithTarget(target) {
                clearColor?.let {
                    drawer.background(it)
                }
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
                    postBufferCache += colorBuffer(rt.width, rt.height).apply {
                        Session.active.untrack(this)
                    }
                    postBufferCache += colorBuffer(rt.width, rt.height).apply {
                        Session.active.untrack(this)
                    }
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
fun Layer.layer(function: Layer.() -> Unit) : Layer {
    val layer = Layer().apply { function() }
    children.add(layer)
    return layer
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
fun <F : Filter> Layer.post(filter: F, configure: F.() -> Unit = {}) : F {
    postFilters.add(Pair(filter as Filter, configure as Filter.() -> Unit))
    return filter
}

/**
 * add a blend filter to the layer
 */
fun <F : Filter> Layer.blend(filter: F, configure: F.() -> Unit = {}) : F {
    blendFilter = Pair(filter as Filter, configure as Filter.() -> Unit)
    return filter
}

/**
 * create a layered composition
 */
fun compose(function: Layer.() -> Unit): Layer {
    val root = Layer()
    root.function()
    return root
}

class Compositor: Extension {
    override var enabled: Boolean = true
    var composite = Layer()

    override fun afterDraw(drawer: Drawer, program: Program) {
        drawer.isolated {
            drawer.defaults()
            composite.draw(drawer)
        }
    }
}