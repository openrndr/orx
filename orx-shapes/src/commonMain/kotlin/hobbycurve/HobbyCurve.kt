package org.openrndr.extra.shapes.hobbycurve
// Code adapted from http://weitz.de/hobby/

import org.openrndr.math.*
import org.openrndr.math.transforms.buildTransform
import org.openrndr.shape.*
import kotlin.math.*

fun ShapeContour.hobbyCurve(curl: Double = 0.0): ShapeContour {
    val vertices = if (closed)
        segments.map { it.start }
    else
        segments.map { it.start } + segments.last().end
    return hobbyCurve(vertices, closed, curl)
}

fun Shape.hobbyCurve(curl: Double = 0.0): Shape {
    return Shape(contours.map {
        it.hobbyCurve(curl)
    })
}

private fun Vector2.atan22(other: Vector2): Double {
    val u = this.normalized
    val v = other.normalized
    val x = u.cross(v)
    val y = u.dot(v)
    return atan2(x, y)
}

private fun Vector3.atan22(other: Vector3): Double {
    val u = this.normalized
    val v = other.normalized
    val x = u.cross(v).length
    val y = u.dot(v)
    return atan2(x, y)
}


/**
 * Generates a smooth contour passing through a set of points based on Hobby's algorithm.
 *
 * @param points A list of 2D points through which the curve should pass.
 * @param closed A boolean value indicating whether the curve is closed (true)
 *               forming a loop, or open (false). The default is false.
 * @param curl A parameter that controls the curvature of the open-ended curve. Only
 *             applicable if the curve is not closed. The default is 0.0.
 * @param tensions A lambda function that accepts the chord index (an integer) and returns
 *                 a pair of tension values (Double) for the curve's control points. These
 *                 tensions influence how tightly the curve conforms to the points.
 *                 The default lambda assigns both values as 1.0.
 * @return A ShapeContour object representing the smoothed curve based on Hobby's algorithm.
 */
fun hobbyCurve(
    points: List<Vector2>, closed: Boolean = false, curl: Double = 0.0,
    tensions: (chordIndex: Int, inAngleDegrees:Double, outAngleDegrees:Double) -> Pair<Double, Double> = { _, _, _ -> Pair(1.0, 1.0) },
): ShapeContour {
    if (points.size <= 1) return ShapeContour.EMPTY

    val m = points.size

    /** Chord count */
    val n = if (closed) m else m - 1

    /** Chords array stores vectors representing line segments between consecutive points
    Each chord is calculated as the vector difference between the next point and current point */
    val chords = Array(n) { points[(it + 1) % m] - points[it] }
    val distances = Array(n) { chords[it].length }

    require(distances.all { it > 0.0 })

    /** Array storing turning angles (in radians) between adjacent chords at each point
    For each point i, gamma[i] represents the angle between chord[i-1] and chord[i] */
    val gamma = DoubleArray(m)
    for (i in (if (closed) 0 else 1) until n) {
        gamma[i] = chords[(i - 1).mod(m)].atan22(chords[(i).mod(m)])
    }
    if (!closed) gamma[n] = 0.0

    val a = DoubleArray(m) { 0.0 }
    val b = DoubleArray(m) { 0.0 }
    val c = DoubleArray(m) { 0.0 }
    val d = DoubleArray(m) { 0.0 }

    for (i in (if (closed) 0 else 1) until n) {
        val next = (i + 1).mod(m)
        val prev = (i - 1).mod(m)
        a[i] = 1 / distances[prev]
        b[i] = (2 * distances[prev] + 2 * distances[i]) / (distances[prev] * distances[i])
        c[i] = 1 / distances[i]
        d[i] = -(2 * gamma[i] * distances[i] + gamma[next] * distances[prev]) / (distances[prev] * distances[i])
    }

    val alpha: DoubleArray
    val beta: DoubleArray

    if (!closed) {
        a[0] = 0.0
        b[0] = 2 + curl
        c[0] = 2 * curl + 1
        d[0] = -c[0] * gamma[1]

        a[n] = 2 * curl + 1
        b[n] = 2 + curl
        c[n] = 0.0
        d[n] = 0.0

        alpha = thomas(a, b, c, d)
        beta = DoubleArray(n) { 0.0 }
        for (i in 0 until n - 1) {
            beta[i] = -gamma[i + 1] - alpha[i + 1]
        }
        beta[n - 1] = -alpha[n]
    } else {
        val s = a[0]
        a[0] = 0.0
        val t = c[n - 1]
        c[n - 1] = 0.0
        alpha = sherman(a, b, c, d, s, t)
        beta = DoubleArray(n)
        for (i in 0 until n) {
            val j = (i + 1) % n
            beta[i] = -gamma[j] - alpha[j]
        }
    }

    val c1s = ArrayList<Vector2>(n)
    val c2s = ArrayList<Vector2>(n)
    for (i in 0 until n) {
        val v1 = rotateAngle(chords[i], alpha[i]).normalized
        val v2 = rotateAngle(chords[i], -beta[i]).normalized
        val t = tensions(i, gamma[i].asDegrees, gamma[(i + 1).mod(m)].asDegrees)
        c1s.add(points[i % m] + v1 * rho(alpha[i], beta[i]) * t.first * distances[i] / 3.0)
        c2s.add(points[(i + 1) % m] - v2 * rho(beta[i], alpha[i]) * t.second * distances[i] / 3.0)
    }
    return ShapeContour(List(n) { Segment2D(points[it], c1s[it], c2s[it], points[(it + 1) % m]) }, closed = closed)
}

/**
 * Uses Hobby's algorithm to construct a [Path3D] through a given list of points.
 * @param points The list of points through which the curve should go.
 * @param closed Whether to construct a closed or open curve.
 * @param curl The 'curl' at the endpoints of the curve; this is only applicable when [closed] is false. Best results for values in [-1, 1], where a higher value makes segments closer to circular arcs.
 * @param tensions A function that returns the in and out tensions given a chord index.
 * @return A [Path3D] through [points].
 */
fun hobbyCurve(
    points: List<Vector3>,
    closed: Boolean = false,
    curl: Double = 0.0,
    tensions: (chordIndex: Int, inAngleDegrees:Double, outAngleDegrees:Double) -> Pair<Double, Double> = { _, _, _ -> Pair(1.0, 1.0) },
): Path3D {
    if (points.size <= 1) return Path3D.EMPTY

    val m = points.size
    val n = if (closed) m else m - 1

    val chords = Array(n) { points[(it + 1) % m] - points[it] }
    val distances = Array(n) { chords[it].length }
    val normals = Array(n) { Vector3.ZERO }

    require(distances.all { it > 0.0 })

    val gamma = DoubleArray(m)
    for (i in (if (closed) 0 else 1) until n) {
        val zc = chords[i].normalized
        val zp = chords[(i - 1).mod(m)].normalized
        val normal = zc.cross(-zp)
        gamma[i] = zp.atan22(zc) * sign(normal.z)
        normals[i] = normal * sign(normal.z)
    }
    if (!closed) {
        gamma[n] = 0.0
    }

    val a = DoubleArray(m) { 0.0 }
    val b = DoubleArray(m) { 0.0 }
    val c = DoubleArray(m) { 0.0 }
    val d = DoubleArray(m) { 0.0 }

    for (i in (if (closed) 0 else 1) until n) {
        val j = (i + 1).mod(m)
        val k = (i - 1).mod(m)

        a[i] = 1 / distances[k]
        b[i] = (2 * distances[k] + 2 * distances[i]) / (distances[k] * distances[i])
        c[i] = 1 / distances[i]
        d[i] = -(2 * gamma[i] * distances[i] + gamma[j] * distances[k]) / (distances[k] * distances[i])
    }

    val alpha: DoubleArray
    val beta: DoubleArray

    if (!closed) {
        a[0] = 0.0
        b[0] = 2 + curl
        c[0] = 2 * curl + 1
        d[0] = -c[0] * gamma[1]

        a[n] = 2 * curl + 1
        b[n] = 2 + curl
        c[n] = 0.0
        d[n] = 0.0

        alpha = thomas(a, b, c, d)
        beta = DoubleArray(n) { 0.0 }
        for (i in 0 until n - 1) {
            beta[i] = -gamma[i + 1] - alpha[i + 1]
        }
        beta[n - 1] = -alpha[n]
    } else {
        val s = a[0]
        a[0] = 0.0
        val t = c[n - 1]
        c[n - 1] = 0.0
        alpha = sherman(a, b, c, d, s, t)
        beta = DoubleArray(n) { 0.0 }
        for (i in 0 until n) {
            val j = (i + 1) % n
            beta[i] = -gamma[j] - alpha[j]
        }
    }

    val c1s = ArrayList<Vector3>(n)
    val c2s = ArrayList<Vector3>(n)
    for (i in 0 until n) {
        val r1 = buildTransform { rotate(normals[i], alpha[i].asDegrees) }
        val r2 = buildTransform { rotate(normals[(i + 1).mod(normals.size)], -beta[i].asDegrees) }
        val v1 = (r1 * chords[i].xyz0).xyz.normalized
        val v2 = (r2 * chords[i].xyz0).xyz.normalized
        val t = tensions(i, gamma[i].asDegrees, gamma[(i + 1).mod(m)].asDegrees)
        c1s.add(points[i % m] + v1 * rho(alpha[i], beta[i]) * distances[i] * t.first / 3.0)
        c2s.add(points[(i + 1) % m] - v2 * rho(beta[i], alpha[i]) * distances[i] * t.second / 3.0)
    }

    return Path3D(List(n) {
        Segment3D(points[it], c1s[it], c2s[it], points[(it + 1) % m])
    }, closed = closed)
}

/**
 * Solves a tridiagonal system of equations using the Thomas algorithm.
 * [Wikipedia](https://en.wikipedia.org/wiki/Tridiagonal_matrix_algorithm)
 * @param a The subdiagonal elements of the tridiagonal matrix.
 * @param b The diagonal elements of the tridiagonal matrix.
 * @param c The superdiagonal elements of the tridiagonal matrix.
 * @param d The right-hand side vector of the system.
 * @return A double array representing the solution to the tridiagonal system.
 */
private fun thomas(a: DoubleArray, b: DoubleArray, c: DoubleArray, d: DoubleArray): DoubleArray {
    val n = a.size
    val cc = DoubleArray(n) { 0.0 }
    val dd = DoubleArray(n) { 0.0 }
    cc[0] = c[0] / b[0]
    dd[0] = d[0] / b[0]
    for (i in 1 until n) {
        val den = b[i] - cc[i - 1] * a[i]
        cc[i] = c[i] / den
        dd[i] = (d[i] - dd[i - 1] * a[i]) / den
    }
    val x = DoubleArray(n) { 0.0 }
    x[n - 1] = dd[n - 1]
    for (i in n - 2 downTo 0) {
        x[i] = dd[i] - cc[i] * x[i + 1]
    }
    return x
}

/**
 * Uses the Sherman-Morrison formula to solve a system of linear equations with a modified
 * tridiagonal matrix, based on the Thomas algorithm.
 *
 * @param a The lower diagonal coefficients of the tridiagonal matrix.
 * @param b The main diagonal coefficients of the tridiagonal matrix.
 * @param c The upper diagonal coefficients of the tridiagonal matrix.
 * @param d The right-hand side vector of the system of equations.
 * @param s The value for the last row modification in the matrix.
 * @param t The value for the first row modification in the matrix.
 * @return A solution vector of the system of equations considering the specified matrix modifications.
 */
private fun sherman(
    a: DoubleArray,
    b: DoubleArray,
    c: DoubleArray,
    d: DoubleArray,
    s: Double,
    t: Double
): DoubleArray {
    val n = a.size
    val u = DoubleArray(n) { if (it == 0 || it == n - 1) 1.0 else 0.0 }
    val v = DoubleArray(n) {
        when (it) {
            0 -> t; n - 1 -> s; else -> 0.0
        }
    }
    b[0] -= t
    b[n - 1] -= s
    val Td = thomas(a, b, c, d)
    val Tu = thomas(a, b, c, u)
    val factor = (t * Td[0] + s * Td[n - 1]) / (1 + t * Tu[0] + s * Tu[n - 1])
    return DoubleArray(n) {
        Td[it] - factor * Tu[it]
    }
}

/**
 * Calculates a ratio used in Hobby's curve construction based on the angles between curve segments.
 *
 * @param a The angle (in radians) at the current point on the curve.
 * @param b The angle (in radians) at the neighboring point on the curve.
 * @return A calculated ratio used to control the curve's shape.
 */
private fun rho(a: Double, b: Double): Double {
    val sa = sin(a)
    val sb = sin(b)
    val ca = cos(a)
    val cb = cos(b)
    val s5 = sqrt(5.0)
    val num = 4 + sqrt(8.0) * (sa - sb / 16) * (sb - sa / 16) * (ca - cb)
    val den = 2 + (s5 - 1) * ca + (3 - s5) * cb
    return num / den
}

private fun rotate(v: Vector2, s: Double, c: Double) = Vector2(v.x * c - v.y * s, v.x * s + v.y * c)
private fun rotateAngle(v: Vector2, alpha: Double) = rotate(v, sin(alpha), cos(alpha))