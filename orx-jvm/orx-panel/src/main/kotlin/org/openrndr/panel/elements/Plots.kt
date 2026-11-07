package org.openrndr.panel.elements

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.math.Vector2
import org.openrndr.panel.style.*
import org.openrndr.shape.Rectangle
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

private val seriesColors = listOf("#5ab0ff", "#f88379", "#7fd1b9", "#ffe08a", "#c49bff", "#ff9f5a").map { ColorRGBa.fromHex(it) }

/** a series of a [Plot]: the last [capacity] values that were added, in [color] */
class PlotSeries internal constructor(val name: String, val capacity: Int, var color: ColorRGBa, private val plot: Plot) {
    private val values = DoubleArray(capacity)
    private var start = 0

    /** the number of values */
    var size = 0
        private set

    /** adds [value], the oldest value goes when there are [capacity] values */
    fun add(value: Double) {
        if (size < capacity) {
            values[(start + size) % capacity] = value
            size++
        } else {
            values[start] = value
            start = (start + 1) % capacity
        }
        plot.requestRedraw()
    }

    /** the value [i] from the oldest */
    operator fun get(i: Int): Double = values[(start + i) % capacity]

    fun clear() {
        size = 0
        start = 0
        plot.requestRedraw()
    }

    internal var source: (() -> Double)? = null
}

/** a step between the lines of a grid that has about [count] lines over [span]: 1, 2 or 5 times a power of ten */
internal fun niceStep(span: Double, count: Int): Double {
    if (span <= 0.0 || span.isNaN()) return 1.0
    val raw = span / count
    val power = 10.0.pow(floor(log10(raw)))
    return listOf(1.0, 2.0, 5.0, 10.0).map { it * power }.first { it >= raw }
}

private fun label(v: Double, step: Double): String {
    val decimals = maxOf(0, -floor(log10(step)).toInt())
    return String.format("%.${decimals}f", v).let { if (it == "-0") "0" else it }
}

/**
 * Line graphs of series of values over time, like frame times or levels: [series] adds a series that values are
 * [PlotSeries.add]ed to, [watch] one that takes a value every frame. The values fit [range], or the range of the values
 * when that is null, with lines of a grid and their values at the left; the names of the series are at the top.
 */
class Plot : Element(ElementType("plot")) {
    val series = mutableListOf<PlotSeries>()

    /** the range of the values, the range of the values in the plot when null */
    var range: ClosedRange<Double>? = null

    /** the number of values of new series */
    var capacity = 300

    /** adds a series [name], in [color] */
    fun series(name: String, color: ColorRGBa = seriesColors[series.size % seriesColors.size], capacity: Int = this.capacity): PlotSeries =
        PlotSeries(name, capacity, color, this).also { series.add(it) }

    /** adds a series [name] that takes the value of [value] every frame */
    fun watch(name: String, color: ColorRGBa = seriesColors[series.size % seriesColors.size], value: () -> Double): PlotSeries =
        series(name, color).also { it.source = value }

    override fun prepare() {
        for (s in series) s.source?.let { s.add(it()) }
    }

    /** the range the values are drawn in */
    fun shownRange(): ClosedRange<Double> {
        range?.let { return it }
        var lo = Double.POSITIVE_INFINITY
        var hi = Double.NEGATIVE_INFINITY
        for (s in series) for (i in 0 until s.size) {
            val v = s[i]
            if (v.isFinite()) {
                lo = minOf(lo, v); hi = maxOf(hi, v)
            }
        }
        if (lo > hi) return 0.0..1.0
        if (hi - lo < 1E-9) return (lo - 0.5)..(hi + 0.5)
        // the range is rounded to the grid
        val step = niceStep(hi - lo, 4)
        return floor(lo / step) * step..ceil(hi / step) * step
    }

    override fun draw(drawer: Drawer) {
        val color = computedStyle.effectiveColor ?: ColorRGBa.WHITE
        val text = panelText ?: return
        val face = text.face(computedStyle)
        val w = layout.screenWidth
        val h = layout.screenHeight
        val legend = face.ascent - face.descent + 8.0
        val r = shownRange()
        val span = r.endInclusive - r.start
        val step = niceStep(span, 4)
        val labels = generateSequence(ceil(r.start / step) * step) { it + step }.takeWhile { it <= r.endInclusive + step * 1E-6 }.toList()
        val labelWidth = labels.maxOfOrNull { text.width(computedStyle, label(it, step)) } ?: 0.0
        val area = Rectangle(labelWidth + 10.0, legend, w - labelWidth - 14.0, h - legend - 6.0)
        fun y(v: Double) = area.y + area.height - (v - r.start) / span * area.height
        // the grid and its values
        drawer.strokeWeight = 1.0
        for (v in labels) {
            drawer.stroke = color.opacify(if (abs(v) < step * 1E-6) 0.3 else 0.12)
            drawer.lineSegment(area.x, y(v), area.x + area.width, y(v))
            text.drawInBox(drawer, this, "grid $v", computedStyle, label(v, step), Rectangle(0.0, y(v) - 10.0, labelWidth + 4.0, 20.0), 1.0, 0.5, color.opacify(0.55))
        }
        // the series, the newest values at the right
        drawer.fill = null
        drawer.strokeWeight = 1.5
        var x = area.x
        for (s in series) {
            if (s.size >= 2) {
                drawer.stroke = s.color
                val dx = area.width / (s.capacity - 1)
                val offset = area.x + area.width - (s.size - 1) * dx
                drawer.lineStrip(List(s.size) { i -> Vector2(offset + i * dx, y(s[i].coerceIn(r.start, r.endInclusive))) })
            }
            // the name and the last value
            val name = if (s.size > 0) "${s.name} ${label(s[s.size - 1], step / 10.0)}" else s.name
            drawer.stroke = null
            drawer.fill = s.color
            drawer.rectangle(x, legend / 2.0 - 4.0, 8.0, 8.0)
            text.drawLine(drawer, this, "legend ${s.name}", computedStyle, name, x + 12.0, legend / 2.0 + (face.ascent + face.descent) / 2.0, color.opacify(0.8))
            x += text.width(computedStyle, name) + 26.0
        }
    }
}

/**
 * The distribution of [samples] in [bins] between the ends of [range], or of the samples when that is null, as bars
 */
class Histogram : Element(ElementType("histogram")) {
    var samples: DoubleArray = DoubleArray(0)
        set(value) {
            field = value
            requestRedraw()
        }

    var bins = 32
        set(value) {
            field = value.coerceAtLeast(1)
            requestRedraw()
        }

    var range: ClosedRange<Double>? = null

    /** the color of the bars, the color of the style when null */
    var barColor: ColorRGBa? = null

    /** the numbers of samples in the bins */
    fun counts(): IntArray {
        val counts = IntArray(bins)
        val r = range ?: (samples.minOrNull() ?: 0.0)..(samples.maxOrNull() ?: 1.0)
        val span = (r.endInclusive - r.start).takeIf { it > 0.0 } ?: 1.0
        for (v in samples) {
            if (v < r.start || v > r.endInclusive) continue
            counts[((v - r.start) / span * bins).toInt().coerceAtMost(bins - 1)]++
        }
        return counts
    }

    override fun draw(drawer: Drawer) {
        val color = barColor ?: computedStyle.effectiveColor ?: ColorRGBa.WHITE
        val counts = counts()
        val most = counts.maxOrNull()?.takeIf { it > 0 } ?: return
        val w = layout.screenWidth / bins
        val h = layout.screenHeight
        drawer.stroke = null
        drawer.fill = color.opacify(0.8)
        drawer.rectangles(counts.indices.filter { counts[it] > 0 }.map { i ->
            val bh = counts[i].toDouble() / most * (h - 2.0)
            Rectangle(i * w + 0.5, h - bh, maxOf(1.0, w - 1.0), bh)
        })
    }
}

/** a waveform of [samples] between -1 and 1, or [range], across the element, like of a sound */
class Scope : Element(ElementType("scope")) {
    var samples: FloatArray = FloatArray(0)
        set(value) {
            field = value
            requestRedraw()
        }

    var range: ClosedRange<Double> = -1.0..1.0

    /** the color of the line, the color of the style when null */
    var lineColor: ColorRGBa? = null

    override fun draw(drawer: Drawer) {
        val color = lineColor ?: computedStyle.effectiveColor ?: ColorRGBa.WHITE
        val w = layout.screenWidth
        val h = layout.screenHeight
        val span = range.endInclusive - range.start
        fun y(v: Double) = h - (v - range.start) / span * h
        drawer.stroke = color.opacify(0.15)
        drawer.strokeWeight = 1.0
        drawer.lineSegment(0.0, y(0.0), w, y(0.0))
        val n = samples.size
        if (n < 2) return
        drawer.stroke = color
        drawer.strokeWeight = 1.5
        drawer.fill = null
        // more samples than pixels are drawn as the lowest and highest of each pixel
        if (n > w * 2) {
            val columns = w.toInt().coerceAtLeast(1)
            drawer.lineSegments((0 until columns).flatMap { c ->
                val from = c * n / columns
                val to = maxOf(from + 1, (c + 1) * n / columns)
                var lo = Float.POSITIVE_INFINITY
                var hi = Float.NEGATIVE_INFINITY
                for (i in from until to) { lo = minOf(lo, samples[i]); hi = maxOf(hi, samples[i]) }
                listOf(Vector2(c + 0.5, y(lo.toDouble())), Vector2(c + 0.5, y(hi.toDouble()) - 0.5))
            })
        } else {
            drawer.lineStrip(List(n) { i -> Vector2(i * w / (n - 1), y(samples[i].toDouble())) })
        }
    }
}

fun Element.plot(vararg classes: String, init: Plot.() -> Unit): Plot = initElement(classes, Plot(), init)
fun Element.histogram(vararg classes: String, init: Histogram.() -> Unit): Histogram = initElement(classes, Histogram(), init)
fun Element.scope(vararg classes: String, init: Scope.() -> Unit): Scope = initElement(classes, Scope(), init)
