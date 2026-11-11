package org.openrndr.extra.shapes.bezierpatches

import org.openrndr.color.AlgebraicColor
import org.openrndr.color.ColorRGBa
import org.openrndr.color.ConvertibleToColorRGBa
import org.openrndr.math.GeometricPrimitive3D
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector3
import org.openrndr.shape.Path3D
import org.openrndr.shape.Segment3D
import kotlin.random.Random

/**
 * Represents a 3D Bezier patch defined by a 4x4 grid of control points and optional color data.
 * This class provides utilities for manipulation, transformation, and evaluation of the patch.
 *
 * The control points and colors must be organized as a 4x4 grid. The patch supports operations
 * including transformation, sub-patching, path extraction, and random point generation.
 *
 * @param C The type of color data associated with the patch. It must implement both
 * `AlgebraicColor` and `ConvertibleToColorRGBa`.
 * @property points A 4x4 grid of control points that define the shape of the Bezier patch.
 * @property colors A 4x4 grid of color data corresponding to the control points. This parameter
 * is optional and defaults to an empty list.
 *
 * @throws IllegalArgumentException if `points` or `colors`, if provided, do not conform
 * to the required 4x4 structure.
 */
open class BezierPatch3DBase<C>(
    val points: List<List<Vector3>>,
    val colors: List<List<C>> = emptyList()
)
        where C : AlgebraicColor<C>, C : ConvertibleToColorRGBa {
    init {
        require(points.size == 4 && points.all { it.size == 4 })
        require(colors.isEmpty() || colors.size == 4 && colors.all { it.size == 4 })
    }

    // the coordinates of the control points, row by row, for evaluation without allocations
    private val xs = DoubleArray(16) { points[it / 4][it % 4].x }
    private val ys = DoubleArray(16) { points[it / 4][it % 4].y }
    private val zs = DoubleArray(16) { points[it / 4][it % 4].z }

    /** the sum over the control points, weighted by [a0]..[a3] along u and [b0]..[b3] along v */
    private fun combine(
        a0: Double, a1: Double, a2: Double, a3: Double,
        b0: Double, b1: Double, b2: Double, b3: Double
    ): Vector3 {
        var rx = 0.0
        var ry = 0.0
        var rz = 0.0
        for (j in 0 until 4) {
            val b = when (j) {
                0 -> b0; 1 -> b1; 2 -> b2; else -> b3
            }
            val o = j * 4
            rx += (xs[o] * a0 + xs[o + 1] * a1 + xs[o + 2] * a2 + xs[o + 3] * a3) * b
            ry += (ys[o] * a0 + ys[o + 1] * a1 + ys[o + 2] * a2 + ys[o + 3] * a3) * b
            rz += (zs[o] * a0 + zs[o + 1] * a1 + zs[o + 2] * a2 + zs[o + 3] * a3) * b
        }
        return Vector3(rx, ry, rz)
    }

    /**
     * Return a transposed version of the bezier path by transposing the [points] matrix
     */
    open val transposed: BezierPatch3DBase<C>
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

    open fun transform(transform: Matrix44): BezierPatch3DBase<C> = BezierPatch3DBase(points.map { r ->
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
    fun position(u: Double, v: Double): Vector3 = combine(
        bernstein3(u, 0), bernstein3(u, 1), bernstein3(u, 2), bernstein3(u, 3),
        bernstein3(v, 0), bernstein3(v, 1), bernstein3(v, 2), bernstein3(v, 3)
    )

    /** the derivative of the patch along u at [u], [v] */
    fun du(u: Double, v: Double): Vector3 = combine(
        dbernstein3(u, 0), dbernstein3(u, 1), dbernstein3(u, 2), dbernstein3(u, 3),
        bernstein3(v, 0), bernstein3(v, 1), bernstein3(v, 2), bernstein3(v, 3)
    )

    /** the derivative of the patch along v at [u], [v] */
    fun dv(u: Double, v: Double): Vector3 = combine(
        bernstein3(u, 0), bernstein3(u, 1), bernstein3(u, 2), bernstein3(u, 3),
        dbernstein3(v, 0), dbernstein3(v, 1), dbernstein3(v, 2), dbernstein3(v, 3)
    )

    /** the mixed derivative of the patch along u and v at [u], [v]: the twist vector */
    fun duv(u: Double, v: Double): Vector3 = combine(
        dbernstein3(u, 0), dbernstein3(u, 1), dbernstein3(u, 2), dbernstein3(u, 3),
        dbernstein3(v, 0), dbernstein3(v, 1), dbernstein3(v, 2), dbernstein3(v, 3)
    )

    /**
     * the normal of the patch at [u], [v]: the cross product of [du] and [dv], of unit length; where one of them
     * vanishes, like at a corner where control points meet, from the derivatives a little inside the patch, then from
     * the diagonals of the control points, then the z axis, so that it always has unit length
     */
    fun normal(u: Double, v: Double): Vector3 = normal(u, v, du(u, v), dv(u, v))

    /** the [normal] at [u], [v] given the derivatives [du] and [dv] there */
    private fun normal(u: Double, v: Double, du: Vector3, dv: Vector3): Vector3 {
        val n = du.cross(dv)
        if (n.length > 1E-12) return n.normalized
        val e = 1E-4
        val uu = u.coerceIn(e, 1.0 - e)
        val vv = v.coerceIn(e, 1.0 - e)
        val inside = du(uu, vv).cross(dv(uu, vv))
        if (inside.length > 1E-12) return inside.normalized
        val net = (points[3][3] - points[0][0]).cross(points[0][3] - points[3][0])
        if (net.length > 1E-12) return net.normalized
        return Vector3.UNIT_Z
    }

    /**
     * The frame of the patch at [u], [v]: its origin at [position], its z axis the [normal], its x axis along [du] in
     * the plane across the normal, and its y axis z × x, which follows [dv]; orthonormal and right-handed, x × y = z.
     * Something drawn in the plane of x and y around the origin lies on the patch there, as the envelope of a path on
     * a patch places it, see [org.openrndr.extra.shapes.distort.envelope]. Where [du] vanishes, like at an edge of the
     * patch that is collapsed to a point, x follows [du] a little inside the patch, or [dv] turned around the normal,
     * so the frame never has NaNs.
     */
    fun frame(u: Double, v: Double): Matrix44 {
        val du = du(u, v)
        val dv = dv(u, v)
        val z = normal(u, v, du, dv)
        // the direction of x: du, du a little inside the patch, dv turned a quarter around the normal, or any
        fun across(d: Vector3) = d - z * d.dot(z)
        var x = across(du)
        if (x.length <= 1E-12) {
            val e = 1E-4
            x = across(du(u.coerceIn(e, 1.0 - e), v.coerceIn(e, 1.0 - e)))
        }
        if (x.length <= 1E-12) x = across(dv.cross(z))
        if (x.length <= 1E-12) x = across(if (kotlin.math.abs(z.x) < 0.9) Vector3.UNIT_X else Vector3.UNIT_Y)
        x = x.normalized
        val y = z.cross(x)
        val o = position(u, v)
        return Matrix44.fromColumnVectors(x.xyz0, y.xyz0, z.xyz0, o.xyz1)
    }

    /** the [frame]s of the patch at the u, v of [uvs] */
    fun frames(uvs: List<org.openrndr.math.Vector2>): List<Matrix44> = uvs.map { frame(it.x, it.y) }

    /**
     * A ninth of the mixed derivative of the patch along u and v, [duv], at [u], [v]; not a gradient
     * @param u a value between 0 and 1
     * @param v a value between 0 and 1
     */
    @Deprecated("A ninth of the mixed derivative, not a gradient", ReplaceWith("duv(u, v) / 9.0"))
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
     * Extract a sub-patch based on uv parameterization, with its colors
     */
    open fun sub(u0: Double, v0: Double, u1: Double, v1: Double): BezierPatch3DBase<C> = BezierPatch3DBase(
        subGrid(points, u0, v0, u1, v1, { p, w -> p * w }, { a, b -> a + b }),
        if (colors.isEmpty()) emptyList() else subGrid(colors, u0, v0, u1, v1, { c, w -> c * w }, { a, b -> a + b })
    )

    val path: Path3D by lazy {
        Path3D(
        listOf(
            Segment3D(points[0][0], points[0][1], points[0][2], points[0][3]),
            Segment3D(points[0][3], points[1][3], points[2][3], points[3][3]),
            Segment3D(points[3][3], points[3][2], points[3][1], points[3][0]),
            Segment3D(points[3][0], points[2][0], points[1][0], points[0][0]),
        ), true
        )
    }

    open operator fun times(scale: Double): BezierPatch3DBase<C> =
        BezierPatch3DBase(
            points.map { j -> j.map { i -> i * scale } },
            if (colors.isEmpty()) colors else colors.map { j -> j.map { i -> i * scale } }
        )

    open operator fun div(scale: Double): BezierPatch3DBase<C> =
        BezierPatch3DBase(points.map { j -> j.map { i -> i / scale } },
            if (colors.isEmpty()) colors else colors.map { j -> j.map { i -> i / scale } }
        )
    open operator fun plus(right: BezierPatch3DBase<C>): BezierPatch3DBase<C> =
        BezierPatch3DBase(List(4) { j -> List(4) { i -> points[j][i] + right.points[j][i] } },
            if (colors.isEmpty() && right.colors.isEmpty()) { colors }
            else if (colors.isEmpty() && right.colors.isNotEmpty()) { right.colors }
            else if (colors.isNotEmpty() && right.colors.isEmpty()) { colors }
            else { List(4) { j -> List(4) { i -> colors[j][i] + right.colors[j][i] } } }
            )

    open operator fun minus(right: BezierPatch3DBase<C>): BezierPatch3DBase<C> =
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
    BezierPatch3DBase<ColorRGBa>(points, colors), GeometricPrimitive3D {
    private fun BezierPatch3DBase<ColorRGBa>.typed() = BezierPatch3D(points, colors)

    override val transposed: BezierPatch3D get() = super.transposed.typed()
    override fun transform(transform: Matrix44): BezierPatch3D = super.transform(transform).typed()
    override fun sub(u0: Double, v0: Double, u1: Double, v1: Double): BezierPatch3D = super.sub(u0, v0, u1, v1).typed()
    override fun times(scale: Double): BezierPatch3D = super.times(scale).typed()
    override fun div(scale: Double): BezierPatch3D = super.div(scale).typed()
    override fun plus(right: BezierPatch3DBase<ColorRGBa>): BezierPatch3D = super.plus(right).typed()
    override fun minus(right: BezierPatch3DBase<ColorRGBa>): BezierPatch3D = super.minus(right).typed()
}

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
