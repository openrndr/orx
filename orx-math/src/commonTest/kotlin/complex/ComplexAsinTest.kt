package org.openrndr.extra.math.complex

import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals

class ComplexAsinTest {
    
    @Test
    fun testAsinOfZero() {
        val z = Complex(0.0, 0.0)
        val result = asin(z)
        assertEquals(0.0, result.real, 1e-10)
        assertEquals(0.0, result.imaginary, 1e-10)
    }
    
    @Test
    fun testAsinOfOne() {
        val z = Complex(1.0, 0.0)
        val result = asin(z)
        assertEquals(PI/2, result.real, 1e-10)
        assertEquals(0.0, result.imaginary, 1e-10)
    }
    
    @Test
    fun testAsinOfMinusOne() {
        val z = Complex(-1.0, 0.0)
        val result = asin(z)
        assertEquals(-PI/2, result.real, 1e-10)
        assertEquals(0.0, result.imaginary, 1e-10)
    }
    
    @Test
    fun testAsinOfImaginaryUnit() {
        val z = Complex(0.0, 1.0)
        val result = asin(z)
        assertEquals(0.0, result.real, 1e-10)
        assertEquals(0.881373587, result.imaginary, 1e-8) // approximately ln(1 + sqrt(2))
    }
    
    @Test
    fun testAsinOfComplexNumber() {
        val z = Complex(1.0, 1.0)
        val result = asin(z)
        // Expected values calculated using external reference
        assertEquals(0.666239432, result.real, 1e-8)
        assertEquals(1.061275061, result.imaginary, 1e-8)
    }
}