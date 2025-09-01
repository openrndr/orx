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

/**
 * Represents a cell in a 2D space, defined by its position and size.
 *
 * @property x The x-coordinate of the cell in the grid.
 * @property y The y-coordinate of the cell in the grid.
 * @property cellSize The size of the cell along each axis.
 */
class Cell(val x: Int, val y: Int, val cellSize: Double) {
    var xMin: Double = Double.POSITIVE_INFINITY
        private set
    var xMax: Double = Double.NEGATIVE_INFINITY
        private set
    var yMin: Double = Double.POSITIVE_INFINITY
        private set
    var yMax: Double = Double.NEGATIVE_INFINITY
        private set

    /**
     * Calculates and returns the rectangular bounds of the cell in the 2D grid.
     * The bounds are represented as a rectangle with its top-left position and size derived
     * from the cell's position (`x`, `y`) and `cellSize`.
     */
    val bounds: Rectangle
        get() {
            return Rectangle(x * cellSize, y * cellSize, cellSize, cellSize)
        }

    /**
     * Computes the bounds of the content within the cell, considering the points stored in it.
     * If no points are present in the cell, the bounds will be represented as an empty rectangle.
     * Otherwise, the bounds are determined by the minimum and maximum x and y coordinates
     * among the points in the cell.
     */
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

    /**
     * Generates a sequence of points contained within the current cell.
     * Iterates over the points stored in the cell and yields each point one by one.
     *
     * @return A sequence of points in the cell.
     */
    fun points() = sequence {
        for (point in points) {
            yield(point)
        }
    }
}

/**
 * Represents a 2D spatial hash grid used for efficiently managing and querying points in a sparse space.
 *
 * @property radius The maximum distance between points for them to be considered neighbors.
 */
class HashGrid(val radius: Double) {
    private val cells = mutableMapOf<GridCoords, Cell>()

    /**
     * Returns a sequence of all cells stored in the grid.
     * Iterates through the values in the internal `cells` map and yields each cell.
     */
    fun cells() = sequence {
        for (cell in cells.values) {
            yield(cell)
        }
    }

    /**
     * Represents the total number of elements (points or data) that are currently stored in the grid.
     *
     * This property is managed internally and reflects the current size of the grid data structure.
     * It cannot be modified directly from outside the class.
     */
    var size: Int = 0
        private set

    /**
     * Represents the size of a single cell in the hash grid.
     *
     * Computed as the radius divided by the square root of 2.
     * This value determines the spatial resolution of each cell in the grid.
     */
    val cellSize = radius / sqrt(2.0)
    private fun coords(v: Vector2): GridCoords {
        val x = (v.x / cellSize).fastFloor()
        val y = (v.y / cellSize).fastFloor()
        return GridCoords(x, y)
    }

    /**
     * Generates a sequence of all points stored within the grid.
     *
     * Iterates through each cell in the grid's `cells` map, yielding all points
     * contained within each cell.
     *
     * @return A sequence of points from all cells in the grid.
     */
    fun points() = sequence {
        for (cell in cells.values) {
            for (point in cell.points) {
                yield(point)
            }
        }
    }

    /**
     * Selects a random point from the grid using the provided random number generator.
     *
     * @param random The random number generator to use. Defaults to `Random.Default`.
     * @return A randomly selected point, represented as a `Vector2`, from the grid's cells.
     */
    fun random(random: Random = Random.Default): Vector2 {
        return cells.values.random(random).points.random().first
    }

    /**
     * Inserts a point into the grid, associating it with an owner if provided.
     * The method calculates the grid cell corresponding to the provided point and inserts
     * the point into that cell. If the cell does not exist, it is created.
     *
     * @param point The point to insert, represented as a `Vector2` object.
     * @param owner An optional object to associate with the point. Defaults to `null` if no owner is specified.
     */
    fun insert(point: Vector2, owner: Any? = null) {
        val gc = coords(point)
        val cell = cells.getOrPut(gc) { Cell(gc.x, gc.y, cellSize) }
        cell.insert(point, owner)
        size += 1
    }

    /**
     * Retrieves the cell corresponding to the given query point in the grid.
     * The method calculates the grid coordinates for the query point and returns
     * the cell found at those coordinates, if it exists.
     *
     * @param query The point in 2D space, represented as a `Vector2`, for which
     * to retrieve the corresponding cell.
     * @return The `Cell` corresponding to the given query point, or `null` if
     * no cell exists at the calculated coordinates.
     */
    fun cell(query: Vector2): Cell? = cells[coords(query)]

    /**
     * Checks if a specific query point in 2D space is free from any nearby points or owners,
     * according to the internal grid structure and other constraints.
     *
     * @param query The 2D point represented as a Vector2 to check for available space.
     * @param ignoreOwners A set of owners to be ignored while checking for nearby points. Defaults to an empty set.
     * @return `true` if the query point is free, `false` otherwise.
     */
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