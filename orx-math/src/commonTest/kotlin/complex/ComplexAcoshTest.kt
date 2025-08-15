package org.openrndr.extra.math.complex

import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals

class ComplexAcoshTest {
    
    @Test
    fun testAcoshOfOne() {
        val z = Complex(1.0, 0.0)
        val result = acosh(z)
        assertEquals(0.0, result.real, 1e-10)
        assertEquals(0.0, result.imaginary, 1e-10)
    }
    
    @Test
    fun testAcoshOfZero() {
        val z = Complex(0.0, 0.0)
        val result = acosh(z)
        assertEquals(0.0, result.real, 1e-10)
        assertEquals(PI/2, result.imaginary, 1e-10)
    }
    
    @Test
    fun testAcoshOfMinusOne() {
        val z = Complex(-1.0, 0.0)
        val result = acosh(z)
        assertEquals(0.0, result.real, 1e-10)
        assertEquals(PI, result.imaginary, 1e-10)
    }
    
    @Test
    fun testAcoshOfTwo() {
        val z = Complex(2.0, 0.0)
        val result = acosh(z)
        assertEquals(ln(2.0 + sqrt(3.0)), result.real, 1e-10)
        assertEquals(0.0, result.imaginary, 1e-10)
    }
    
    @Test
    fun testAcoshOfImaginaryUnit() {
        val z = Complex(0.0, 1.0)
        val result = acosh(z)
        assertEquals(0.8813735870195429, result.real, 1e-10)
        assertEquals(PI/2, result.imaginary, 1e-10)
    }
    
    @Test
    fun testAcoshOfComplexNumber() {
        val z = Complex(1.0, 1.0)
        val result = acosh(z)
        // Expected values calculated using external reference
        assertEquals(1.061275061, result.real, 1e-8)
        assertEquals(0.904556894, result.imaginary, 1e-8)
    }
}