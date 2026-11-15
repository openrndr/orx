package org.openrndr.extra.textwriter.math

/** how an operator is laid out: the space around it in em, and whether it stretches or is a large operator */
internal data class OperatorProperties(
    val leftSpaceInEm: Double,
    val rightSpaceInEm: Double,
    val stretchy: Boolean = false,
    val symmetric: Boolean = false,
    val largeOperator: Boolean = false,
    val movableLimits: Boolean = false
)

/**
 * A part of the operator dictionary of MathML Core: the common fences, separators, relations, binary and large
 * operators. Operators it does not have are spaced as relations.
 */
internal object MathOperators {
    private const val THIN = 3 / 18.0
    private const val MEDIUM = 4 / 18.0
    private const val THICK = 5 / 18.0

    private val fences = setOf("(", ")", "[", "]", "{", "}", "|", "‖", "⟨", "⟩", "⌈", "⌉", "⌊", "⌋")
    private val relations = setOf(
        "=", "<", ">", "≤", "≥", "≠", "≈", "≡", "∼", "≃", "≅", "∝", "→", "←", "↔", "⇒", "⇐", "⇔", "↦",
        "∈", "∉", "∋", "⊂", "⊃", "⊆", "⊇", ":", "≔", "≪", "≫", "⊥", "∣"
    )
    private val binary = setOf("+", "-", "−", "±", "∓", "×", "÷", "·", "⋅", "∘", "∗", "∪", "∩", "∧", "∨", "⊕", "⊗", "⊖", "∖", "/")
    private val largeWithLimits = setOf("∑", "∏", "∐", "⋃", "⋂", "⋁", "⋀", "⨁", "⨂", "⨀", "lim", "max", "min", "sup", "inf")
    private val integrals = setOf("∫", "∬", "∭", "∮", "∯", "∰")
    private val separators = setOf(",", ";")
    private val invisible = setOf("⁡", "⁢", "⁣", "⁤")

    /** the properties of [text] in [form], `"prefix"`, `"infix"` or `"postfix"` */
    fun properties(text: String, form: String): OperatorProperties = when {
        text in invisible -> OperatorProperties(0.0, 0.0)
        text in fences -> OperatorProperties(0.0, 0.0, stretchy = true, symmetric = true)
        text in separators -> OperatorProperties(0.0, THIN)
        text in largeWithLimits && text.length == 1 -> OperatorProperties(THIN / 3.0, THIN, largeOperator = true, movableLimits = true, symmetric = true)
        // functions like lim are written as operators, they have limits but are not enlarged
        text in largeWithLimits -> OperatorProperties(THIN / 3.0, THIN, movableLimits = true)
        text in integrals -> OperatorProperties(THIN / 3.0, THIN / 3.0, largeOperator = true, symmetric = true)
        text in binary && form == "infix" -> OperatorProperties(MEDIUM, MEDIUM)
        text in binary -> OperatorProperties(0.0, 0.0)
        text == "!" || text == "′" || text == "″" -> OperatorProperties(0.0, 0.0)
        else -> OperatorProperties(THICK, THICK)
    }

    /** the operator as it is drawn: a hyphen-minus is drawn as a minus */
    fun glyphText(text: String) = if (text == "-") "−" else text
}
