package org.openrndr.extra.math.complex

import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals

class ComplexAtanhTest {
    
    @Test
    fun testAtanhOfZero() {
        val z = Complex(0.0, 0.0)
        val result = atanh(z)
        assertEquals(0.0, result.real, 1e-10)
        assertEquals(0.0, result.imaginary, 1e-10)
    }
    
    @Test
    fun testAtanhOfHalf() {
        val z = Complex(0.5, 0.0)
        val result = atanh(z)
        // atanh(0.5) = 0.5493061443340548
        assertEquals(0.5493061443340548, result.real, 1e-10)
        assertEquals(0.0, result.imaginary, 1e-10)
    }
    
    @Test
    fun testAtanhOfImaginaryUnit() {
        val z = Complex(0.0, 1.0)
        val result = atanh(z)
        assertEquals(0.0, result.real, 1e-10)
        // atanh(i) = i*π/2
        assertEquals(PI/2, result.imaginary, 1e-10)
    }
    
    @Test
    fun testAtanhOfNegativeImaginaryUnit() {
        val z = Complex(0.0, -1.0)
        val result = atanh(z)
        assertEquals(0.0, result.real, 1e-10)
        // atanh(-i) = -i*π/2
        assertEquals(-PI/2, result.imaginary, 1e-10)
    }
    
    @Test
    fun testAtanhOfComplexNumber() {
        val z = Complex(0.5, 0.5)
        val result = atanh(z)
        
        // Calculate expected values using the formula: atanh(z) = 0.5 * ln((1+z)/(1-z))
        val one = Complex(1.0, 0.0)
        val numerator = one + z
        val denominator = one - z
        val fraction = numerator / denominator
        val expected = ln(fraction) * Complex(0.5, 0.0)
        
        assertEquals(expected.real, result.real, 1e-10)
        assertEquals(expected.imaginary, result.imaginary, 1e-10)
    }
    
    @Test
    fun testAtanhIdentity() {
        // Test the identity: tanh(atanh(z)) = z for |z| < 1
        val z = Complex(0.3, 0.4)
        
        // Calculate atanh(z)
        val atanhZ = atanh(z)
        
        // Calculate tanh(atanh(z))
        val result = tanh(atanhZ)
        
        assertEquals(z.real, result.real, 1e-10)
        assertEquals(z.imaginary, result.imaginary, 1e-10)
    }
}