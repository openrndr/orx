package org.openrndr.extra.hashgrid
import org.openrndr.math.Vector3
import org.openrndr.shape.Box
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

/**
 * Represents a 3D cell with a fixed size in a spatial hash grid structure. A `Cell3D` is aligned
 * along a grid using its integer coordinates and supports operations to manage points within
 * its bounds, calculate distances to a query point, and retrieve its own bounding boxes.
 *
 * @property x The x-coordinate of the cell within the grid.
 * @property y The y-coordinate of the cell within the grid.
 * @property z The z-coordinate of the cell within the grid.
 * @property cellSize The size of the cell in all dimensions.
 */
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

    /**
     * Represents the 3D bounding box of the cell.
     *
     * The bounds are calculated based on the cell's position (`x`, `y`, `z`) and
     * the uniform size of the cell (`cellSize`). It defines a cuboid in 3D space
     * with its origin at `(x * cellSize, y * cellSize, z * cellSize)` and dimensions
     * defined by `cellSize` along all three axes.
     *
     * @return A `Box` representing the spatial boundary of the cell.
     */
    val bounds: Box
        get() {
            return Box(Vector3(x * cellSize, y * cellSize, z * cellSize), cellSize, cellSize, cellSize)
        }

    /**
     * Provides the bounding 3D box that contains all the points within the cell.
     * If the `points` collection is empty, it returns an empty box. Otherwise,
     * it calculates the bounding box based on the minimum and maximum coordinates
     * of the stored points (`xMin`, `xMax`, `yMin`, `yMax`, `zMin`, `zMax`).
     */
    val contentBounds: Box
        get() {
            return if (points.isEmpty()) {
                Box.EMPTY
            } else {
                Box(Vector3(xMin, yMin, zMin), xMax - xMin, yMax - yMin, zMax - zMin)
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

    /**
     * Generates a sequence of all the points stored in the `points` collection.
     *
     * This method iterates over the `points` collection and yields each element.
     * Useful for lazily accessing the points in the order they are stored.
     *
     * @return A sequence of points contained within the `points` collection.
     */
    fun points() = sequence {
        for (point in points) {
            yield(point)
        }
    }
}

/**
 * Represents a 3D Hash Grid structure used for spatial partitioning of points in 3D space.
 * This structure organizes points into grid-based cells, enabling efficient spatial querying
 * and insertion operations.
 *
 * @property radius The radius used to determine proximity checks within the grid.
 *                  Points are considered neighbors if their spatial distance is less than or equal to this radius.
 */
class HashGrid3D(val radius: Double) {
    private val cells = mutableMapOf<GridCoords3D, Cell3D>()


    /**
     * Returns a sequence of all the cells present in the hash grid.
     * Each cell is yielded individually from the internal mapping.
     */
    fun cells() = sequence {
        for (cell in cells.values) {
            yield(cell)
        }
    }

    /**
     * Represents the total number of points currently stored in the hash grid.
     * This property is incremented whenever a new point is inserted into the grid.
     * It's read-only for external access and cannot be modified outside the class.
     */
    var size: Int = 0
        private set

    /**
     * The size of a single cell in the 3D hash grid.
     *
     * The cell size is computed as the radius of the grid divided by the square root of 3,
     * which ensures that the cell dimensions are scaled appropriately in a 3D space.
     * This value influences the spatial resolution of the grid and determines
     * how points are grouped into cells during computations such as insertion or querying.
     */
    val cellSize = radius / sqrt(3.0)
    private fun coords(v: Vector3): GridCoords3D {
        val x = (v.x / cellSize).fastFloor()
        val y = (v.y / cellSize).fastFloor()
        val z = (v.z / cellSize).fastFloor()
        return GridCoords3D(x, y, z)
    }

    /**
     * Returns a sequence of all points contained in the hash grid.
     *
     * Iterates over all cells in the grid and yields each contained point.
     * Each point is represented as a value yielded by the sequence.
     *
     * @return A sequence of all points stored in the hash grid.
     */
    fun points() = sequence {
        for (cell in cells.values) {
            for (point in cell.points) {
                yield(point)
            }
        }
    }

    /**
     * Selects a random 3D vector from the points stored in the hash grid.
     *
     * @param random A random number generator to use for selection. Defaults to `Random.Default`.
     * @return A randomly selected `Vector3` from the hash grid.
     */
    fun random(random: Random = Random.Default): Vector3 {
        return cells.values.random(random).points.random().first
    }

    fun insert(point: Vector3, owner: Any? = null) {
        val gc = coords(point)
        val cell = cells.getOrPut(gc) { Cell3D(gc.x, gc.y, gc.z, cellSize) }
        cell.insert(point, owner)
        size += 1
    }

    /**
     * Retrieves the 3D cell corresponding to the given query point in the spatial hash grid.
     *
     * This method computes the grid coordinates of the query vector and attempts to fetch
     * the corresponding cell from the internal cell mapping.
     *
     * @param query A `Vector3` object representing the point used to locate the corresponding cell.
     * @return A `Cell3D` object if a cell exists for the given query point, or `null` if no such cell is found.
     */
    fun cell(query: Vector3): Cell3D? = cells[coords(query)]

    /**
     * Determines whether a specific point in the 3D grid is free, considering the proximity
     * to other points and optionally ignoring specified owners.
     *
     * @param query The `Vector3` representing the point to check for availability.
     * @param ignoreOwners A set of owners to ignore during the proximity check. Default is an empty set.
     * @return `true` if the point is considered free or not occupied; otherwise, `false`.
     */
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