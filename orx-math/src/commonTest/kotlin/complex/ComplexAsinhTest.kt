package org.openrndr.extra.math.complex

import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals

class ComplexAsinhTest {
    
    @Test
    fun testAsinhOfZero() {
        val z = Complex(0.0, 0.0)
        val result = asinh(z)
        assertEquals(0.0, result.real, 1e-10)
        assertEquals(0.0, result.imaginary, 1e-10)
    }
    
    @Test
    fun testAsinhOfOne() {
        val z = Complex(1.0, 0.0)
        val result = asinh(z)
        // Expected value is ln(1 + sqrt(2))
        assertEquals(ln(1.0 + sqrt(2.0)), result.real, 1e-10)
        assertEquals(0.0, result.imaginary, 1e-10)
    }
    
    @Test
    fun testAsinhOfMinusOne() {
        val z = Complex(-1.0, 0.0)
        val result = asinh(z)
        // Expected value is -ln(1 + sqrt(2))
        assertEquals(-ln(1.0 + sqrt(2.0)), result.real, 1e-10)
        assertEquals(0.0, result.imaginary, 1e-10)
    }
    
    @Test
    fun testAsinhOfImaginaryUnit() {
        val z = Complex(0.0, 1.0)
        val result = asinh(z)
        assertEquals(0.0, result.real, 1e-10)
        assertEquals(PI/2, result.imaginary, 1e-10)
    }
    
    @Test
    fun testAsinhOfNegativeImaginaryUnit() {
        val z = Complex(0.0, -1.0)
        val result = asinh(z)
        assertEquals(0.0, result.real, 1e-10)
        assertEquals(-PI/2, result.imaginary, 1e-10)
    }
    
    @Test
    fun testAsinhOfComplexNumber() {
        val z = Complex(1.0, 1.0)
        val result = asinh(z)
        // Expected values calculated using external reference
        assertEquals(1.061275061, result.real, 1e-8)
        assertEquals(0.666239432, result.imaginary, 1e-8)
    }
}