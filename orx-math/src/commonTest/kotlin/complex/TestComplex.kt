package org.openrndr.extra.math.complex

import kotlin.math.PI
import kotlin.math.exp
import kotlin.test.Test
import kotlin.test.assertTrue

class TestComplex {
    // Small epsilon for floating-point comparisons
    private val e = 1E-6

    /**
     * Tests the arc cosine function for complex numbers.
     * 
     * This test verifies that the arc cosine of specific complex numbers
     * produces results that match the expected values within an acceptable
     * error tolerance.
     */
    @Test
    fun testAcos() {
        // Test case 1: acos(1) should be 0
        val z1 = Complex(1.0, 0.0)
        val result1 = acos(z1)
        assertTrue(result1.real in 0.0 - e..0.0 + e)
        assertTrue(result1.imaginary in 0.0 - e..0.0 + e)
        
        // Test case 2: acos(-1) should be PI
        val z2 = Complex(-1.0, 0.0)
        val result2 = acos(z2)
        assertTrue(result2.real in PI - e..PI + e)
        assertTrue(result2.imaginary in 0.0 - e..0.0 + e)
        
        // Test case 3: acos(0) should be PI/2
        val z3 = Complex(0.0, 0.0)
        val result3 = acos(z3)
        assertTrue(result3.real in PI/2 - e..PI/2 + e)
        assertTrue(result3.imaginary in 0.0 - e..0.0 + e)
        
        // Test case 4: acos(i) should be PI/2 - i*ln(1 + sqrt(2))
        val z4 = Complex(0.0, 1.0)
        val result4 = acos(z4)
        val expectedImag4 = -kotlin.math.ln(1.0 + kotlin.math.sqrt(2.0))
        assertTrue(result4.real in PI/2 - e..PI/2 + e)
        assertTrue(result4.imaginary in expectedImag4 - e..expectedImag4 + e)
        
        // Test case 5: acos(2) should be 0 + i*acosh(2)
        // For real x > 1, acos(x) = 0 + i*acosh(x) where acosh(x) = ln(x + sqrt(x^2 - 1))
        val z5 = Complex(2.0, 0.0)
        val result5 = acos(z5)
        val expectedImag5 = kotlin.math.ln(2.0 + kotlin.math.sqrt(3.0))
        assertTrue(result5.real in 0.0 - e..0.0 + e)
        assertTrue(result5.imaginary in expectedImag5 - e..expectedImag5 + e)
    }

    /**
     * Tests the exponential function for complex numbers.
     * 
     * This test verifies that the exponential of specific complex numbers
     * produces results that match the expected values within an acceptable
     * error tolerance.
     */
    @Test
    fun testExp() {
        // Test case 1: exp(0) should be 1
        val z1 = Complex(0.0, 0.0)
        val result1 = exp(z1)
        assertTrue(result1.real in 1.0 - e..1.0 + e)
        assertTrue(result1.imaginary in 0.0 - e..0.0 + e)
        
        // Test case 2: exp(1) should be e
        val z2 = Complex(1.0, 0.0)
        val result2 = exp(z2)
        val expectedReal2 = exp(1.0)
        assertTrue(result2.real in expectedReal2 - e..expectedReal2 + e)
        assertTrue(result2.imaginary in 0.0 - e..0.0 + e)
        
        // Test case 3: exp(i*PI) should be -1
        val z3 = Complex(0.0, PI)
        val result3 = exp(z3)
        assertTrue(result3.real in -1.0 - e..-1.0 + e)
        assertTrue(result3.imaginary in 0.0 - e..0.0 + e)
        
        // Test case 4: exp(i*PI/2) should be i
        val z4 = Complex(0.0, PI/2)
        val result4 = exp(z4)
        assertTrue(result4.real in 0.0 - e..0.0 + e)
        assertTrue(result4.imaginary in 1.0 - e..1.0 + e)
        
        // Test case 5: exp(1+i) = e^1 * (cos(1) + i*sin(1))
        val z5 = Complex(1.0, 1.0)
        val result5 = exp(z5)
        val expectedReal5 = exp(1.0) * kotlin.math.cos(1.0)
        val expectedImag5 = exp(1.0) * kotlin.math.sin(1.0)
        assertTrue(result5.real in expectedReal5 - e..expectedReal5 + e)
        assertTrue(result5.imaginary in expectedImag5 - e..expectedImag5 + e)
    }
}