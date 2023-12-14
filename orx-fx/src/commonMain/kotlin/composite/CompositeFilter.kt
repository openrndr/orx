package org.openrndr.extra.fx.composite

import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Filter
import org.openrndr.draw.createEquivalent
import org.openrndr.draw.isEquivalentTo
import org.openrndr.shape.Rectangle

/**
 * @param first the filter that is applied first
 * @param second the filter that is applied second
 * @param firstSource a function that maps source color buffers for the [first] filter
 * @param secondSource a function that maps source color buffers for the [second] filter
 * @param firstParameters a function that sets parameters for the [first] filter
 * @param secondParameters a function that sets parameters for the [second] fillter
 * @param useIntermediateBuffer should an intermediate buffer be maintained? when set to false the [first] filter will
 * write to the target color buffer
 */
class CompositeFilter<F0 : Filter, F1 : Filter>(
    val first: F0,
    val second: F1,
    private val firstSource: (List<ColorBuffer>) -> List<ColorBuffer>,
    private val secondSource: (List<ColorBuffer>, ColorBuffer) -> List<ColorBuffer>,
    private val firstParameters: F0.() -> Unit,
    private val secondParameters: F1.() -> Unit,
    private val useIntermediateBuffer: Boolean = false
) : Filter() {
    private var intermediate: ColorBuffer? = null

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>, clip: Rectangle?) {
        val firstSource = firstSource(source.toList()).toTypedArray()
        if (!useIntermediateBuffer) {
            first.firstParameters()
            first.apply(firstSource, target, clip)

            second.secondParameters()
            val secondSource = secondSource(source.toList(), target.first()).toTypedArray()
            second.apply(secondSource, target, clip)
        } else {
            val li = intermediate
            if (li != null && !li.isEquivalentTo(target.first())) {
                li.destroy()
                intermediate = null
            }
            if (intermediate == null) {
                intermediate = target.first().createEquivalent()
            }
            first.firstParameters()
            first.apply(firstSource, arrayOf(intermediate!!), clip)
            val secondSource = secondSource(source.toList(), intermediate!!).toTypedArray()
            second.secondParameters()
            second.apply(secondSource, target, clip)
        }
    }

    override fun destroy() {
        intermediate?.destroy()
        super.destroy()
    }
}

class CompositeFilterBuilder<F0 : Filter, F1 : Filter>(val first: F0, val second: F1) {
    private var firstSourceFunction: (inputs: List<ColorBuffer>) -> List<ColorBuffer> = { inputs -> inputs }
    private var secondSourceFunction: (inputs: List<ColorBuffer>, intermediate: ColorBuffer) -> List<ColorBuffer> =
        { inputs, intermediate -> listOf(intermediate) + inputs.drop(1) }

    private var firstParametersFunction: (F0.() -> Unit) = {}
    private var secondParametersFunction: (F1.() -> Unit) = {}


    /** Supply the function that sets the source color buffers for the [first] filter */
    fun firstSource(function: (source: List<ColorBuffer>) -> List<ColorBuffer>) {
        firstSourceFunction = function
    }

    /** Supply the function that sets the source color buffers for the [second] filter */
    fun secondSource(function: (source: List<ColorBuffer>, intermediate: ColorBuffer) -> List<ColorBuffer>) {
        secondSourceFunction = function
    }

    /**
     * Supply the function that sets the filter parameters for the [first] filter
     */
    fun firstParameters(function: (F0.() -> Unit)) {
        firstParametersFunction = function
    }

    /**
     * Supply the function that sets the filter parameter the [second] filter
     */
    fun secondParameters(function: (F1.() -> Unit)) {
        secondParametersFunction = function
    }

    /**
     * Should an intermediate color buffer be used?
     */
    var useIntermediateBuffer = true

    fun build(): CompositeFilter<F0, F1> {
        return CompositeFilter(
            first,
            second,
            firstSourceFunction,
            secondSourceFunction,
            firstParametersFunction,
            secondParametersFunction,
            useIntermediateBuffer
        )
    }
}

/**
 * Create a composite filter that first applies [this] filter and then the [next] filter.
 */
fun <F0 : Filter, F1 : Filter> F0.then(
    next: F1,
    builder: CompositeFilterBuilder<F0, F1>.() -> Unit = {}
): CompositeFilter<F0, F1> {
    val compositeFilterBuilder = CompositeFilterBuilder(this, next)
    compositeFilterBuilder.builder()
    return compositeFilterBuilder.build()
}