package org.openrndr.extra.math.complex

import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals

class ComplexCoshTest {
    
    @Test
    fun testCoshOfZero() {
        val z = Complex(0.0, 0.0)
        val result = cosh(z)
        assertEquals(1.0, result.real, 1e-10)
        assertEquals(0.0, result.imaginary, 1e-10)
    }
    
    @Test
    fun testCoshOfOne() {
        val z = Complex(1.0, 0.0)
        val result = cosh(z)
        assertEquals(kotlin.math.cosh(1.0), result.real, 1e-10)
        assertEquals(0.0, result.imaginary, 1e-10)
    }
    
    @Test
    fun testCoshOfImaginaryUnit() {
        val z = Complex(0.0, 1.0)
        val result = cosh(z)
        assertEquals(kotlin.math.cos(1.0), result.real, 1e-10)
        assertEquals(0.0, result.imaginary, 1e-10)
    }
    
    @Test
    fun testCoshOfImaginaryPi() {
        val z = Complex(0.0, PI)
        val result = cosh(z)
        assertEquals(kotlin.math.cos(PI), result.real, 1e-10)
        assertEquals(0.0, result.imaginary, 1e-10)
    }
    
    @Test
    fun testCoshOfComplexNumber() {
        val z = Complex(1.0, 1.0)
        val result = cosh(z)
        // Expected values calculated using the formula: cosh(1+i) = cosh(1)cos(1) + i·sinh(1)sin(1)
        val expectedReal = kotlin.math.cosh(1.0) * kotlin.math.cos(1.0)
        val expectedImaginary = kotlin.math.sinh(1.0) * kotlin.math.sin(1.0)
        assertEquals(expectedReal, result.real, 1e-10)
        assertEquals(expectedImaginary, result.imaginary, 1e-10)
    }
}