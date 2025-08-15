package org.openrndr.extra.math.complex

import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals

class ComplexTanhTest {
    
    @Test
    fun testTanhOfZero() {
        val z = Complex(0.0, 0.0)
        val result = tanh(z)
        assertEquals(0.0, result.real, 1e-10)
        assertEquals(0.0, result.imaginary, 1e-10)
    }
    
    @Test
    fun testTanhOfOne() {
        val z = Complex(1.0, 0.0)
        val result = tanh(z)
        assertEquals(kotlin.math.tanh(1.0), result.real, 1e-10)
        assertEquals(0.0, result.imaginary, 1e-10)
    }
    
    @Test
    fun testTanhOfImaginaryUnit() {
        val z = Complex(0.0, 1.0)
        val result = tanh(z)
        assertEquals(0.0, result.real, 1e-10)
        assertEquals(kotlin.math.tan(1.0), result.imaginary, 1e-10)
    }
    
    @Test
    fun testTanhOfImaginaryPi() {
        val z = Complex(0.0, PI)
        val result = tanh(z)
        assertEquals(0.0, result.real, 1e-10)
        assertEquals(kotlin.math.tan(PI), result.imaginary, 1e-10)
    }
    
    @Test
    fun testTanhOfComplexNumber() {
        val z = Complex(1.0, 1.0)
        val result = tanh(z)
        
        // Calculate expected values using the formula: tanh(z) = sinh(z) / cosh(z)
        val sinhZ = sinh(z)
        val coshZ = cosh(z)
        val expected = sinhZ / coshZ
        
        assertEquals(expected.real, result.real, 1e-10)
        assertEquals(expected.imaginary, result.imaginary, 1e-10)
    }
    
    @Test
    fun testTanhIdentity() {
        // Test the identity: tanh(z) = sinh(z) / cosh(z)
        val z = Complex(2.0, 3.0)
        
        // Calculate using our tanh implementation
        val result = tanh(z)
        
        // Calculate using the identity
        val sinhZ = sinh(z)
        val coshZ = cosh(z)
        val expected = sinhZ / coshZ
        
        assertEquals(expected.real, result.real, 1e-10)
        assertEquals(expected.imaginary, result.imaginary, 1e-10)
    }
}