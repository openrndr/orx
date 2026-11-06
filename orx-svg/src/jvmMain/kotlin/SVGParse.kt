package org.openrndr.extra.svg
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.nodes.*
import org.openrndr.color.*
import org.openrndr.extra.composition.*
import org.openrndr.math.*
import org.openrndr.math.transforms.*
import org.openrndr.shape.*
import java.util.regex.*
import kotlin.math.*
import kotlin.text.MatchResult

private val logger = KotlinLogging.logger {}

internal sealed interface PropertyRegex {

    val regex: Regex

    companion object {
        val wsp = "(?:\\s|\\A|\\Z)+".toRegex()
        val commaWsp = "(?:\\s*,\\s*|\\s+)".toRegex()
        const val align = "(?<align>none|[xy](?:Min|Mid|Max)[XY](?:Min|Mid|Max))?"
        const val meetOrSlice = "(?<meetOrSlice>meet|slice)?"
        const val unitIdentifier = "in|pc|pt|px|cm|mm|Q"
        val opts = RegexOption.IGNORE_CASE
    }

    object Any : PropertyRegex {
        override val regex = ".+".toRegex()
    }

    object Number : PropertyRegex {
        override val regex = "[+-]?(?:\\d+(?:\\.\\d+)?|\\.\\d+)(?:[eE][+-]?\\d+)?".toRegex()
    }

    object NumberList : PropertyRegex {
        override val regex = "(?:${Number.regex}$commaWsp${Number.regex}$commaWsp?)+".toRegex()
    }

    object Length : PropertyRegex {
        override val regex = "(?<number>${Number.regex})(?<ident>$unitIdentifier)?".toRegex(opts)
    }

    object Percentage : PropertyRegex {
        override val regex = "${Number.regex}%".toRegex()
    }

    object LengthOrPercentage : PropertyRegex {
        override val regex = "${Length.regex}|${Percentage.regex}".toRegex(opts)
    }

    object PreserveAspectRatio : PropertyRegex {
        // We don't care for "defer", but if it's there, we'll ignore it.
        override val regex = "$wsp(?:defer)?$wsp${align}$wsp${meetOrSlice}$wsp".toRegex(opts)
    }

    object RGBHex : PropertyRegex {
        override val regex = "#?([0-9a-f]{3,6})".toRegex(opts)
    }

    object RGBFunctional : PropertyRegex {
        // Matches rgb(255, 255, 255)
        private val rgb8BitRegex = "(${Number.regex})${commaWsp}(${Number.regex})${commaWsp}(${Number.regex})"

        // Matches rgb(100%, 100%, 100%)
        private val rgbPercentageRegex = "(${Number.regex})%${commaWsp}(${Number.regex})%${commaWsp}(${Number.regex})%"

        override val regex = "${wsp}rgb\\(\\s*(?>$rgb8BitRegex\\s*|\\s*$rgbPercentageRegex)\\s*\\)$wsp".toRegex(opts)
    }
}

internal object SVGParse {
    fun viewBox(element: Element): ViewBox {
        val viewBoxValue = element.attr(Attr.VIEW_BOX).trim()
        val (minX, minY, width, height) = PropertyRegex.NumberList.regex.matches(viewBoxValue).let {
            if (!it) {
                return ViewBox.None
            }
            val list = viewBoxValue.split(PropertyRegex.commaWsp).map(String::toDouble)
            when (list.size) {
                // Early return and signal that the element should not be rendered at all
                1 -> if (list[0] == 0.0) {
                    return ViewBox.None
                } else {
                    // Interpret as height
                    listOf(0.0, 0.0, 0.0, list[0])
                }
                2 -> listOf(0.0, 0.0, list[0], list[1])
                3 -> listOf(0.0, list[0], list[1], list[2])
                4 -> list
                else -> return ViewBox.None
            }
        }

        return ViewBox.Value(Rectangle(minX, minY, width.coerceAtLeast(0.0), height.coerceAtLeast(0.0)))
    }

    fun preserveAspectRatio(element: Element): AspectRatio {
        val aspectRatioValue = element.attr(Attr.PRESERVE_ASPECT_RATIO)

        val (alignmentValue, meetValue) = PropertyRegex.PreserveAspectRatio.regex.matchEntire(aspectRatioValue).let {
            val value = (it?.groups as? MatchNamedGroupCollection)?.get("align")?.value
            val type = (it?.groups as? MatchNamedGroupCollection)?.get("meetOrSlice")?.value

            value to type
        }

        val meet = when (meetValue) {
            "slice" -> MeetOrSlice.SLICE
            // Lacuna value
            else -> MeetOrSlice.MEET
        }

        return when (alignmentValue) {
            "none" -> AspectRatio(Align.NONE, meet)
            "xMinYMin" -> AspectRatio(Align.X_MIN_Y_MIN, meet)
            "xMidYMin" -> AspectRatio(Align.X_MID_Y_MIN, meet)
            "xMaxYMin" -> AspectRatio(Align.X_MAX_Y_MIN, meet)
            "xMinYMid" -> AspectRatio(Align.X_MIN_Y_MID, meet)
            "xMidYMid" -> AspectRatio(Align.X_MID_Y_MID, meet)
            "xMaxYMid" -> AspectRatio(Align.X_MAX_Y_MID, meet)
            "xMinYMax" -> AspectRatio(Align.X_MIN_Y_MAX, meet)
            "xMidYMax" -> AspectRatio(Align.X_MID_Y_MAX, meet)
            "xMaxYMax" -> AspectRatio(Align.X_MAX_Y_MAX, meet)
            else -> AspectRatio(Align.X_MID_Y_MID, meet)
        }
    }

    fun bounds(element: Element): CompositionDimensions {
        val values = listOf(Attr.X, Attr.Y, Attr.WIDTH, Attr.HEIGHT).map { attribute ->
            element.attr(attribute).let {
                it.ifEmpty { "0" }
            }
        }

        // There's no way this'll throw an OOB, right?
        val (x, y, width, height) = values.map { str ->
            PropertyRegex.Length.regex.matchEntire(str).let {
                val value = (it?.groups as? MatchNamedGroupCollection)?.get("number")?.value?.toDouble() ?: 0.0
                val type = Length.UnitIdentifier.valueOf(
                    (it?.groups as? MatchNamedGroupCollection)?.get("ident")?.value?.uppercase() ?: "PX"
                )

                when (type) {
                    Length.UnitIdentifier.IN -> Length.Pixels.fromInches(value)
                    Length.UnitIdentifier.PC -> Length.Pixels.fromPicas(value)
                    Length.UnitIdentifier.PT -> Length.Pixels.fromPoints(value)
                    Length.UnitIdentifier.PX -> Length.Pixels(value)
                    Length.UnitIdentifier.CM -> Length.Pixels.fromCentimeters(value)
                    Length.UnitIdentifier.MM -> Length.Pixels.fromMillimeters(value)
                    Length.UnitIdentifier.Q -> Length.Pixels.fromQuarterMillimeters(value)
                }
            }
        }

        return CompositionDimensions(x, y, width, height)
    }

    fun lineJoin(value: String): LineJoin {
        return when (value) {
            "miter" -> LineJoin.Miter
            "bevel" -> LineJoin.Bevel
            "round" -> LineJoin.Round
            else -> LineJoin.Miter
        }
    }

    fun lineCap(value: String): LineCap {
        return when (value) {
            "round" -> LineCap.Round
            "butt" -> LineCap.Butt
            "square" -> LineCap.Square
            else -> LineCap.Butt
        }
    }

    /**
     * The value of a CSS calc() function with +, -, * and / and parentheses; [leaf] gives the value of a number with
     * or without a unit. Null when the expression is not valid.
     */
    fun calc(value: String, leaf: (String) -> Double?): Double? {
        val tokens = Regex("calc\\(|\\(|\\)|\\*|/|[+-](?=\\s)|[+-]?(?:\\d+\\.?\\d*|\\.\\d+)(?:e[+-]?\\d+)?(?:[a-zA-Z]+|%)?")
            .findAll(value).map { it.value }.toList()
        var position = 0
        fun peek() = tokens.getOrNull(position)
        fun expression(): Double? {
            fun factor(): Double? = when (val token = tokens.getOrNull(position++)) {
                null -> null
                "calc(", "(" -> expression().takeIf { tokens.getOrNull(position++) == ")" }
                else -> leaf(token)
            }
            fun term(): Double? {
                var result = factor() ?: return null
                while (peek() == "*" || peek() == "/") {
                    val operator = tokens[position++]
                    val operand = factor() ?: return null
                    result = if (operator == "*") result * operand else result / operand
                }
                return result
            }
            var result = term() ?: return null
            while (peek() == "+" || peek() == "-") {
                val operator = tokens[position++]
                val operand = term() ?: return null
                result = if (operator == "+") result + operand else result - operand
            }
            return result
        }
        return expression()?.takeIf { position == tokens.size && it.isFinite() }
    }

    fun number(value: String): Numeric {
        if ("calc(" in value) return Numeric.Rational(calc(value.trim()) { it.toDoubleOrNull() } ?: 0.0)
        return when (val match = PropertyRegex.Number.regex.matchEntire(value)) {
            is MatchResult -> Numeric.Rational(match.groups[0]?.value?.toDouble() ?: 0.0)
            else -> Numeric.Rational(0.0)
        }
    }

    fun length(value: String): Length {
        if ("calc(" in value) return Length.Pixels(lengthValue(value, null) ?: 0.0)
        val (number, ident) = PropertyRegex.Length.regex.matchEntire(value).let {
            val number = (it?.groups as? MatchNamedGroupCollection)?.get("number")?.value?.toDouble() ?: 0.0
            val ident = Length.UnitIdentifier.valueOf(
                (it?.groups as? MatchNamedGroupCollection)?.get("ident")?.value?.uppercase() ?: "PX"
            )

            number to ident
        }

        return when (ident) {
            Length.UnitIdentifier.IN -> Length.Pixels.fromInches(number)
            Length.UnitIdentifier.PC -> Length.Pixels.fromPicas(number)
            Length.UnitIdentifier.PT -> Length.Pixels.fromPoints(number)
            Length.UnitIdentifier.PX -> Length.Pixels(number)
            Length.UnitIdentifier.CM -> Length.Pixels.fromCentimeters(number)
            Length.UnitIdentifier.MM -> Length.Pixels.fromMillimeters(number)
            Length.UnitIdentifier.Q -> Length.Pixels.fromQuarterMillimeters(number)
        }
    }

    // Syntax should map to https://www.w3.org/TR/css-transforms-1/#svg-syntax
    fun transform(element: Element, attribute: String = Attr.TRANSFORM): Transform =
        transform(element.attr(attribute))

    /**
     * A transform list, of SVG or CSS: matrix, translate, scale, rotate, skewX and skewY, and the CSS functions
     * translateX, translateY, scaleX and scaleY. CSS units px and deg are ignored, turn and rad are converted.
     */
    fun transform(value: String): Transform {
        var transform = Matrix44.IDENTITY

        val transformValue = value.trim().let {
            it.ifEmpty {
                return Transform.None
            }
        }.let { css ->
            // CSS angles and lengths: degrees and pixels are the units of SVG
            css.replace(Regex("([0-9.])turn")) { "${it.groupValues[1]}*360" }
                .replace(Regex("(-?[0-9.]+(?:[eE][-+]?[0-9]+)?)rad")) { (it.groupValues[1].toDouble() * 180.0 / PI).toString() }
                .replace(Regex("(-?[0-9.]+(?:[eE][-+]?[0-9]+)?)\\*360")) { (it.groupValues[1].toDouble() * 360.0).toString() }
                .replace(Regex("(?<=[0-9.])(deg|px)"), "")
                .replace("translateX(", "translate(").replace("scaleX(", "scale1x(")
                .replace(Regex("translateY\\(\\s*([^)]*)\\)")) { "translate(0 ${it.groupValues[1]})" }
                .replace(Regex("scaleY\\(\\s*([^)]*)\\)")) { "scale(1 ${it.groupValues[1]})" }
                .replace(Regex("scale1x\\(\\s*([^)]*)\\)")) { "scale(${it.groupValues[1]} 1)" }
        }

        // TODO: Number regex accepts `-` as a number lol
        val p = Pattern.compile("(matrix|translate|scale|rotate|skewX|skewY)\\([\\d\\.,\\-\\seE+]+\\)")
        val m = p.matcher(transformValue)

        // TODO: This looks to be making far too many assumptions about the well-formedness of its input
        fun getTransformOperands(token: String): List<Double> {
            val number = Pattern.compile("-?[0-9.eE\\-]+")
            val nm = number.matcher(token)
            val operands = mutableListOf<Double>()
            while (nm.find()) {
                val n = nm.group().toDouble()
                operands.add(n)
            }
            return operands
        }
        while (m.find()) {
            val token = m.group()
            if (token.startsWith("matrix")) {
                val operands = getTransformOperands(token)
                val mat = Matrix44(
                    operands[0], operands[2], 0.0, operands[4],
                    operands[1], operands[3], 0.0, operands[5],
                    0.0, 0.0, 1.0, 0.0,
                    0.0, 0.0, 0.0, 1.0
                )
                transform *= mat
            }
            if (token.startsWith("scale")) {
                val operands = getTransformOperands(token.substring(5))
                // the z scale is 1, so the transform can be inverted, as the transforms of paints are
                val mat = Matrix44.scale(operands[0], operands.elementAtOrElse(1) { operands[0] }, 1.0)
                transform *= mat
            }
            if (token.startsWith("translate")) {
                val operands = getTransformOperands(token.substring(9))
                val mat = Matrix44.translate(operands[0], operands.elementAtOrElse(1) { 0.0 }, 0.0)
                transform *= mat
            }
            if (token.startsWith("rotate")) {
                val operands = getTransformOperands(token.substring(6))
                val angle = Math.toRadians(operands[0])
                val sina = sin(angle)
                val cosa = cos(angle)
                val x = operands.elementAtOrElse(1) { 0.0 }
                val y = operands.elementAtOrElse(2) { 0.0 }
                val mat = Matrix44(
                    cosa, -sina, 0.0, -x * cosa + y * sina + x,
                    sina, cosa, 0.0, -x * sina - y * cosa + y,
                    0.0, 0.0, 1.0, 0.0,
                    0.0, 0.0, 0.0, 1.0
                )
                transform *= mat
            }
            if (token.startsWith("skewX")) {
                val operands = getTransformOperands(token.substring(5))
                val mat = Matrix44(
                    1.0, tan(Math.toRadians(operands[0])), 0.0, 0.0,
                    0.0, 1.0, 0.0, 0.0,
                    0.0, 0.0, 1.0, 0.0,
                    0.0, 0.0, 0.0, 1.0
                )
                transform *= mat
            }
            if (token.startsWith("skewY")) {
                val operands = getTransformOperands(token.substring(5))
                val mat = Matrix44(
                    1.0, 0.0, 0.0, 0.0,
                    tan(Math.toRadians(operands[0])), 1.0, 0.0, 0.0,
                    0.0, 0.0, 1.0, 0.0,
                    0.0, 0.0, 0.0, 1.0
                )
                transform *= mat
            }
        }

        return if (transform != Matrix44.IDENTITY) {
            Transform.Matrix(transform)
        } else {
            Transform.None
        }
    }

    /** Assumes [numbers] consists of at least 2 elements. */
    private fun pointsToCommands(numbers: List<Double>): List<Command> {
        val commands = mutableListOf(Command("M", numbers[0], numbers[1]))
        numbers.drop(2).windowed(2, 2, false).mapTo(commands) { (x, y) ->
            Command("L", x, y)
        }
        return commands
    }

    fun polygon(element: Element): List<Command> {
        val commands = polyline(element).toMutableList()
        commands.add(Command("Z"))
        return commands
    }

    fun polyline(element: Element): List<Command> {
        val numbers = element.attr(Attr.POINTS)
            .trim()
            .split(PropertyRegex.commaWsp)
            .takeWhile(PropertyRegex.Number.regex::matches)
            .map(String::toDouble)
        return if (numbers.size > 1) {
            if (numbers.size and 1 == 1) {
                logger.warn { "${element.tagName()} attribute ${Attr.POINTS} has odd amount of numbers" }
                pointsToCommands(numbers.dropLast(1))
            } else {
                pointsToCommands(numbers)
            }
        } else {
            emptyList()
        }
    }

    private fun ellipsePath(x: Double, y: Double, width: Double, height: Double): List<Command> {
        val dx = x - width / 2
        val dy = y - height / 2

        val kappa = 0.5522848
        // control point offset horizontal
        val ox = width / 2 * kappa
        // control point offset vertical
        val oy = height / 2 * kappa
        // x-end
        val xe = dx + width
        // y-end
        val ye = dy + height

        // like SVG, the outline starts at the right and runs to the bottom first, dash patterns start there
        return listOf(
            Command("M", xe, y),
            Command("C", xe, y + oy, x + ox, ye, x, ye),
            Command("C", x - ox, ye, dx, y + oy, dx, y),
            Command("C", dx, y - oy, x - ox, dy, x, dy),
            Command("C", x + ox, dy, xe, y - oy, xe, y),
            Command("z")
        )
    }

    /** which size of the viewport a percentage is relative to */
    enum class LengthAxis { X, Y, OTHER }

    /** the size of the viewport of the document that is being parsed, which percentages refer to */
    val viewport = ThreadLocal<Vector2>()

    /** the font size of [element]: its own or that of the nearest ancestor that has one, in pixels; 16 by default */
    fun fontSize(element: Element?): Double {
        var current: Element? = element
        val sizes = mutableListOf<String>()
        while (current != null) {
            val inline = current.attr("style").split(";").map { it.split(":").map(String::trim) }
                .firstOrNull { it.size >= 2 && it[0] == "font-size" }?.get(1)
            (inline ?: current.attr("font-size").trim().takeIf { it.isNotEmpty() })?.let { sizes.add(it) }
            current = current.parent()
        }
        // relative sizes are resolved from the outermost size inwards
        var size = 16.0
        for (value in sizes.asReversed()) {
            size = when {
                value.endsWith("em") && !value.endsWith("rem") -> (value.dropLast(2).toDoubleOrNull() ?: 1.0) * size
                value.endsWith("%") -> (value.dropLast(1).toDoubleOrNull() ?: 100.0) / 100.0 * size
                else -> value.toDoubleOrNull() ?: length(value).value
            }
        }
        return size
    }

    /**
     * A length in pixels: a number, a number with an absolute unit, em or ex, relative to the font size of [element],
     * or a percentage of the size of the viewport along [axis]. Null when it is not a length.
     */
    fun lengthValue(value: String, element: Element?, axis: LengthAxis = LengthAxis.OTHER): Double? {
        val v = value.trim()
        if (v.isEmpty()) return null
        v.toDoubleOrNull()?.let { return it }
        if (v.startsWith("calc(")) return calc(v) { lengthValue(it, element, axis) }
        return when {
            v.endsWith("%") -> {
                val fraction = (v.dropLast(1).toDoubleOrNull() ?: return null) / 100.0
                val size = viewport.get() ?: Vector2(100.0, 100.0)
                fraction * when (axis) {
                    LengthAxis.X -> size.x
                    LengthAxis.Y -> size.y
                    LengthAxis.OTHER -> sqrt((size.x * size.x + size.y * size.y) / 2.0)
                }
            }
            v.endsWith("em") && !v.endsWith("rem") -> (v.dropLast(2).toDoubleOrNull() ?: return null) * fontSize(element)
            v.endsWith("ex") -> (v.dropLast(2).toDoubleOrNull() ?: return null) * fontSize(element) * 0.5
            PropertyRegex.Length.regex.matchEntire(v) != null -> length(v).value
            else -> null
        }
    }

    /** the length of attribute [name] of [element] in pixels, [default] when it is not given, null when it is invalid */
    private fun attributeLength(element: Element, name: String, axis: LengthAxis, default: Double?): Double? {
        val value = element.attr(name).trim()
        if (value.isEmpty()) return default
        return lengthValue(value, element, axis)
    }

    fun circle(element: Element): List<Command> {
        val cx = attributeLength(element, Attr.CX, LengthAxis.X, 0.0) ?: return emptyList()
        val cy = attributeLength(element, Attr.CY, LengthAxis.Y, 0.0) ?: return emptyList()
        val r = attributeLength(element, Attr.R, LengthAxis.OTHER, 0.0)?.times(2.0) ?: return emptyList()

        return ellipsePath(cx, cy, r, r)
    }

    fun ellipse(element: Element): List<Command> {
        val cx = attributeLength(element, Attr.CX, LengthAxis.X, 0.0) ?: return emptyList()
        val cy = attributeLength(element, Attr.CY, LengthAxis.Y, 0.0) ?: return emptyList()
        val rx = attributeLength(element, Attr.RX, LengthAxis.X, 0.0)?.times(2.0) ?: return emptyList()
        val ry = attributeLength(element, Attr.RY, LengthAxis.Y, 0.0)?.times(2.0) ?: return emptyList()

        return ellipsePath(cx, cy, rx, ry)
    }

    fun rectangle(element: Element): List<Command> {
        val x = attributeLength(element, Attr.X, LengthAxis.X, 0.0) ?: return emptyList()
        val y = attributeLength(element, Attr.Y, LengthAxis.Y, 0.0) ?: return emptyList()
        val width = attributeLength(element, Attr.WIDTH, LengthAxis.X, null) ?: return emptyList()
        val height = attributeLength(element, Attr.HEIGHT, LengthAxis.Y, null) ?: return emptyList()

        // rounded corners: a missing radius is the other radius, radii are at most half the width and height
        val rxAttribute = attributeLength(element, "rx", LengthAxis.X, null)?.takeIf { it > 0.0 }
        val ryAttribute = attributeLength(element, "ry", LengthAxis.Y, null)?.takeIf { it > 0.0 }
        val rx = (rxAttribute ?: ryAttribute ?: 0.0).coerceAtMost(width / 2.0)
        val ry = (ryAttribute ?: rxAttribute ?: 0.0).coerceAtMost(height / 2.0)
        if (rx > 0.0 && ry > 0.0) {
            return listOf(
                Command("M", x + rx, y),
                Command("h", width - 2.0 * rx),
                Command("a", rx, ry, 0.0, 0.0, 1.0, rx, ry),
                Command("v", height - 2.0 * ry),
                Command("a", rx, ry, 0.0, 0.0, 1.0, -rx, ry),
                Command("h", -(width - 2.0 * rx)),
                Command("a", rx, ry, 0.0, 0.0, 1.0, -rx, -ry),
                Command("v", -(height - 2.0 * ry)),
                Command("a", rx, ry, 0.0, 0.0, 1.0, rx, -ry),
                Command("z")
            )
        }

        return listOf(
            Command("M", x, y),
            Command("h", width),
            Command("v", height),
            Command("h", -width),
            Command("z")
        )
    }

    fun line(element: Element): List<Command> {
        val x1 = attributeLength(element, Attr.X1, LengthAxis.X, 0.0) ?: return emptyList()
        val x2 = attributeLength(element, Attr.X2, LengthAxis.X, 0.0) ?: return emptyList()
        val y1 = attributeLength(element, Attr.Y1, LengthAxis.Y, 0.0) ?: return emptyList()
        val y2 = attributeLength(element, Attr.Y2, LengthAxis.Y, 0.0) ?: return emptyList()

        return listOf(
            Command("M", x1, y1),
            Command("L", x2, y2)
        )
    }

    fun path(element: Element): List<Command> {
        val pathValue = element.attr(Attr.D)

        if (pathValue.trim() == "none") {
            return emptyList()
        }

        return pathCommands(pathValue)
    }

    /**
     * The commands of the path data [pathValue]. The flags of arcs can be written without separators, as minified
     * paths do: "a5 5 0 105 5" has the flags 1 and 0.
     */
    fun pathCommands(pathValue: String): List<Command> {
        val rawCommands = pathValue.split("(?=[MmZzLlHhVvCcSsQqTtAa])".toRegex()).map(String::trim)
        val numbers = Pattern.compile("[-+]?[0-9]*[.]?[0-9]+(?:[eE][-+]?[0-9]+)?")
        val commands = mutableListOf<Command>()

        for (rawCommand in rawCommands) {
            if (rawCommand.isNotEmpty()) {
                val operands = mutableListOf<Double>()
                val isArc = rawCommand[0] == 'a' || rawCommand[0] == 'A'
                var position = 1
                while (position < rawCommand.length) {
                    // the fourth and fifth argument of every arc are flags of a single digit
                    if (isArc && operands.size % 7 in 3..4) {
                        val c = rawCommand[position]
                        if (c == '0' || c == '1') {
                            operands.add(if (c == '1') 1.0 else 0.0)
                            position++
                            continue
                        }
                        if (c == ' ' || c == ',' || c == '\t' || c == '\n' || c == '\r') {
                            position++
                            continue
                        }
                    }
                    val numberMatcher = numbers.matcher(rawCommand)
                    if (!numberMatcher.find(position)) break
                    // a number of an arc flag position is read as a flag in the next round
                    if (isArc && operands.size % 7 in 3..4) break
                    operands.add(numberMatcher.group().toDouble())
                    position = numberMatcher.end()
                }
                commands += Command(rawCommand[0].toString(), *(operands.toDoubleArray()))
            }
        }

        return commands
    }

    /**
     * A dash array: none, or lengths separated by commas or spaces. Negative or invalid lengths, or lengths that are
     * all zero, give no dashes, as in SVG.
     */
    fun dashArray(value: String): DashArray {
        val trimmed = value.trim()
        if (trimmed.isEmpty() || trimmed == "none") return DashArray.None
        val lengths = trimmed.split(Regex("[\\s,]+")).filter { it.isNotEmpty() }.map {
            if (it.endsWith("%")) return DashArray.None else length(it).value
        }
        return if (lengths.isEmpty() || lengths.any { it < 0.0 } || lengths.sum() <= 0.0) DashArray.None
        else DashArray.Lengths(lengths)
    }

    /**
     * A paint order: the stroke is painted first when it is named before the fill, or the fill is not named
     */
    /** an angle in degrees: a number, or a number with the unit deg, rad, grad or turn */
    fun angle(value: String): Double? {
        val v = value.trim()
        return when {
            v.endsWith("deg") -> v.dropLast(3).toDoubleOrNull()
            v.endsWith("grad") -> v.dropLast(4).toDoubleOrNull()?.times(0.9)
            v.endsWith("rad") -> v.dropLast(3).toDoubleOrNull()?.times(180.0 / PI)
            v.endsWith("turn") -> v.dropLast(4).toDoubleOrNull()?.times(360.0)
            else -> v.toDoubleOrNull()
        }
    }

    /** a marker: none, or a reference to a marker element */
    fun marker(value: String): MarkerReference {
        val match = Regex("^\\s*url\\(\\s*['\"]?#([^)'\"]+)['\"]?\\s*\\)\\s*$").matchEntire(value) ?: return MarkerReference.None
        return SVGGradients.lookupMarker(match.groupValues[1].trim())?.let { MarkerReference.Value(it) } ?: MarkerReference.None
    }

    /** a clip path: none, or a reference to a clipPath element */
    fun clipPath(value: String): ClipPath {
        val match = Regex("^\\s*url\\(\\s*['\"]?#([^)'\"]+)['\"]?\\s*\\)\\s*$").matchEntire(value) ?: return ClipPath.None
        return SVGGradients.lookupClip(match.groupValues[1].trim()) ?: ClipPath.None
    }

    fun mask(value: String): MaskReference {
        val match = Regex("^\\s*url\\(\\s*['\"]?#([^)'\"]+)['\"]?\\s*\\)\\s*$").matchEntire(value) ?: return MaskReference.None
        return SVGGradients.lookupMask(match.groupValues[1].trim())?.let { MaskReference.Value(it) } ?: MaskReference.None
    }

    /**
     * The tags and values of font-feature-settings ([features] true, values default to 1 and can be on or off) or of
     * font-variation-settings
     */
    fun fontSettings(value: String, features: Boolean): Map<String, Double> {
        if (value.trim() == "normal") return emptyMap()
        return value.split(",").mapNotNull { entry ->
            val match = Regex("^\\s*['\"]([^'\"]{4})['\"]\\s*(\\S*)\\s*$").matchEntire(entry) ?: return@mapNotNull null
            val v = match.groupValues[2]
            val number = when {
                v.isEmpty() -> if (features) 1.0 else return@mapNotNull null
                v == "on" -> 1.0
                v == "off" -> 0.0
                else -> v.toDoubleOrNull() ?: return@mapNotNull null
            }
            match.groupValues[1] to number
        }.toMap()
    }

    /** the OpenType features of the keywords of font-variant */
    fun fontVariant(value: String): Map<String, Double> {
        val features = mutableMapOf<String, Double>()
        for (keyword in value.trim().split(Regex("\\s+"))) {
            when (keyword) {
                "none" -> listOf("liga", "clig", "dlig", "hlig", "calt").forEach { features[it] = 0.0 }
                "small-caps" -> features["smcp"] = 1.0
                "all-small-caps" -> { features["smcp"] = 1.0; features["c2sc"] = 1.0 }
                "petite-caps" -> features["pcap"] = 1.0
                "all-petite-caps" -> { features["pcap"] = 1.0; features["c2pc"] = 1.0 }
                "unicase" -> features["unic"] = 1.0
                "titling-caps" -> features["titl"] = 1.0
                "lining-nums" -> features["lnum"] = 1.0
                "oldstyle-nums" -> features["onum"] = 1.0
                "proportional-nums" -> features["pnum"] = 1.0
                "tabular-nums" -> features["tnum"] = 1.0
                "diagonal-fractions" -> features["frac"] = 1.0
                "stacked-fractions" -> features["afrc"] = 1.0
                "ordinal" -> features["ordn"] = 1.0
                "slashed-zero" -> features["zero"] = 1.0
                "common-ligatures" -> { features["liga"] = 1.0; features["clig"] = 1.0 }
                "no-common-ligatures" -> { features["liga"] = 0.0; features["clig"] = 0.0 }
                "discretionary-ligatures" -> features["dlig"] = 1.0
                "no-discretionary-ligatures" -> features["dlig"] = 0.0
                "historical-ligatures" -> features["hlig"] = 1.0
                "no-historical-ligatures" -> features["hlig"] = 0.0
                "contextual" -> features["calt"] = 1.0
                "no-contextual" -> features["calt"] = 0.0
                "super" -> features["sups"] = 1.0
                "sub" -> features["subs"] = 1.0
            }
        }
        return features
    }

    fun paintOrder(value: String): PaintOrder {
        val names = value.trim().split(Regex("\\s+")).filter { it == "fill" || it == "stroke" }
        return if (names.firstOrNull() == "stroke") PaintOrder.StrokeFirst else PaintOrder.Normal
    }

    fun color(colorValue: String): Paint {
        // a reference to a gradient, with an optional color for when the reference can not be resolved
        Regex("^\\s*url\\(\\s*['\"]?#([^)'\"]+)['\"]?\\s*\\)\\s*(.*)$").matchEntire(colorValue)?.let { match ->
            return SVGGradients.lookup(match.groupValues[1].trim())
                ?: match.groupValues[2].trim().takeIf { it.isNotEmpty() }?.let { color(it) }
                ?: Paint.None
        }
        val col = colorValue.trim().lowercase()

        return when {
            col.isEmpty() || col == "none" -> Paint.None
            col == "currentcolor" -> Paint.CurrentColor
            col == "transparent" -> Paint.RGB(ColorRGBa(0.0, 0.0, 0.0, 0.0, linearity = Linearity.SRGB))
            col.startsWith("#") -> hexColor(col.drop(1))?.let { Paint.RGB(it) } ?: Paint.None
            Regex("^(rgba?|hsla?)\\((.*)\\)$").matchEntire(col) != null -> functionalColor(col)?.let { Paint.RGB(it) } ?: Paint.None
            // the color functions of CSS Color 4 and 5
            Regex("^([a-z-]+)\\((.*)\\)$", RegexOption.DOT_MATCHES_ALL).matchEntire(col) != null -> {
                val name = col.substringBefore("(")
                CSSColors.function(name, col.substringAfter("(").substringBeforeLast(")")) { (color(it) as? Paint.RGB)?.value }
                    ?.let { Paint.RGB(it) } ?: Paint.None
            }
            col in cssColorNames -> Paint.RGB(ColorRGBa.fromHex(cssColorNames[col]!!))
            else -> Paint.None
        }
    }

    /** a hexadecimal color of 3, 4, 6 or 8 digits: rgb, rgba, rrggbb or rrggbbaa */
    private fun hexColor(hex: String): ColorRGBa? {
        if (!hex.all { it in "0123456789abcdef" }) return null
        val digits = when (hex.length) {
            3, 4 -> hex.map { "$it$it" }
            6, 8 -> hex.chunked(2)
            else -> return null
        }.map { it.toInt(16) / 255.0 }
        return ColorRGBa(digits[0], digits[1], digits[2], digits.getOrElse(3) { 1.0 }, linearity = Linearity.SRGB)
    }

    /**
     * A color in functional notation: rgb(), rgba(), hsl() or hsla(), with arguments separated by commas or spaces and
     * an optional alpha, after a slash in the space separated syntax
     */
    private fun functionalColor(value: String): ColorRGBa? {
        val name = value.substringBefore("(")
        val arguments = value.substringAfter("(").substringBeforeLast(")")
            .split(Regex("[\\s,/]+")).filter { it.isNotEmpty() }
        if (arguments.size !in 3..4) return null

        /** a number, or a percentage of [full] */
        fun component(argument: String, full: Double): Double? =
            if (argument.endsWith("%")) argument.dropLast(1).toDoubleOrNull()?.let { it / 100.0 * full }
            else argument.toDoubleOrNull()

        val alpha = arguments.getOrNull(3)?.let { component(it, 1.0) ?: return null }?.coerceIn(0.0, 1.0) ?: 1.0
        return if (name.startsWith("rgb")) {
            val (r, g, b) = arguments.take(3).map { (component(it, 255.0) ?: return null).coerceIn(0.0, 255.0) / 255.0 }
            ColorRGBa(r, g, b, alpha, linearity = Linearity.SRGB)
        } else {
            val hue = arguments[0].removeSuffix("deg").toDoubleOrNull() ?: return null
            val saturation = (component(arguments[1], 1.0) ?: return null).coerceIn(0.0, 1.0)
            val lightness = (component(arguments[2], 1.0) ?: return null).coerceIn(0.0, 1.0)
            // hsl to rgb, as in CSS Color 4
            fun f(n: Double): Double {
                val k = (n + hue / 30.0).mod(12.0)
                val a = saturation * min(lightness, 1.0 - lightness)
                return lightness - a * max(-1.0, min(k - 3.0, min(9.0 - k, 1.0)))
            }
            ColorRGBa(f(0.0), f(8.0), f(4.0), alpha, linearity = Linearity.SRGB)
        }
    }

    private fun expandToTwoDigitsPerComponent(hexValue: String) =
        hexValue.asSequence()
            .map { "$it$it" }
            .reduce(String::plus)
}