package org.openrndr.extra.compositor

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.fx.blend.SourceIn
import org.openrndr.extra.fx.blend.SourceOut
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.Description
import kotlin.jvm.JvmRecord

fun RenderTarget.deepDestroy() {
    val cbcopy = colorAttachments.map { it }
    val dbcopy = depthBuffer
    detachDepthBuffer()
    detachColorAttachments()
    cbcopy.forEach {
        when (it) {
            is ColorBufferAttachment -> it.colorBuffer.destroy()
            is CubemapAttachment -> it.cubemap.destroy()
            is ArrayTextureAttachment -> it.arrayTexture.destroy()
            is ArrayCubemapAttachment -> it.arrayCubemap.destroy()
            else -> {}
        }
    }
    dbcopy?.destroy()
    destroy()
}

enum class LayerType {
    LAYER,
    ASIDE
}

private val sourceOut = persistent { SourceOut() }
private val sourceIn = persistent { SourceIn() }

/**
 * A single layer representation
 */
@Description("Layer")
open class Layer internal constructor(
    val type: LayerType,
    private val bufferMultisample: BufferMultisample = BufferMultisample.Disabled
) {
    var maskLayer: Layer? = null
    var drawFunc: () -> Unit = {}
    val children: MutableList<Layer> = mutableListOf()
    var blendFilter: Pair<Filter, Filter.() -> Unit>? = null
    val postFilters: MutableList<Triple<Filter, Array<out Layer>, Filter.() -> Unit>> = mutableListOf()
    var colorType = ColorType.UINT8_SRGB
    private var unresolvedAccumulation: ColorBuffer? = null
    var accumulation: ColorBuffer? = null

    @BooleanParameter("enabled")
    var enabled = true

    @BooleanParameter("Invert mask")
    var invertMask = false
    var clearColor: ColorRGBa? = ColorRGBa.TRANSPARENT
    private var layerTarget: RenderTarget? = null

    val result: ColorBuffer
        get() {
            return layerTarget?.colorBuffer(0) ?: error("layer result not ready")
        }

    /**
     * draw the layer
     */
    protected fun drawLayer(drawer: Drawer, cache: ColorBufferCache) {
        if (!enabled) {
            return
        }

        val activeRenderTarget = RenderTarget.active

        if (shouldCreateLayerTarget(activeRenderTarget)) {
            createLayerTarget(activeRenderTarget, drawer, bufferMultisample)
        }

        layerTarget?.let { target ->
            maskLayer?.let {
                if (it.shouldCreateLayerTarget(activeRenderTarget)) {
                    it.createLayerTarget(activeRenderTarget, drawer, it.bufferMultisample)
                }

                it.layerTarget?.let { maskRt ->
                    drawer.isolatedWithTarget(maskRt) {
                        drawer.fill = ColorRGBa.WHITE
                        drawer.stroke = ColorRGBa.WHITE
                        drawer.clear(ColorRGBa.TRANSPARENT)
                        it.drawFunc()
                    }
                }
            }

            drawer.isolatedWithTarget(target) {
                children.filter { it.type == LayerType.ASIDE }.forEach {
                    it.drawLayer(drawer, cache)
                }

                clearColor?.let {
                    drawer.clear(it)
                }
                drawFunc()
                children.filter { it.type == LayerType.LAYER }.forEach {
                    it.drawLayer(drawer, cache)
                }
            }

            val layerPost = if (postFilters.isEmpty()) target.colorBuffer(0) else postFilters.let { filters ->
                val targets = cache[ColorBufferCacheKey(colorType, target.contentScale)]
                targets.forEach {
                    it.fill(ColorRGBa.TRANSPARENT)
                }
                var localSource = target.colorBuffer(0)
                for ((i, filter) in filters.withIndex()) {
                    filter.first.apply(filter.third)
                    val sources =
                        arrayOf(localSource) + filter.second.map { it.result }
                            .toTypedArray()
                    filter.first.apply(sources, arrayOf(targets[i % targets.size]))
                    localSource = targets[i % targets.size]
                }
                targets[postFilters.lastIndex % targets.size]
            }

            maskLayer?.let {
                val maskFilter = if (invertMask) sourceOut else sourceIn
                maskFilter.apply(arrayOf(layerPost, it.layerTarget!!.colorBuffer(0)), layerPost)
            }

            if (type == LayerType.ASIDE) {
                if (postFilters.isNotEmpty()) {
                    require(layerPost != result)
                    layerPost.copyTo(result)
                }
            } else if (type == LayerType.LAYER) {
                val localBlendFilter = blendFilter
                if (localBlendFilter == null) {
                    drawer.isolated {
                        drawer.defaults()
                        if (bufferMultisample == BufferMultisample.Disabled) {
                            drawer.image(layerPost, layerPost.bounds, drawer.bounds)
                        } else {
                            layerPost.copyTo(accumulation!!)
                            drawer.image(accumulation!!, layerPost.bounds, drawer.bounds)
                        }
                    }
                } else {
                    localBlendFilter.first.apply(localBlendFilter.second)
                    activeRenderTarget.colorBuffer(0).copyTo(unresolvedAccumulation!!)
                    if (bufferMultisample == BufferMultisample.Disabled) {
                        localBlendFilter.first.apply(
                            arrayOf(unresolvedAccumulation!!, layerPost),
                            unresolvedAccumulation!!
                        )
                    } else {
                        layerPost.copyTo(accumulation!!)
                        localBlendFilter.first.apply(
                            arrayOf(unresolvedAccumulation!!, accumulation!!),
                            unresolvedAccumulation!!
                        )
                    }

                    if (activeRenderTarget !is ProgramRenderTarget) {
                        unresolvedAccumulation!!.copyTo(target.colorBuffer(0))
                    }
                    unresolvedAccumulation!!.copyTo(activeRenderTarget.colorBuffer(0))
                }
            }
        }
    }

    private fun shouldCreateLayerTarget(activeRenderTarget: RenderTarget): Boolean {
        return layerTarget == null
                || ((layerTarget?.width != activeRenderTarget.width || layerTarget?.height != activeRenderTarget.height)
                && activeRenderTarget.width > 0 && activeRenderTarget.height > 0)
    }

    private fun createLayerTarget(
        activeRenderTarget: RenderTarget, drawer: Drawer, bufferMultisample: BufferMultisample
    ) {
        layerTarget?.deepDestroy()
        layerTarget = renderTarget(
            activeRenderTarget.width, activeRenderTarget.height,
            activeRenderTarget.contentScale, bufferMultisample
        ) {
            colorBuffer(type = colorType)
            depthBuffer()
        }
        if (bufferMultisample != BufferMultisample.Disabled) {
            accumulation?.destroy()
            accumulation = colorBuffer(
                activeRenderTarget.width, activeRenderTarget.height,
                activeRenderTarget.contentScale, type = colorType
            )
        }
        unresolvedAccumulation?.destroy()
        unresolvedAccumulation = colorBuffer(
            activeRenderTarget.width, activeRenderTarget.height,
            activeRenderTarget.contentScale, type = colorType
        )
        layerTarget?.let {
            drawer.withTarget(it) {
                drawer.clear(ColorRGBa.TRANSPARENT)
            }
        }
    }

    fun Drawer.image(layer: Layer) {
        val cb = layer.result
        image(cb)
    }
}

/**
 * create a layer within the composition
 */
fun Layer.layer(
    colorType: ColorType = this.colorType,
    multisample: BufferMultisample = BufferMultisample.Disabled,
    function: Layer.() -> Unit
): Layer {
    val layer = Layer(LayerType.LAYER, multisample).apply { function() }
    layer.colorType = colorType
    children.add(layer)
    return layer
}

fun Layer.aside(
    colorType: ColorType = this.colorType,
    multisample: BufferMultisample = BufferMultisample.Disabled,
    function: Layer.() -> Unit
): Layer {
    val layer = Layer(LayerType.ASIDE, multisample).apply { function() }
    layer.colorType = colorType
    children.add(layer)
    return layer
}

fun <T : Filter1to1> Layer.apply(drawer: Drawer,
    filter: T, source: Layer, colorType: ColorType = this.colorType,
    function: T.() -> Unit
): Layer  {
    val layer = Layer(LayerType.ASIDE)
    layer.colorType = colorType
    layer.draw {
        drawer.image(source.result)
    }
    layer.post(filter, function)
    children.add(layer)
    return layer
}

fun <T : Filter2to1> Layer.apply(drawer: Drawer,
    filter: T, source0: Layer,  source1:Layer, colorType: ColorType = this.colorType,
    function: T.() -> Unit
): Layer  {
    val layer = Layer(LayerType.ASIDE)
    layer.colorType = colorType
    layer.draw {
        drawer.image(source0.result)
    }
    layer.post(filter, source1, function)
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
 * the drawing acts as a mask on the layer
 */
fun Layer.mask(function: () -> Unit) {
    maskLayer = Layer(LayerType.LAYER).apply {
        this.drawFunc = function
    }
}

/**
 * add a post-processing filter to the layer
 */
fun <F : Filter1to1> Layer.post(filter: F, configure: F.() -> Unit = {}): F {
    @Suppress("UNCHECKED_CAST")
    postFilters.add(Triple(filter as Filter, emptyArray(), configure as Filter.() -> Unit))
    return filter
}

fun <F : Filter2to1> Layer.post(filter: F, input1: Layer, configure: F.() -> Unit = {}): F {
    require(input1.type == LayerType.ASIDE)
    @Suppress("UNCHECKED_CAST")
    postFilters.add(Triple(filter as Filter, arrayOf(input1), configure as Filter.() -> Unit))
    return filter
}

fun <F : Filter3to1> Layer.post(filter: F, input1: Layer, input2: Layer, configure: F.() -> Unit = {}): F {
    require(input1.type == LayerType.ASIDE)
    require(input2.type == LayerType.ASIDE)
    @Suppress("UNCHECKED_CAST")
    postFilters.add(Triple(filter as Filter, arrayOf(input1, input2), configure as Filter.() -> Unit))
    return filter
}


/**
 * add a blend filter to the layer
 */
fun <F : Filter2to1> Layer.blend(filter: F, configure: F.() -> Unit = {}): F {
    @Suppress("UNCHECKED_CAST")
    blendFilter = Pair(filter as Filter, configure as Filter.() -> Unit)
    return filter
}

@JvmRecord
data class ColorBufferCacheKey(
    val colorType: ColorType,
    val contentScale: Double
)

class ColorBufferCache(val width: Int, val height: Int) {
    val cache = mutableMapOf<ColorBufferCacheKey, List<ColorBuffer>>()

    operator fun get(key: ColorBufferCacheKey): List<ColorBuffer> {
        return cache.getOrPut(key) {
            listOf(
                colorBuffer(width, height, type = key.colorType, contentScale = key.contentScale),
                colorBuffer(width, height, type = key.colorType, contentScale = key.contentScale),
            )
        }
    }

    fun destroy() {
        cache.forEach {
            it.value.forEach { cb -> cb.destroy() }
        }
    }
}

class Composite : Layer(LayerType.LAYER) {

    private var cache = ColorBufferCache(RenderTarget.active.width, RenderTarget.active.height)
    fun draw(drawer: Drawer) {

        if (cache.width != RenderTarget.active.width || cache.height != RenderTarget.active.height) {
            cache.destroy()
            cache = ColorBufferCache(RenderTarget.active.width, RenderTarget.active.height)
        }

        drawLayer(drawer, cache)
    }
}

/**
 * create a layered composition
 */
fun compose(function: Layer.() -> Unit): Composite {
    val root = Composite()
    root.function()
    return root
}

class Compositor : Extension {
    override var enabled: Boolean = true
    var composite = Composite()

    override fun afterDraw(drawer: Drawer, program: Program) {
        drawer.isolated {
            drawer.defaults()
            composite.draw(drawer)
        }
    }
}
