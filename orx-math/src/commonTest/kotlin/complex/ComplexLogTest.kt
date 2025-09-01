package org.openrndr.extra.math.complex

import kotlin.math.ln
import kotlin.test.Test
import kotlin.test.assertTrue

class ComplexLogTest {
    // Small epsilon for floating-point comparisons
    private val e = 1E-6

    /**
     * Tests the logarithm function with arbitrary base for complex numbers.
     * 
     * This test verifies that the logarithm of specific complex numbers
     * with different bases produces results that match the expected values
     * within an acceptable error tolerance.
     */
    @Test
    fun testLog() {
        // Test case 1: log_10(10) should be 1
        val z1 = Complex(10.0, 0.0)
        val result1 = log(z1, 10.0)
        assertTrue(result1.real in 1.0 - e..1.0 + e)
        assertTrue(result1.imaginary in 0.0 - e..0.0 + e)
        
        // Test case 2: log_2(8) should be 3
        val z2 = Complex(8.0, 0.0)
        val result2 = log(z2, 2.0)
        assertTrue(result2.real in 3.0 - e..3.0 + e)
        assertTrue(result2.imaginary in 0.0 - e..0.0 + e)
        
        // Test case 3: log_e(e) should be 1
        val z3 = Complex(kotlin.math.E, 0.0)
        val result3 = log(z3, kotlin.math.E)
        assertTrue(result3.real in 1.0 - e..1.0 + e)
        assertTrue(result3.imaginary in 0.0 - e..0.0 + e)
        
        // Test case 4: log_10(i) should be ln(i)/ln(10)
        val z4 = Complex(0.0, 1.0)
        val result4 = log(z4, 10.0)
        // ln(i) = ln(e^(i*π/2)) = i*π/2
        val expectedReal4 = 0.0
        val expectedImag4 = kotlin.math.PI / (2 * ln(10.0))
        assertTrue(result4.real in expectedReal4 - e..expectedReal4 + e)
        assertTrue(result4.imaginary in expectedImag4 - e..expectedImag4 + e)
        
        // Test case 5: log_2(-1) should be ln(-1)/ln(2) = i*π/ln(2)
        val z5 = Complex(-1.0, 0.0)
        val result5 = log(z5, 2.0)
        val expectedReal5 = 0.0
        val expectedImag5 = kotlin.math.PI / ln(2.0)
        assertTrue(result5.real in expectedReal5 - e..expectedReal5 + e)
        assertTrue(result5.imaginary in expectedImag5 - e..expectedImag5 + e)
    }
    
    /**
     * Tests the logarithm function with complex base for complex numbers.
     * 
     * This test verifies that the logarithm of specific complex numbers
     * with different complex bases produces results that match the expected values
     * within an acceptable error tolerance.
     */
    @Test
    fun testLogWithComplexBase() {
        // Test case 1: log_i(i) should be 1
        val z1 = Complex(0.0, 1.0)
        val base1 = Complex(0.0, 1.0)
        val result1 = log(z1, base1)
        assertTrue(result1.real in 1.0 - e..1.0 + e)
        assertTrue(result1.imaginary in 0.0 - e..0.0 + e)
        
        // Test case 2: log_(2+3i)(4+5i)
        val z2 = Complex(4.0, 5.0)
        val base2 = Complex(2.0, 3.0)
        val result2 = log(z2, base2)
        // Expected result is ln(4+5i) / ln(2+3i)
        val expectedResult2 = ln(z2) / ln(base2)
        assertTrue(result2.real in expectedResult2.real - e..expectedResult2.real + e)
        assertTrue(result2.imaginary in expectedResult2.imaginary - e..expectedResult2.imaginary + e)
        
        // Test case 3: log_(1+0i)(e+0i) should be 1
        val z3 = Complex(kotlin.math.E, 0.0)
        val base3 = Complex(kotlin.math.E, 0.0)
        val result3 = log(z3, base3)
        assertTrue(result3.real in 1.0 - e..1.0 + e)
        assertTrue(result3.imaginary in 0.0 - e..0.0 + e)
        
        // Test case 4: log_(2+0i)(8+0i) should be 3
        val z4 = Complex(8.0, 0.0)
        val base4 = Complex(2.0, 0.0)
        val result4 = log(z4, base4)
        assertTrue(result4.real in 3.0 - e..3.0 + e)
        assertTrue(result4.imaginary in 0.0 - e..0.0 + e)
    }
}