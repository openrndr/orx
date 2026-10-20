package org.openrndr.extra.math.eigenmodes

import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

class EigenSolverTest {

    @Test
    fun normalizeSignMakesLargestMagnitudeComponentPositive() {
        val v = doubleArrayOf(0.1, -0.9, 0.3)
        normalizeSign(v)
        assertTrue(v[1] > 0.0)
        assertTrue(abs(v[0] - -0.1) < 1e-12)
        assertTrue(abs(v[1] - 0.9) < 1e-12)
        assertTrue(abs(v[2] - -0.3) < 1e-12)
    }

    @Test
    fun normalizeSignIsANoOpWhenAlreadyCanonical() {
        val v = doubleArrayOf(0.1, 0.9, -0.3)
        val copy = v.copyOf()
        normalizeSign(v)
        for (i in v.indices) assertTrue(abs(v[i] - copy[i]) < 1e-12)
    }

    @Test
    fun eigenvectorSignIsStableAcrossDifferentSeeds() {
        // A 1-row (effectively 1D) strip has a strictly non-degenerate Dirichlet spectrum
        // (4*sin^2(p*pi/(2(n+1))) is strictly increasing in p), so comparing "the" eigenvector
        // for a given mode across seeds is actually well-posed here -- unlike e.g. a square
        // grid, whose symmetric mode pairs share an eigenvalue and so have no single canonical
        // eigenvector to agree on in the first place.
        val mask = BooleanArray(10 * 1) { true }
        val (laplacian, _) = buildDirichletLaplacian(mask, 10, 1)

        val a = laplacian.smallestEigenpairs(5, random = Random(1))
        val b = laplacian.smallestEigenpairs(5, random = Random(42))

        for (i in a.indices) {
            for (k in a[i].eigenvector.indices) {
                assertTrue(
                    abs(a[i].eigenvector[k] - b[i].eigenvector[k]) < 1e-4,
                    "mode $i, component $k: seed 1 gave ${a[i].eigenvector[k]}, seed 42 gave ${b[i].eigenvector[k]}"
                )
            }
        }
    }

    @Test
    fun knownEigenpairsAreIncludedAndDeflatedAgainst() {
        // A cycle graph Laplacian is singular with the constant vector as its null eigenvector;
        // seeding it should make mode 0 exactly that, and later modes orthogonal to it.
        val laplacian = buildCycleLaplacian(6)
        val constant = DoubleArray(6) { 1.0 / kotlin.math.sqrt(6.0) }

        val eigenpairs = laplacian.smallestEigenpairs(3, knownEigenpairs = listOf(Eigenpair(0.0, constant)))

        assertTrue(abs(eigenpairs[0].eigenvalue) < 1e-9)
        for (i in constant.indices) {
            assertTrue(abs(eigenpairs[0].eigenvector[i] - constant[i]) < 1e-9)
        }
        for (i in 1 until eigenpairs.size) {
            assertTrue(abs(dot(eigenpairs[i].eigenvector, constant)) < 1e-6)
        }
    }

    @Test
    fun countSmallerThanKnownEigenpairsTruncates() {
        val laplacian = buildCycleLaplacian(6)
        val constant = DoubleArray(6) { 1.0 / kotlin.math.sqrt(6.0) }
        val known = listOf(Eigenpair(0.0, constant), Eigenpair(1.0, DoubleArray(6)))

        val eigenpairs = laplacian.smallestEigenpairs(1, knownEigenpairs = known)
        assertTrue(eigenpairs.size == 1)
    }
}
