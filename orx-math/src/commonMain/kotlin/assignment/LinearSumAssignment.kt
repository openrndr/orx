package org.openrndr.extra.math.assignment

import org.openrndr.extra.math.matrix.Matrix

/**
 * Solves the linear sum assignment problem (also known as the minimum weight matching in bipartite graphs).
 *
 * This implementation uses a modified Jonker-Volgenant algorithm with no initialization,
 * based on the shortest augmenting path approach.
 *
 * @param costMatrix A [Matrix] of costs, where costMatrix[i, j] is the cost of assigning row i to column j.
 * @return An array containing two [IntArray]s: the first one contains row indices, and the second one contains
 * corresponding column indices of the optimal assignment.
 */
fun linearSumAssignment(costMatrix: Matrix): Array<IntArray> {
    val transposed = costMatrix.cols < costMatrix.rows
    val C = if (transposed) costMatrix.transposed() else costMatrix

    val rowCount = C.rows
    val columnCount = C.cols

    // dual variables
    val u = DoubleArray(rowCount)
    val v = DoubleArray(columnCount)

    // assignment
    val row2col = IntArray(rowCount) { -1 }
    val col2row = IntArray(columnCount) { -1 }

    for (row in 0 until rowCount) {
        val minSlack = DoubleArray(columnCount) { Double.POSITIVE_INFINITY }
        val visitedCol = BooleanArray(columnCount)
        val parentCol = IntArray(columnCount) { -1 }

        var i = row
        var j = -1

        while (i != -1) {
            visitedCol.indices.forEach { k ->
                if (!visitedCol[k]) {
                    val slack = C[i, k] - u[i] - v[k]
                    if (slack < minSlack[k]) {
                        minSlack[k] = slack
                        parentCol[k] = j
                    }
                }
            }

            var delta = Double.POSITIVE_INFINITY
            var nextJ = -1
            for (k in 0 until columnCount) {
                if (!visitedCol[k] && minSlack[k] < delta) {
                    delta = minSlack[k]
                    nextJ = k
                }
            }

            u[row] += delta
            for (k in 0 until columnCount) {
                if (visitedCol[k]) {
                    u[col2row[k]] += delta
                    v[k] -= delta
                } else {
                    minSlack[k] -= delta
                }
            }

            j = nextJ
            visitedCol[j] = true
            i = col2row[j]
        }

        while (j != -1) {
            val prevJ = parentCol[j]
            val r = if (prevJ == -1) row else col2row[prevJ]
            col2row[j] = r
            row2col[r] = j
            j = prevJ
        }
    }

    return if (!transposed) {
        val rows = IntArray(rowCount) { it }
        val cols = IntArray(rowCount) { row2col[it] }
        arrayOf(rows, cols)
    } else {
        val rows = IntArray(columnCount) { col2row[it] }
        val cols = IntArray(columnCount) { it }
        // Filter out unassigned rows if it was transposed and rectangular
        val assignedRows = mutableListOf<Int>()
        val assignedCols = mutableListOf<Int>()
        for (idx in rows.indices) {
            if (rows[idx] != -1) {
                assignedRows.add(rows[idx])
                assignedCols.add(cols[idx])
            }
        }
        // sort by row index
        val pairs = assignedRows.zip(assignedCols).sortedBy { it.first }
        arrayOf(pairs.map { it.first }.toIntArray(), pairs.map { it.second }.toIntArray())
    }
}