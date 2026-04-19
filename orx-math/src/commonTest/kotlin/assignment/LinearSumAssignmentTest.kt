package org.openrndr.extra.math.assignment

import org.openrndr.extra.math.matrix.Matrix
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class LinearSumAssignmentTest {

    @Test
    fun testSquareAssignment() {
        val costMatrix = Matrix(3, 3)
        costMatrix[0, 0] = 1.0; costMatrix[0, 1] = 2.0; costMatrix[0, 2] = 3.0
        costMatrix[1, 0] = 3.0; costMatrix[1, 1] = 1.0; costMatrix[1, 2] = 2.0
        costMatrix[2, 0] = 2.0; costMatrix[2, 1] = 3.0; costMatrix[2, 2] = 1.0

        val (rowInd, colInd) = linearSumAssignment(costMatrix)

        assertContentEquals(intArrayOf(0, 1, 2), rowInd)
        assertContentEquals(intArrayOf(0, 1, 2), colInd)

        var totalCost = 0.0
        for (i in rowInd.indices) {
            totalCost += costMatrix[rowInd[i], colInd[i]]
        }
        assertEquals(3.0, totalCost)
    }

    @Test
    fun testRectangularAssignmentMoreCols() {
        // 2x3 matrix
        val costMatrix = Matrix(2, 3)
        costMatrix[0, 0] = 10.0; costMatrix[0, 1] = 1.0; costMatrix[0, 2] = 10.0
        costMatrix[1, 0] = 1.0; costMatrix[1, 1] = 10.0; costMatrix[1, 2] = 10.0

        val (rowInd, colInd) = linearSumAssignment(costMatrix)

        assertContentEquals(intArrayOf(0, 1), rowInd)
        assertContentEquals(intArrayOf(1, 0), colInd)

        var totalCost = 0.0
        for (i in rowInd.indices) {
            totalCost += costMatrix[rowInd[i], colInd[i]]
        }
        assertEquals(2.0, totalCost)
    }

    @Test
    fun testRectangularAssignmentMoreRows() {
        // 3x2 matrix
        val costMatrix = Matrix(3, 2)
        costMatrix[0, 0] = 1.0; costMatrix[0, 1] = 10.0
        costMatrix[1, 0] = 10.0; costMatrix[1, 1] = 1.0
        costMatrix[2, 0] = 10.0; costMatrix[2, 1] = 10.0

        val (rowInd, colInd) = linearSumAssignment(costMatrix)

        assertContentEquals(intArrayOf(0, 1), rowInd)
        assertContentEquals(intArrayOf(0, 1), colInd)

        var totalCost = 0.0
        for (i in rowInd.indices) {
            totalCost += costMatrix[rowInd[i], colInd[i]]
        }
        assertEquals(2.0, totalCost)
    }

    @Test
    fun testScipyExample() {
        // Example from SciPy documentation
        val costMatrix = Matrix(3, 3)
        costMatrix[0, 0] = 4.0; costMatrix[0, 1] = 1.0; costMatrix[0, 2] = 3.0
        costMatrix[1, 0] = 2.0; costMatrix[1, 1] = 0.0; costMatrix[1, 2] = 5.0
        costMatrix[2, 0] = 3.0; costMatrix[2, 1] = 2.0; costMatrix[2, 2] = 2.0

        val (rowInd, colInd) = linearSumAssignment(costMatrix)

        assertContentEquals(intArrayOf(0, 1, 2), rowInd)
        assertContentEquals(intArrayOf(1, 0, 2), colInd)

        var totalCost = 0.0
        for (i in rowInd.indices) {
            totalCost += costMatrix[rowInd[i], colInd[i]]
        }
        assertEquals(1.0 + 2.0 + 2.0, totalCost)
    }
}
