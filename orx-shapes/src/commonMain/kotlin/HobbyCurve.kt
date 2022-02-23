package org.openrndr.extra.shapes
// Code adapted from http://weitz.de/hobby/

import org.openrndr.math.Vector2
import org.openrndr.shape.Segment
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


fun ShapeContour.hobbyCurve(curl: Double = 0.0): ShapeContour {
    val vertices = if (closed)
        segments.map { it.start }
    else
        segments.map { it.start } + segments.last().end
    return hobbyCurve(vertices, closed, curl)
}

fun Shape.hobbyCurve(curl: Double = 0.0) : Shape {
    return Shape(contours.map {
        it.hobbyCurve(curl)
    })
}

/**
 * Uses Hobby's algorithm to construct a [ShapeContour] through a given list of points.
 * @param points The list of points through which the curve should go.
 * @param closed Whether to construct a closed or open curve.
 * @param curl The 'curl' at the endpoints of the curve; this is only applicable when [closed] is false. Best results for values in [-1, 1], where a higher value makes segments closer to circular arcs.
 * @return A [ShapeContour] through [points].
 */
fun hobbyCurve(points: List<Vector2>, closed: Boolean = false, curl: Double = 0.0): ShapeContour {
    if (points.size <= 1) return ShapeContour.EMPTY

    val m = points.size
    val n = if (closed) m else m - 1

    val diffs = Array(n) { points[(it+1) % m] - points[it] }
    val distances = Array(n) { diffs[it].length }

    val gamma = arrayOfNulls<Double>(m)
    for (i in (if (closed) 0 else 1) until n){
        val k = (i + m - 1) % m
        val n1 = diffs[k].normalized
        val s = n1.y
        val c = n1.x
        val v = rotate(diffs[i], -s, c)
        gamma[i] = atan2(v.y, v.x)
    }
    if (!closed) gamma[n] = 0.0

    val a = arrayOfNulls<Double>(m)
    val b = arrayOfNulls<Double>(m)
    val c = arrayOfNulls<Double>(m)
    val d = arrayOfNulls<Double>(m)

    for (i in (if (closed) 0 else 1) until n){
        val j = (i + 1) % m
        val k = (i + m - 1) % m

        a[i] = 1 / distances[k]
        b[i] = (2 * distances[k] + 2 * distances[i]) / (distances[k] * distances[i])
        c[i] = 1 / distances[i]
        d[i] = -(2 * gamma[i]!! * distances[i] + gamma[j]!! * distances[k]) / (distances[k] * distances[i])
    }

    lateinit var alpha: Array<Double>
    lateinit var beta: Array<Double?>

    if (!closed) {
        a[0] = 0.0
        b[0] = 2 + curl
        c[0] = 2 * curl + 1
        d[0] = -c[0]!! * gamma[1]!!

        a[n] = 2 * curl + 1
        b[n] = 2 + curl
        c[n] = 0.0
        d[n] = 0.0

        alpha = thomas(a.requireNoNulls(), b.requireNoNulls(), c.requireNoNulls(), d.requireNoNulls())
        beta = arrayOfNulls(n)
        for (i in 0 until n-1){
            beta[i] = -gamma[i+1]!! - alpha[i+1]
        }
        beta[n-1] = -alpha[n]
    } else {
        val s = a[0]!!
        a[0] = 0.0
        val t = c[n-1]!!
        c[n-1] = 0.0
        alpha = sherman(a.requireNoNulls(), b.requireNoNulls(), c.requireNoNulls(), d.requireNoNulls(), s, t)
        beta = arrayOfNulls(n)
        for (i in 0 until n){
            val j = (i+1) % n
            beta[i] = -gamma[j]!! - alpha[j]
        }
    }

    val c1s = mutableListOf<Vector2>()
    val c2s = mutableListOf<Vector2>()
    for (i in 0 until n){
        val v1 = rotateAngle(diffs[i], alpha[i]).normalized
        val v2 = rotateAngle(diffs[i], -beta[i]!!).normalized
        c1s.add(points[i % m] + v1 * rho(alpha[i], beta[i]!!) * distances[i] / 3.0)
        c2s.add(points[(i+1) % m] - v2 * rho(beta[i]!!, alpha[i]) * distances[i] / 3.0)
    }

    return ShapeContour(List(n) { Segment(points[it], c1s[it], c2s[it], points[(it+1)%m]) }, closed=closed)
}

private fun thomas(a: Array<Double>, b: Array<Double>, c: Array<Double>, d: Array<Double>): Array<Double> {
    val n = a.size
    val cc = arrayOfNulls<Double>(n)
    val dd = arrayOfNulls<Double>(n)
    cc[0] = c[0] / b[0]
    dd[0] = d[0] / b[0]
    for (i in 1 until n){
        val den = b[i] - cc[i-1]!! * a[i]
        cc[i] = c[i] / den
        dd[i] = (d[i] - dd[i-1]!!*a[i]) / den
    }
    val x = arrayOfNulls<Double>(n)
    x[n-1] = dd[n-1]
    for (i in n-2 downTo 0){
        x[i] = dd[i]!! - cc[i]!! * x[i+1]!!
    }
    return x.requireNoNulls()
}

private fun sherman(a: Array<Double>, b: Array<Double>, c: Array<Double>, d: Array<Double>, s: Double, t: Double): Array<Double> {
    val n = a.size
    val u = Array(n) { if (it == 0 || it == n-1) 1.0 else 0.0 }
    val v = Array(n) { when (it){ 0 -> t; n-1 -> s; else -> 0.0 } }
    b[0] -= t
    b[n-1] -= s
    val Td = thomas(a, b, c, d)
    val Tu = thomas(a, b, c, u)
    val factor = (t * Td[0] + s*Td[n-1]) / (1 + t * Tu[0] + s*Tu[n-1])
    return Array(n) {
        Td[it] - factor * Tu[it]
    }
}

private fun rho(a: Double, b: Double): Double {
    val sa = sin(a)
    val sb = sin(b)
    val ca = cos(a)
    val cb = cos(b)
    val s5 = sqrt(5.0)
    val num = 4 + sqrt(8.0) * (sa - sb/16) * (sb - sa/16) * (ca - cb)
    val den = 2 + (s5 - 1) * ca + (3 - s5) * cb
    return num/den
}

private fun rotate(v: Vector2, s: Double, c: Double) = Vector2(v.x * c - v.y * s, v.x * s + v.y * c)
private fun rotateAngle(v: Vector2, alpha: Double) = rotate(v, sin(alpha), cos(alpha))
