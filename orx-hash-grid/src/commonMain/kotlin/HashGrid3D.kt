package org.openrndr.extra.hashgrid
import org.openrndr.math.Vector3
import kotlin.jvm.JvmRecord
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

private fun Double.fastFloor(): Int {
    return if (this >= 0) this.toInt() else this.toInt() - 1
}

@JvmRecord
private data class GridCoords3D(val x: Int, val y: Int, val z: Int) {
    fun offset(i: Int, j: Int, k : Int): GridCoords3D = copy(x = x + i, y = y + j, z = z + k)
}

class Cell3D(val x: Int, val y: Int, val z: Int, val cellSize: Double) {
    var xMin: Double = Double.POSITIVE_INFINITY
        private set
    var xMax: Double = Double.NEGATIVE_INFINITY
        private set
    var yMin: Double = Double.POSITIVE_INFINITY
        private set
    var yMax: Double = Double.NEGATIVE_INFINITY
        private set
    var zMin: Double = Double.POSITIVE_INFINITY
        private set
    var zMax: Double = Double.NEGATIVE_INFINITY
        private set

    val bounds: Box3D
        get() {
            return Box3D(Vector3(x * cellSize, y * cellSize, z * cellSize), cellSize, cellSize, cellSize)
        }

    val contentBounds: Box3D
        get() {
            return if (points.isEmpty()) {
                Box3D.EMPTY
            } else {
                Box3D(Vector3(xMin, yMin, zMin), xMax - xMin, yMax - yMin, zMax - zMin)
            }
        }

    internal val points = mutableListOf<Pair<Vector3, Any?>>()
    internal fun insert(point: Vector3, owner: Any?) {
        points.add(Pair(point, owner))
        xMin = min(xMin, point.x)
        xMax = max(xMax, point.x)
        yMin = min(yMin, point.y)
        yMax = max(yMax, point.y)
        zMin = min(zMin, point.z)
        zMax = max(zMax, point.z)
    }

    internal fun squaredDistanceTo(query: Vector3): Double {
        val width = xMax - xMin
        val height = yMax - yMin
        val depth = zMax - zMin
        val x = (xMin + xMax) / 2.0
        val y = (yMin + yMax) / 2.0
        val z = (zMin + zMax) / 2.0
        val dx = max(abs(query.x - x) - width / 2, 0.0)
        val dy = max(abs(query.y - y) - height / 2, 0.0)
        val dz = max(abs(query.z - z) - depth / 2, 0.0)
        return dx * dx + dy * dy + dz * dz
    }

    fun points() = sequence {
        for (point in points) {
            yield(point)
        }
    }
}

class HashGrid3D(val radius: Double) {
    private val cells = mutableMapOf<GridCoords3D, Cell3D>()
    fun cells() = sequence {
        for (cell in cells.values) {
            yield(cell)
        }
    }

    var size: Int = 0
        private set

    val cellSize = radius / sqrt(3.0)
    private fun coords(v: Vector3): GridCoords3D {
        val x = (v.x / cellSize).fastFloor()
        val y = (v.y / cellSize).fastFloor()
        val z = (v.z / cellSize).fastFloor()
        return GridCoords3D(x, y, z)
    }

    fun points() = sequence {
        for (cell in cells.values) {
            for (point in cell.points) {
                yield(point)
            }
        }
    }

    fun random(random: Random = Random.Default): Vector3 {
        return cells.values.random(random).points.random().first
    }

    fun insert(point: Vector3, owner: Any? = null) {
        val gc = coords(point)
        val cell = cells.getOrPut(gc) { Cell3D(gc.x, gc.y, gc.z, cellSize) }
        cell.insert(point, owner)
        size += 1
    }

    fun cell(query: Vector3): Cell3D? = cells[coords(query)]

    fun isFree(query: Vector3, ignoreOwners: Set<Any> = emptySet()): Boolean {
        val c = coords(query)
        if (cells[c] == null) {
            for (k in -2..2) {
                for (j in -2..2) {
                    for (i in -2..2) {
                        if (i == 0 && j == 0 && k == 0) {
                            continue
                        }
                        val n = c.offset(i, j, k)
                        val nc = cells[n]
                        if (nc != null && nc.squaredDistanceTo(query) <= radius * radius) {
                            for (p in nc.points) {
                                if (p.second == null || p.second !in ignoreOwners) {
                                    if (p.first.squaredDistanceTo(query) <= radius * radius) {
                                        return false
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return true
        } else {
            return cells[c]!!.points.all { it.second != null && it.second in ignoreOwners }
        }
    }
}

/**
 * Construct a hash grid containing all points in the list
 * @param radius radius of the hash grid
 */
fun List<Vector3>.hashGrid(radius: Double): HashGrid3D {
    val grid = HashGrid3D(radius)
    for (point in this) {
        grid.insert(point)
    }
    return grid
}

/**
 * Return a list that only contains points at a minimum distance.
 * @param radius the minimum distance between any two points in the returned list
 */
fun List<Vector3>.filter(radius: Double): List<Vector3> {
    return if (size <= 1) {
        this
    } else {
        val grid = HashGrid3D(radius)
        for (point in this) {
            if (grid.isFree(point)) {
                grid.insert(point)
            }
        }
        grid.points().map { it.first }.toList()
    }
}