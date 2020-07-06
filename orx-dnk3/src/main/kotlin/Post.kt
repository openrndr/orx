package org.openrndr.extra.dnk3

import org.openrndr.draw.*
import org.openrndr.math.Matrix44

data class PostContext(val lightContext: LightContext, val inverseViewMatrix: Matrix44)

interface PostStep {
    fun apply(buffers: MutableMap<String, ColorBuffer>, postContext: PostContext)
}

class FilterPostStep<T:Filter>(val outputScale: Double,
                     val filter: T,
                     val inputs: List<String>,
                     val output: String,
                     val outputFormat: ColorFormat,
                     val outputType: ColorType,
                     val update: (T.(PostContext) -> Unit)? = null) : PostStep {

    override fun apply(buffers: MutableMap<String, ColorBuffer>, postContext: PostContext) {
        val inputBuffers = inputs.map { buffers[it]?: error("buffer not found: $it") }
        val outputBuffer = buffers.getOrPut(output) {
            colorBuffer((inputBuffers[0].width * outputScale).toInt(),
                    (inputBuffers[0].height * outputScale).toInt(),
                    format = outputFormat,
                    type = outputType)
        }
        update?.invoke(filter, postContext)
        filter.apply(inputBuffers.toTypedArray(), outputBuffer)
    }
}

class FunctionPostStep(val function:(MutableMap<String, ColorBuffer>)->Unit) : PostStep {
    override fun apply(buffers: MutableMap<String, ColorBuffer>, postContext: PostContext) {
        function(buffers)
    }
}

class FilterPostStepBuilder<T : Filter>(val filter: T) {
    var outputScale = 1.0
    val inputs = mutableListOf<String>()
    var output = "untitled"
    var outputFormat = ColorFormat.RGBa
    var outputType = ColorType.UINT8
    var update: (T.(PostContext) -> Unit)? = null

    internal fun build(): PostStep {
        @Suppress("UNCHECKED_CAST", "PackageDirectoryMismatch")
        return FilterPostStep(outputScale, filter, inputs, output, outputFormat, outputType, update as (Filter.(PostContext) -> Unit)?)
    }
}

fun <T : Filter> postStep(filter: T, configure: FilterPostStepBuilder<T>.() -> Unit) : PostStep {
    val psb = FilterPostStepBuilder(filter)
    psb.configure()
    return psb.build()
}

fun postStep(function: (MutableMap<String, ColorBuffer>)->Unit) : PostStep {
    return FunctionPostStep(function)
}
