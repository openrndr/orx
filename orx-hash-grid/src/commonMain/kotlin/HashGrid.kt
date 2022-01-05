package org.openrndr.extra.hashgrid

import org.openrndr.math.Vector2
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

private fun Double.fastFloor(): Int {
    return if (this >= 0) this.toInt() else this.toInt() - 1
}

private data class GridCoords(val x: Int, val y: Int) {
    fun offset(i: Int, j: Int): GridCoords = copy(x = x + i, y = y + j)
}

private class Cell(
    var xMin: Double = Double.POSITIVE_INFINITY,
    var xMax: Double = Double.NEGATIVE_INFINITY,
    var yMin: Double = Double.POSITIVE_INFINITY,
    var yMax: Double = Double.NEGATIVE_INFINITY,
) {
    val points = mutableListOf<Vector2>()
    fun insert(point: Vector2) {
        points.add(point)
        xMin = min(xMin, point.x)
        xMax = max(xMax, point.x)
        yMin = min(yMin, point.y)
        yMax = max(yMax, point.y)
    }

    fun squaredDistanceTo(query: Vector2): Double {
        val width = xMax - xMin
        val height = yMax - yMin
        val x = (xMin + xMax) / 2.0
        val y = (yMin + yMax) / 2.0
        val dx = max(abs(query.x - x) - width / 2, 0.0)
        val dy = max(abs(query.y - y) - height / 2, 0.0)
        return dx * dx + dy * dy
    }
}

class HashGrid(val radius: Double) {
    private val cells = mutableMapOf<GridCoords, Cell>()
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

    fun random(random: Random = Random.Default) : Vector2 {
        return cells.values.random(random).points.random()
    }

    fun insert(point: Vector2) {
        val gc = coords(point)
        val cell = cells.getOrPut(gc) { Cell() }
        cell.insert(point)
    }

    fun isFree(query: Vector2): Boolean {
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
                            if (p.squaredDistanceTo(query) <= radius * radius) {
                                return false
                            }
                        }
                    }
                }
            }
            return true
        } else {
            return false
        }
    }
}