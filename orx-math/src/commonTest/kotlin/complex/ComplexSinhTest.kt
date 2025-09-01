package org.openrndr.extra.math.complex

import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals

class ComplexSinhTest {
    
    @Test
    fun testSinhOfZero() {
        val z = Complex(0.0, 0.0)
        val result = sinh(z)
        assertEquals(0.0, result.real, 1e-10)
        assertEquals(0.0, result.imaginary, 1e-10)
    }
    
    @Test
    fun testSinhOfOne() {
        val z = Complex(1.0, 0.0)
        val result = sinh(z)
        assertEquals(kotlin.math.sinh(1.0), result.real, 1e-10)
        assertEquals(0.0, result.imaginary, 1e-10)
    }
    
    @Test
    fun testSinhOfImaginaryUnit() {
        val z = Complex(0.0, 1.0)
        val result = sinh(z)
        assertEquals(0.0, result.real, 1e-10)
        assertEquals(kotlin.math.sin(1.0), result.imaginary, 1e-10)
    }
    
    @Test
    fun testSinhOfImaginaryPi() {
        val z = Complex(0.0, PI)
        val result = sinh(z)
        assertEquals(0.0, result.real, 1e-10)
        assertEquals(kotlin.math.sin(PI), result.imaginary, 1e-10)
    }
    
    @Test
    fun testSinhOfComplexNumber() {
        val z = Complex(1.0, 1.0)
        val result = sinh(z)
        // Expected values calculated using the formula: sinh(1+i) = sinh(1)cos(1) + i·cosh(1)sin(1)
        val expectedReal = kotlin.math.sinh(1.0) * kotlin.math.cos(1.0)
        val expectedImaginary = kotlin.math.cosh(1.0) * kotlin.math.sin(1.0)
        assertEquals(expectedReal, result.real, 1e-10)
        assertEquals(expectedImaginary, result.imaginary, 1e-10)
    }
}