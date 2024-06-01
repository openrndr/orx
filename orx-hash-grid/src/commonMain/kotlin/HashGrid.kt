package org.openrndr.extra.hashgrid

import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
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
private data class GridCoords(val x: Int, val y: Int) {
    fun offset(i: Int, j: Int): GridCoords = copy(x = x + i, y = y + j)
}

class Cell(val x: Int, val y: Int, val cellSize: Double) {
    var xMin: Double = Double.POSITIVE_INFINITY
        private set
    var xMax: Double = Double.NEGATIVE_INFINITY
        private set
    var yMin: Double = Double.POSITIVE_INFINITY
        private set
    var yMax: Double = Double.NEGATIVE_INFINITY
        private set

    val bounds: Rectangle
        get() {
            return Rectangle(x * cellSize, y * cellSize, cellSize, cellSize)
        }

    val contentBounds: Rectangle
        get() {
            if (points.isEmpty()) {
                return Rectangle.EMPTY
            } else {
                return Rectangle(xMin, yMin, xMax - xMin, yMax - yMin)
            }
        }


    internal val points = mutableListOf<Pair<Vector2, Any?>>()
    internal fun insert(point: Vector2, owner: Any?) {
        points.add(Pair(point, owner))
        xMin = min(xMin, point.x)
        xMax = max(xMax, point.x)
        yMin = min(yMin, point.y)
        yMax = max(yMax, point.y)
    }

    internal fun squaredDistanceTo(query: Vector2): Double {
        val width = xMax - xMin
        val height = yMax - yMin
        val x = (xMin + xMax) / 2.0
        val y = (yMin + yMax) / 2.0
        val dx = max(abs(query.x - x) - width / 2, 0.0)
        val dy = max(abs(query.y - y) - height / 2, 0.0)
        return dx * dx + dy * dy
    }

    fun points() = sequence {
        for (point in points) {
            yield(point)
        }
    }
}

class HashGrid(val radius: Double) {
    private val cells = mutableMapOf<GridCoords, Cell>()
    fun cells() = sequence {
        for (cell in cells.values) {
            yield(cell)
        }
    }

    var size: Int = 0
        private set

    val cellSize = radius / sqrt(2.0)
    private inline fun coords(v: Vector2): GridCoords {
        val x = (v.x / cellSize).fastFloor()
        val y = (v.y / cellSize).fastFloor()
        return GridCoords(x, y)
    }

    fun points() = sequence {
        for (cell in cells.values) {
            for (point in cell.points) {
                yield(point)
            }
        }
    }

    fun random(random: Random = Random.Default): Vector2 {
        return cells.values.random(random).points.random().first
    }

    fun insert(point: Vector2, owner: Any? = null) {
        val gc = coords(point)
        val cell = cells.getOrPut(gc) { Cell(gc.x, gc.y, cellSize) }
        cell.insert(point, owner)
        size += 1
    }

    fun cell(query: Vector2): Cell? = cells[coords(query)]

    fun isFree(query: Vector2, ignoreOwners: Set<Any> = emptySet()): Boolean {
        val c = coords(query)
        if (cells[c] == null) {
            for (j in -2..2) {
                for (i in -2..2) {
                    if (i == 0 && j == 0) {
                        continue
                    }
                    val n = c.offset(i, j)
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
fun List<Vector2>.hashGrid(radius: Double): HashGrid {
    val grid = HashGrid(radius)
    for (point in this) {
        grid.insert(point)
    }
    return grid
}

/**
 * Return a list that only contains points at a minimum distance.
 * @param radius the minimum distance between any two points in the returned list
 */
fun List<Vector2>.filter(radius: Double): List<Vector2> {
    return if (size <= 1) {
        this
    } else {
        val grid = HashGrid(radius)
        for (point in this) {
            if (grid.isFree(point)) {
                grid.insert(point)
            }
        }
        grid.points().map { it.first }.toList()
    }
}