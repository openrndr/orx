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

    fun number(value: String): Numeric {
        return when (val match = PropertyRegex.Number.regex.matchEntire(value)) {
            is MatchResult -> Numeric.Rational(match.groups[0]?.value?.toDouble() ?: 0.0)
            else -> Numeric.Rational(0.0)
        }
    }

    fun length(value: String): Length {
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
    fun transform(element: Element, attribute: String = Attr.TRANSFORM): Transform {
        var transform = Matrix44.IDENTITY

        val transformValue = element.attr(attribute).let {
            it.ifEmpty {
                return Transform.None
            }
        }

        // TODO: Number regex accepts `-` as a number lol
        val p = Pattern.compile("(matrix|translate|scale|rotate|skewX|skewY)\\([\\d\\.,\\-\\s]+\\)")
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

    fun circle(element: Element): List<Command> {
        val cx = element.attr(Attr.CX).let {
            if (it.isEmpty()) 0.0 else it.toDoubleOrNull()
        } ?: return emptyList()
        val cy = element.attr(Attr.CY).let {
            if (it.isEmpty()) 0.0 else it.toDoubleOrNull()
        } ?: return emptyList()
        val r = element.attr(Attr.R).let {
            if (it.isEmpty()) 0.0 else it.toDoubleOrNull()?.times(2.0)
        } ?: return emptyList()

        return ellipsePath(cx, cy, r, r)
    }

    fun ellipse(element: Element): List<Command> {
        val cx = element.attr(Attr.CX).let {
            if (it.isEmpty()) 0.0 else it.toDoubleOrNull()
        } ?: return emptyList()
        val cy = element.attr(Attr.CY).let {
            if (it.isEmpty()) 0.0 else it.toDoubleOrNull()
        } ?: return emptyList()
        val rx = element.attr(Attr.RX).let {
            if (it.isEmpty()) 0.0 else it.toDoubleOrNull()?.times(2.0)
        } ?: return emptyList()
        val ry = element.attr(Attr.RY).let {
            if (it.isEmpty()) 0.0 else it.toDoubleOrNull()?.times(2.0)
        } ?: return emptyList()

        return ellipsePath(cx, cy, rx, ry)
    }

    fun rectangle(element: Element): List<Command> {
        val x = element.attr(Attr.X).let { if (it.isEmpty()) 0.0 else it.toDoubleOrNull() } ?: return emptyList()
        val y = element.attr(Attr.Y).let { if (it.isEmpty()) 0.0 else it.toDoubleOrNull() } ?: return emptyList()
        val width = element.attr(Attr.WIDTH).toDoubleOrNull() ?: return emptyList()
        val height = element.attr(Attr.HEIGHT).toDoubleOrNull() ?: return emptyList()

        // rounded corners: a missing radius is the other radius, radii are at most half the width and height
        val rxAttribute = element.attr("rx").toDoubleOrNull()?.takeIf { it > 0.0 }
        val ryAttribute = element.attr("ry").toDoubleOrNull()?.takeIf { it > 0.0 }
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
        val x1 = element.attr(Attr.X1).toDoubleOrNull() ?: return emptyList()
        val x2 = element.attr(Attr.X2).toDoubleOrNull() ?: return emptyList()
        val y1 = element.attr(Attr.Y1).toDoubleOrNull() ?: return emptyList()
        val y2 = element.attr(Attr.Y2).toDoubleOrNull() ?: return emptyList()

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

        val rawCommands = pathValue.split("(?=[MmZzLlHhVvCcSsQqTtAa])".toRegex()).map(String::trim)
        val numbers = Pattern.compile("[-+]?[0-9]*[.]?[0-9]+(?:[eE][-+]?[0-9]+)?")
        val commands = mutableListOf<Command>()

        for (rawCommand in rawCommands) {
            if (rawCommand.isNotEmpty()) {
                val numberMatcher = numbers.matcher(rawCommand)
                val operands = mutableListOf<Double>()
                while (numberMatcher.find()) {
                    operands.add(numberMatcher.group().toDouble())
                }
                commands += Command(rawCommand[0].toString(), *(operands.toDoubleArray()))
            }
        }

        return commands
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