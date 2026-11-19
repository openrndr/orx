package org.openrndr.panel.elements

import org.antlr.v4.kotlinruntime.CharStreams
import org.antlr.v4.kotlinruntime.CommonTokenStream
import org.antlr.v4.kotlinruntime.Token
import org.openrndr.color.ColorRGBa
import org.openrndr.events.Event
import org.openrndr.extra.expressions.ExpressionException
import org.openrndr.extra.expressions.ExpressionProblem
import org.openrndr.extra.expressions.builtInValues
import org.openrndr.extra.expressions.checkExpression
import org.openrndr.extra.expressions.parser.KeyLangLexer

/**
 * the colors of the tokens of [line], an expression or a line of a program of orx-expression-evaluator: keywords,
 * numbers, strings, built-in values and the names of functions, called and declared
 */
internal fun expressionHighlights(line: String): List<Highlight> {
    val lexer = KeyLangLexer(CharStreams.fromString(line)).also { it.removeErrorListeners() }
    val tokens = CommonTokenStream(lexer).also { it.fill() }.tokens.filter { it.type != Token.EOF && it.channel == Token.DEFAULT_CHANNEL }
    val result = mutableListOf<Highlight>()
    val keywords = setOf(
        KeyLangLexer.Tokens.VAL, KeyLangLexer.Tokens.FUN, KeyLangLexer.Tokens.STEP,
        KeyLangLexer.Tokens.RANGE_EXCLUSIVE_UNTIL, KeyLangLexer.Tokens.RANGE_DOWNTO
    )
    // the tokens of strings, from their opening to their closing quote
    val strings = setOf(
        KeyLangLexer.Tokens.STRING_OPEN, KeyLangLexer.Tokens.STRING_CLOSE, KeyLangLexer.Tokens.STRING_CONTENT,
        KeyLangLexer.Tokens.ESCAPE_STRING_DELIMITER, KeyLangLexer.Tokens.ESCAPE_SLASH, KeyLangLexer.Tokens.ESCAPE_NEWLINE,
        KeyLangLexer.Tokens.ESCAPE_SHARP
    )
    for ((i, t) in tokens.withIndex()) {
        val color = when (t.type) {
            in keywords -> Highlighters.keywordColor
            KeyLangLexer.Tokens.INTLIT, KeyLangLexer.Tokens.DECLIT -> Highlighters.numberColor
            in strings -> Highlighters.stringColor
            KeyLangLexer.Tokens.ID -> when {
                tokens.getOrNull(i + 1)?.type == KeyLangLexer.Tokens.LPAREN -> Highlighters.functionColor
                tokens.getOrNull(i - 1)?.type == KeyLangLexer.Tokens.FUN -> Highlighters.functionColor
                t.text?.replace("`", "") in builtInValues -> Highlighters.typeColor
                else -> null
            }
            else -> null
        } ?: continue
        result.add(Highlight(t.startIndex, t.stopIndex + 1, color))
    }
    return result
}

/**
 * An editor of the expressions of orx-expression-evaluator and orx-expression-evaluator-typed: a [TextArea] that
 * colors their tokens, see [Highlighters.expressions], and marks their [problems], with their messages as the tooltips
 * of the marks. [check] finds the problems of the text whenever it changes, [report] adds those of an evaluation.
 *
 * ```
 * expressionArea {
 *     check = { checkTypedExpression(it, setOf("t"), functions) }
 * }
 * ```
 */
class ExpressionArea : TextArea() {
    /**
     * the problems of the text that is not empty, without evaluating it: by default those of [checkExpression] without values; for typed
     * expressions [org.openrndr.extra.expressions.typed.checkTypedExpression], both with the values and the functions
     * that the expression is evaluated with
     */
    var check: (String) -> List<ExpressionProblem> = { checkExpression(it, emptySet()) }
        set(v) { field = v; update() }

    /** the color of the marks of problems, but those that are not supported */
    var errorColor = ColorRGBa.fromHex("#ff6b6b")
        set(v) { field = v; update() }

    /** the color of the marks of what is read but not evaluated, see [ExpressionProblem.Kind.UNSUPPORTED] */
    var warningColor = ColorRGBa.fromHex("#ffb454")
        set(v) { field = v; update() }

    /** the problems of [check] and of [report], those of [report] until the text changes */
    var problems: List<ExpressionProblem> = emptyList()
        private set

    /** the problems changed */
    val problemsChanged = Event<List<ExpressionProblem>>("expression-area-problems-changed")

    /** the problems that an evaluation reported, for the text they were reported for */
    private var reported: List<ExpressionProblem> = emptyList()

    /** adds the problems of [exception], which came from evaluating the text, until the text changes */
    fun report(exception: ExpressionException) = report(exception.problems)

    /** adds [problems], which came from evaluating the text, until the text changes */
    fun report(problems: List<ExpressionProblem>) {
        reported = problems
        update()
    }

    private fun update() {
        // an empty text has no problems yet
        val found = if (value.isBlank()) reported else (check(value) + reported).distinct().sortedBy { it.start }
        marks = found.map { problem ->
            val color = if (problem.kind == ExpressionProblem.Kind.UNSUPPORTED) warningColor else errorColor
            Mark(problem.start, problem.end, problem.message, color)
        }
        if (found != problems) {
            problems = found
            problemsChanged.trigger(found)
        }
    }

    init {
        highlighter = Highlighters.expressions
        valueChanged.listen {
            reported = emptyList()
            update()
        }
    }

    override fun close() {
        super.close()
        problemsChanged.close()
    }
}

fun Element.expressionArea(vararg classes: String, init: ExpressionArea.() -> Unit): ExpressionArea = initElement(classes, ExpressionArea(), init)
