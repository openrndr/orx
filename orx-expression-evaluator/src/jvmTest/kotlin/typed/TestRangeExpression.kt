package typed

import org.junit.jupiter.api.Assertions.assertEquals
import org.openrndr.extra.expressions.typed.evaluateTypedExpression
import kotlin.test.Test
import kotlin.test.assertTrue

class TestRangeExpression {
    @Test
    fun testRangeInclusive() {
        val r = evaluateTypedExpression("(0..10)")
        assertTrue(r is List<*>)
        assertEquals(11, r.size)
    }

    @Test
    fun testRangeDownTo() {
        val r = evaluateTypedExpression("(10 downTo 0)")
        assertTrue(r is List<*>)
        assertEquals(11, r.size)
        assertEquals(0.0, r.last())
    }

    @Test
    fun testRangeExclusive() {
        val r = evaluateTypedExpression("(0..<10)")
        assertTrue(r is List<*>)
        assertEquals(10, r.size)
    }

    @Test
    fun testRangeInclusivePrecedenceRight() {
        val r = evaluateTypedExpression("(0..10+2)")
        assertTrue(r is List<*>)
        assertEquals(13, r.size)
    }

    @Test
    fun testRangeInclusivePrecedenceLeft() {
        val r = evaluateTypedExpression("1+2..10")
        assertTrue(r is List<*>)
        assertEquals(8, r.size)
    }

    @Test
    fun testRangeInclusivePrecedenceLeftRight() {
        val r = evaluateTypedExpression("1+2..10+2")
        assertTrue(r is List<*>)
        assertEquals(10, r.size)
    }

    @Test
    fun testRangeUntilPrecedenceLeftRight() {
        val r = evaluateTypedExpression("1+2 until 10+2")
        assertTrue(r is List<*>)
        assertEquals(9, r.size)
    }

    @Test
    fun testRangeUntilPrecedenceLeftRightMap() {
        val r = evaluateTypedExpression("(1+2 until 10+2).map { x -> x * 2 }")
        assertTrue(r is List<*>)
        assertEquals(9, r.size)
    }

    @Test
    fun testRangeExclusiveStep() {
        val r = evaluateTypedExpression("(0..10 step 2)")
        assertTrue(r is List<*>)
        assertEquals(6, r.size)
    }
}