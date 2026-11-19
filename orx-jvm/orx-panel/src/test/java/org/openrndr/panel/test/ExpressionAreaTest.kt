package org.openrndr.panel.test

import org.openrndr.extra.expressions.ExpressionException
import org.openrndr.extra.expressions.ExpressionProblem.Kind
import org.openrndr.extra.expressions.checkExpression
import org.openrndr.extra.expressions.typed.checkTypedExpression
import org.openrndr.extra.expressions.typed.evaluateTypedExpression
import org.openrndr.math.Vector2
import org.openrndr.panel.ControlManager
import org.openrndr.panel.elements.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** the expression area colors expressions, and marks their problems with their messages as tooltips */
class ExpressionAreaTest : PanelTestFixture() {
    private fun ControlManager.frames(n: Int = 2) = repeat(n) { afterDraw(program.drawer, program) }

    /** the parts of [line] in the colors of the highlighter, as text and color */
    private fun colors(line: String) = Highlighters.expressions(line).map { line.substring(it.start, it.end) to it.color }

    @Test
    fun `the tokens of expressions are colored`() {
        assertEquals(
            listOf(
                "fun" to Highlighters.keywordColor, "f" to Highlighters.functionColor, "2.5" to Highlighters.numberColor,
                "PI" to Highlighters.typeColor, "sin" to Highlighters.functionColor, "\"" to Highlighters.stringColor,
                "ab" to Highlighters.stringColor, "\"" to Highlighters.stringColor
            ),
            colors("fun f(x) = x * 2.5 + PI + sin(x) + \"ab\"")
        )
        assertEquals(
            listOf(
                "val" to Highlighters.keywordColor, "1" to Highlighters.numberColor, "until" to Highlighters.keywordColor,
                "10" to Highlighters.numberColor, "step" to Highlighters.keywordColor, "2" to Highlighters.numberColor
            ),
            colors("val r = 1 until 10 step 2")
        )
        // what can not be read is not colored, nor does it stop the coloring
        assertEquals(listOf("1" to Highlighters.numberColor), colors("# 1 #"))
    }

    @Test
    fun `the problems of the text are marked, those of an evaluation until the text changes`() {
        lateinit var area: ExpressionArea
        val cm = panel {
            css("text-area { width: 400px; height: 200px }")
            layout { area = expressionArea { check = { checkExpression(it, setOf("a")) } } }
        }
        cm.frames()
        assertTrue(area.marks.isEmpty(), "an empty text has no problems")
        val changes = mutableListOf<Int>()
        area.problemsChanged.listen { changes.add(it.size) }

        area.value = "val b = a * r\nb + < 2"
        cm.frames()
        assertEquals(listOf(Kind.SYNTAX), area.problems.map { it.kind }, "a syntax error comes alone")
        area.value = "val b = a * r\nb + 2"
        assertEquals(listOf(Kind.UNKNOWN_VALUE to "r"), area.problems.map { it.kind to area.value.substring(it.start, it.end) })
        assertEquals(listOf(Mark(12, 13, "no value r", area.errorColor)), area.marks)
        assertEquals(TextArea.Position(1, 2), area.positionOf(16))
        assertEquals(16, area.offsetOf(TextArea.Position(1, 2)))

        // the problems of an evaluation, until the text changes
        area.check = { checkTypedExpression(it, setOf("a", "r")) }
        assertTrue(area.problems.isEmpty())
        area.value = "val b = a * r\nb.length"
        val e = assertFailsWith<ExpressionException> { evaluateTypedExpression(area.value, { mapOf("a" to 1.0, "r" to 2.0)[it] }) }
        area.report(e)
        assertEquals(e.problems, area.problems)
        assertEquals(area.errorColor, area.marks.single().color)
        area.value = "val b = a * r\nb"
        assertTrue(area.problems.isEmpty())
        assertTrue(changes.size >= 4, "$changes")
        cm.frames()
    }

    @Test
    fun `the message of a mark shows as the tooltip under the pointer`() {
        lateinit var area: ExpressionArea
        val cm = panel {
            css("text-area { width: 400px; height: 200px }")
            layout { area = expressionArea { check = { checkExpression(it, emptySet()) } } }
        }
        area.value = "an_unknown_value_with_a_long_name"
        cm.frames()
        val line = Vector2(area.layout.screenX + 120.0, area.layout.screenY + 12.0)
        cm.mousePosition = line
        assertEquals("no value an_unknown_value_with_a_long_name", area.tooltip)
        // below the text and after its end, on no mark
        cm.mousePosition = Vector2(area.layout.screenX + 120.0, area.layout.screenY + 150.0)
        assertNull(area.tooltip)
        area.value = "r"
        cm.frames()
        cm.mousePosition = line
        assertNull(area.tooltip)
    }
}
