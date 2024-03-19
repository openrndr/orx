package org.openrndr.extra.shapes.bezierpatches

import org.openrndr.color.AlgebraicColor
import org.openrndr.color.ColorRGBa
import org.openrndr.color.ConvertibleToColorRGBa
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector3
import org.openrndr.shape.Path3D
import org.openrndr.shape.Segment3D
import kotlin.random.Random

open class BezierPatch3DBase<C>(
    val points: List<List<Vector3>>,
    val colors: List<List<C>> = emptyList()
)
        where C : AlgebraicColor<C>, C : ConvertibleToColorRGBa {
    init {
        require(points.size == 4 && points.all { it.size == 4 })
        require(colors.isEmpty() || colors.size == 4 && colors.all { it.size == 4 })
    }

    /**
     * Return a transposed version of the bezier path by transposing the [points] matrix
     */
    val transposed
        get() = BezierPatch3DBase(
            listOf(
                listOf(points[0][0], points[1][0], points[2][0], points[3][0]),
                listOf(points[0][1], points[1][1], points[2][1], points[3][1]),
                listOf(points[0][2], points[1][2], points[2][2], points[3][2]),
                listOf(points[0][3], points[1][3], points[2][3], points[3][3]),
            ),
            if (colors.isEmpty()) emptyList() else {
                listOf(
                    listOf(colors[0][0], colors[1][0], colors[2][0], colors[3][0]),
                    listOf(colors[0][1], colors[1][1], colors[2][1], colors[3][1]),
                    listOf(colors[0][2], colors[1][2], colors[2][2], colors[3][2]),
                    listOf(colors[0][3], colors[1][3], colors[2][3], colors[3][3]),
                )
            }
        )

    fun transform(transform: Matrix44) = BezierPatch3DBase(points.map { r ->
        r.map { (transform * it.xyz1).div }
    }, colors)

    private fun coeffs2(t: Double): DoubleArray {
        val it = 1.0 - t
        val it2 = it * it
        val t2 = t * t
        return doubleArrayOf(it2, 2 * it * t, t2)
    }

    private fun coeffs3(t: Double): DoubleArray {
        val it = 1.0 - t
        val it2 = it * it
        val it3 = it2 * it
        val t2 = t * t
        val t3 = t2 * t
        return doubleArrayOf(it3, 3 * it2 * t, 3 * it * t2, t3)
    }

    /**
     * Return a point on the patch by using its u,v parameterization
     * @param u a value between 0 and 1
     * @param v a value between 0 and 1
     */
    fun position(u: Double, v: Double): Vector3 {
        val csu = coeffs3(u)
        val csv = coeffs3(v)
        var result = Vector3.ZERO
        for (j in 0 until 4) {
            for (i in 0 until 4) {
                result += points[j][i] * csu[i] * csv[j]
            }
        }
        return result
    }

    /**
     * Return a gradient vector on the patch by using its u,v parameterization
     * @param u a value between 0 and 1
     * @param v a value between 0 and 1
     */
    fun gradient(u: Double, v: Double): Vector3 {
        val f0 = List(4) { MutableList(3) { Vector3.ZERO } }
        for (j in 0 until 4) {
            for (i in 0 until 3) {
                f0[j][i] = points[j][i + 1] - points[j][i]
            }
        }

        val f1 = List(3) { MutableList(3) { Vector3.ZERO } }
        for (j in 0 until 3) {
            for (i in 0 until 3) {
                f1[j][i] = f0[j + 1][i] - f0[j][i]
            }
        }

        val csu = coeffs2(u)
        val csv = coeffs2(v)
        var result = Vector3.ZERO
        for (j in 0 until 3) {
            for (i in 0 until 3) {
                result += f1[j][i] * csu[i] * csv[j]
            }
        }
        return result
    }

    /**
     * Generate a random point on the path
     * @return a point that is uniformly distributed in uv space
     */
    fun randomPoint(random: Random = Random.Default) = position(random.nextDouble(), random.nextDouble())

    fun horizontal(v: Double): Path3D {
        val cs = coeffs3(v)
        val cps = Array(4) { Vector3.ZERO }
        for (j in 0 until 4) {
            for (i in 0 until 4) {
                cps[j] += points[i][j] * cs[i]
            }
        }
        return Path3D(listOf(Segment3D(cps[0], cps[1], cps[2], cps[3])), false)
    }

    fun vertical(u: Double): Path3D {
        val cs = coeffs3(u)
        val cps = Array(4) { Vector3.ZERO }
        for (j in 0 until 4) {
            for (i in 0 until 4) {
                cps[j] += points[j][i] * cs[i]
            }
        }
        return Path3D(listOf(Segment3D(cps[0], cps[1], cps[2], cps[3])), false)
    }

    /**
     * Extract a sub-patch based on uv parameterization
     */
    fun sub(u0: Double, v0: Double, u1: Double, v1: Double): BezierPatch3DBase<C> {
        val c0 = Segment3D(points[0][0], points[0][1], points[0][2], points[0][3]).sub(u0, u1) as Segment3D
        val c1 = Segment3D(points[1][0], points[1][1], points[1][2], points[1][3]).sub(u0, u1) as Segment3D
        val c2 = Segment3D(points[2][0], points[2][1], points[2][2], points[2][3]).sub(u0, u1) as Segment3D
        val c3 = Segment3D(points[3][0], points[3][1], points[3][2], points[3][3]).sub(u0, u1) as Segment3D

        val sub0 = bezierPatch(c0, c1, c2, c3)
        val d0 = Segment3D(sub0.points[0][0], sub0.points[1][0], sub0.points[2][0], sub0.points[3][0]).sub(v0, v1) as Segment3D
        val d1 = Segment3D(sub0.points[0][1], sub0.points[1][1], sub0.points[2][1], sub0.points[3][1]).sub(v0, v1) as Segment3D
        val d2 = Segment3D(sub0.points[0][2], sub0.points[1][2], sub0.points[2][2], sub0.points[3][2]).sub(v0, v1) as Segment3D
        val d3 = Segment3D(sub0.points[0][3], sub0.points[1][3], sub0.points[2][3], sub0.points[3][3]).sub(v0, v1) as Segment3D

        return fromSegments<C>(d0, d1, d2, d3).transposed
    }

    val path: Path3D = Path3D(
        listOf(
            Segment3D(points[0][0], points[0][1], points[0][2], points[0][3]),
            Segment3D(points[0][3], points[1][3], points[2][3], points[3][3]),
            Segment3D(points[3][3], points[3][2], points[3][1], points[3][0]),
            Segment3D(points[3][0], points[2][0], points[1][0], points[0][0]),
        ), true
    )

    operator fun times(scale: Double) =
        BezierPatch3DBase(
            points.map { j -> j.map { i -> i * scale } },
            if (colors.isEmpty()) colors else colors.map { j -> j.map { i -> i * scale } }
        )

    operator fun div(scale: Double) =
        BezierPatch3DBase(points.map { j -> j.map { i -> i / scale } },
            if (colors.isEmpty()) colors else colors.map { j -> j.map { i -> i / scale } }
        )
    operator fun plus(right: BezierPatch3DBase<C>) =
        BezierPatch3DBase(List(4) { j -> List(4) { i -> points[j][i] + right.points[j][i] } },
            if (colors.isEmpty() && right.colors.isEmpty()) { colors }
            else if (colors.isEmpty() && right.colors.isNotEmpty()) { right.colors }
            else if (colors.isNotEmpty() && right.colors.isEmpty()) { colors }
            else { List(4) { j -> List(4) { i -> colors[j][i] + right.colors[j][i] } } }
            )

    operator fun minus(right: BezierPatch3DBase<C>) =
        BezierPatch3DBase(List(4) { j -> List(4) { i -> points[j][i] - right.points[j][i] } },
            if (colors.isEmpty() && right.colors.isEmpty()) { colors }
            else if (colors.isEmpty() && right.colors.isNotEmpty()) { right.colors }
            else if (colors.isNotEmpty() && right.colors.isEmpty()) { colors }
            else { List(4) { j -> List(4) { i -> colors[j][i] - right.colors[j][i] } } }
            )

    fun <K> withColors(colors: List<List<K>>): BezierPatch3DBase<K>
            where K : AlgebraicColor<K>, K : ConvertibleToColorRGBa {
        return BezierPatch3DBase(points, colors)
    }

    companion object {
        fun <C> fromSegments(c0: Segment3D, c1: Segment3D, c2: Segment3D, c3: Segment3D): BezierPatch3DBase<C>
                where C : AlgebraicColor<C>, C : ConvertibleToColorRGBa {
            val c0c = c0.cubic
            val c1c = c1.cubic
            val c2c = c2.cubic
            val c3c = c3.cubic

            val c0l = listOf(c0c.start, c0c.control[0], c0c.control[1], c0c.end)
            val c1l = listOf(c1c.start, c1c.control[0], c1c.control[1], c1c.end)
            val c2l = listOf(c2c.start, c2c.control[0], c2c.control[1], c2c.end)
            val c3l = listOf(c3c.start, c3c.control[0], c3c.control[1], c3c.end)

            return BezierPatch3DBase(listOf(c0l, c1l, c2l, c3l))
        }
    }
}

class BezierPatch3D(points: List<List<Vector3>>, colors: List<List<ColorRGBa>> = emptyList()) :
    BezierPatch3DBase<ColorRGBa>(points, colors)

/**
 * Create a cubic bezier patch from 4 segments. The control points of the segments are used in row-wise fashion
 */
fun bezierPatch(c0: Segment3D, c1: Segment3D, c2: Segment3D, c3: Segment3D): BezierPatch3D {
    val c0c = c0.cubic
    val c1c = c1.cubic
    val c2c = c2.cubic
    val c3c = c3.cubic

    val c0l = listOf(c0c.start, c0c.control[0], c0c.control[1], c0c.end)
    val c1l = listOf(c1c.start, c1c.control[0], c1c.control[1], c1c.end)
    val c2l = listOf(c2c.start, c2c.control[0], c2c.control[1], c2c.end)
    val c3l = listOf(c3c.start, c3c.control[0], c3c.control[1], c3c.end)

    return BezierPatch3D(listOf(c0l, c1l, c2l, c3l))
}

/**
 * Create a bezier patch from a closed shape contour (with 4 segments).
 * @param alpha control for linearity, default is `1.0/3.0`
 */
fun bezierPatch(path: Path3D, alpha: Double = 1.0 / 3.0): BezierPatch3D {
    require(path.segments.size == 4) {
        """contour needs exactly 4 segments (has ${path.segments.size})"""
    }
    val c0 = path.segments[0].cubic
    val c1 = path.segments[1].cubic
    val c2 = path.segments[2].cubic
    val c3 = path.segments[3].cubic

    val fa = 1.0 - alpha
    val fb = alpha

    val x00 = (c0.control[0] * fa + c2.control[1] * fb + c3.control[1] * fa + c1.control[0] * fb) / 2.0
    val x01 = (c0.control[1] * fa + c2.control[0] * fb + c3.control[1] * fb + c1.control[0] * fa) / 2.0
    val x10 = (c0.control[0] * fb + c2.control[1] * fa + c3.control[0] * fa + c1.control[1] * fb) / 2.0
    val x11 = (c0.control[1] * fb + c2.control[0] * fa + c3.control[0] * fb + c1.control[1] * fa) / 2.0
    val cps = listOf(
        listOf(c0.start, c0.control[0], c0.control[1], c0.end),
        listOf(c3.control[1], x00, x01, c1.control[0]),
        listOf(c3.control[0], x10, x11, c1.control[1]),
        listOf(c2.end, c2.control[1], c2.control[0], c2.start),
    )
    return BezierPatch3D(cps)
}

/**
 * Create a bezier patch from 4 corners
 * @param corners a list of corners from which to create the patch
 * @param alpha control for linearity, default is `1.0/3.0`
 */
fun bezierPatch(corners: List<Vector3>, alpha: Double = 1.0 / 3.0): BezierPatch3D {
    require(corners.size == 4) {
        """need exactly 4 corners (got ${corners.size}"""
    }
    return bezierPatch(Path3D.fromPoints(corners, true), alpha)
}
