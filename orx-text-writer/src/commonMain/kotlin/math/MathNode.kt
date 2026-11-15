package org.openrndr.extra.textwriter.math

/** a formula, as the elements of presentation MathML that [layoutMath] lays out */
sealed class MathNode {
    /** children side by side, `mrow`, and the elements whose children are laid out like it */
    data class Row(val children: List<MathNode>) : MathNode()

    /**
     * an identifier, `mi`; a single letter is in mathematical italic unless [variant] is `"normal"`
     */
    data class Identifier(val text: String, val variant: String? = null) : MathNode()

    /** a number, `mn` */
    data class Number(val text: String) : MathNode()

    /**
     * an operator, `mo`: spaced, stretched and enlarged by the operator dictionary unless the attributes say
     * otherwise; [form] is `"prefix"`, `"infix"` or `"postfix"`, null to take it from its place in its row
     */
    data class Operator(
        val text: String,
        val form: String? = null,
        val stretchy: Boolean? = null,
        val largeOperator: Boolean? = null,
        val movableLimits: Boolean? = null,
        val leftSpaceInEm: Double? = null,
        val rightSpaceInEm: Double? = null
    ) : MathNode()

    /** text, `mtext` */
    data class Text(val text: String) : MathNode()

    /** space, `mspace`, in em */
    data class Space(val widthInEm: Double, val heightInEm: Double = 0.0, val depthInEm: Double = 0.0) : MathNode()

    /** a fraction, `mfrac`; [lineThicknessFactor] scales the thickness of the fraction rule, 0 for none */
    data class Fraction(val numerator: MathNode, val denominator: MathNode, val lineThicknessFactor: Double = 1.0) : MathNode()

    /** a square root, `msqrt` */
    data class SquareRoot(val radicand: MathNode) : MathNode()

    /** a root with an index, `mroot` */
    data class Root(val radicand: MathNode, val index: MathNode) : MathNode()

    /** sub- and superscripts, `msub`, `msup` and `msubsup` */
    data class Scripts(val base: MathNode, val subscript: MathNode?, val superscript: MathNode?) : MathNode()

    /**
     * under- and overscripts, `munder`, `mover` and `munderover`; [accent] and [accentUnder] keep the script at the
     * size of the base and close to it
     */
    data class UnderOver(
        val base: MathNode,
        val underscript: MathNode?,
        val overscript: MathNode?,
        val accent: Boolean = false,
        val accentUnder: Boolean = false
    ) : MathNode()

    /** a change of style, `mstyle`; null keeps what is inherited */
    data class Style(val child: MathNode, val displayStyle: Boolean? = null, val scriptLevel: Int? = null) : MathNode()

    /** the root of a formula, `math`; [display] for `display="block"` */
    data class Formula(val child: MathNode, val display: Boolean = false) : MathNode()
}
