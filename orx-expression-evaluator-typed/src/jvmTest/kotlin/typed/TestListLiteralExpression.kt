package typed

import org.junit.jupiter.api.Assertions.assertEquals
import org.openrndr.extra.expressions.typed.evaluateTypedExpression
import kotlin.test.Test
import kotlin.test.assertTrue

class TestListLiteralExpression {

    @Test
    fun testSimpleList() {
        val r = evaluateTypedExpression("[0, 1, 2]")
        assertTrue(r is List<*>)
        assertEquals(3, r.size )
    }

    @Test
    fun testRangesList() {
        val r = evaluateTypedExpression("[0..1, 1..2, 2..3]")
        assertTrue(r is List<*>)
        assertEquals(3, r.size )
    }

}