package polynomials

import Polynomial
import polynomialRoots
import kotlin.test.Test
import kotlin.test.assertEquals

class TestPolynomial {
    @Test
    fun testPolynomial() {
        // test for x^3 - 1 = 0
        val p = Polynomial(doubleArrayOf(-1.0, 0.0, 0.0, 1.0))
        val roots = polynomialRoots(p)
        assertEquals(1, roots.size)
        assertEquals(roots[0], 1.0)

        // test for x^9 - 1 = 0
        val p9 = Polynomial(doubleArrayOf(-1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0))
        val roots9 = polynomialRoots(p)
        assertEquals(1, roots9.size)
        assertEquals(roots[0], 1.0)

    }
}