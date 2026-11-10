package org.openrndr.extra.shapes.bezierpatches

import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import kotlin.math.floor
import kotlin.random.Random

/**
 * Places spread evenly over this patch, as u, v: places at least [distance] apart on the surface, by the distance in
 * space between their points, until no more fit (a Poisson disk sampling). Candidates are drawn in proportion to the
 * area of the surface, so where the patch stretches it gets as many places as it has room for, and where it is
 * squeezed it gets few, unlike places spread evenly in u, v. Use [frames] to place things at them.
 *
 * @param distance the smallest distance in space between two places
 * @param random the source of randomness, the same seed gives the same places
 * @param maxCount the most places
 * @param misses the number of candidates in a row that do not fit after which the patch counts as full
 * @param resolution the number of cells along u and along v of the table of areas that candidates are drawn from
 */
fun BezierPatch3D.scatter(
    distance: Double,
    random: Random = Random.Default,
    maxCount: Int = Int.MAX_VALUE,
    misses: Int = 3000,
    resolution: Int = 64
): List<Vector2> {
    require(distance > 0.0) { "the distance must be positive" }
    val n = resolution.coerceAtLeast(1)
    // the areas of the cells of a grid in u, v, as two triangles of their corners, accumulated
    val corners = List(n + 1) { j -> List(n + 1) { i -> position(i.toDouble() / n, j.toDouble() / n) } }
    val cumulative = DoubleArray(n * n)
    var total = 0.0
    for (j in 0 until n) for (i in 0 until n) {
        val a = corners[j][i]
        val b = corners[j][i + 1]
        val c = corners[j + 1][i + 1]
        val d = corners[j + 1][i]
        total += ((b - a).cross(c - a).length + (c - a).cross(d - a).length) / 2.0
        cumulative[j * n + i] = total
    }
    if (total <= 0.0) return emptyList()

    // the places by the cell of a grid in space with cells as wide as the distance
    val cells = HashMap<Triple<Int, Int, Int>, MutableList<Vector3>>()
    fun cell(p: Vector3) = Triple(floor(p.x / distance).toInt(), floor(p.y / distance).toInt(), floor(p.z / distance).toInt())
    fun fits(p: Vector3): Boolean {
        val (cx, cy, cz) = cell(p)
        for (x in cx - 1..cx + 1) for (y in cy - 1..cy + 1) for (z in cz - 1..cz + 1) {
            val near = cells[Triple(x, y, z)] ?: continue
            if (near.any { it.squaredDistanceTo(p) < distance * distance }) return false
        }
        return true
    }

    val result = mutableListOf<Vector2>()
    var missed = 0
    while (missed < misses && result.size < maxCount) {
        // a cell by its area, a place in it uniformly in u, v
        val r = random.nextDouble() * total
        var lo = 0
        var hi = cumulative.size - 1
        while (lo < hi) {
            val mid = (lo + hi) / 2
            if (cumulative[mid] < r) lo = mid + 1 else hi = mid
        }
        val uv = Vector2((lo % n + random.nextDouble()) / n, (lo / n + random.nextDouble()) / n)
        val p = position(uv.x, uv.y)
        if (fits(p)) {
            cells.getOrPut(cell(p)) { mutableListOf() }.add(p)
            result.add(uv)
            missed = 0
        } else {
            missed++
        }
    }
    return result
}

/**
 * Places on a grid over this patch whose lines are spaced evenly along the surface rather than in u, v, as rows of u, v:
 * [rows] rows from v = 0 to v = 1, spaced by length along the patch where u is 0.5, and in every row [columns] places
 * from u = 0 to u = 1, spaced by length along that row. Both counts are at least 2, so that the grid reaches the edges.
 */
fun BezierPatch3D.evenGrid(columns: Int, rows: Int): List<List<Vector2>> {
    require(columns >= 2 && rows >= 2) { "a grid has at least 2 columns and 2 rows" }
    val down = vertical(0.5).rectified()
    return List(rows) { j ->
        val v = if (j == rows - 1) 1.0 else down.rectify(j.toDouble() / (rows - 1))
        val across = horizontal(v).rectified()
        List(columns) { i ->
            val u = if (i == columns - 1) 1.0 else across.rectify(i.toDouble() / (columns - 1))
            Vector2(u, v)
        }
    }
}
