package typed

import org.openrndr.color.ColorRGBa
import org.openrndr.extra.expressions.typed.TypedFunctionExtensions
import org.openrndr.extra.expressions.typed.evaluateTypedExpression
import org.openrndr.math.Polar
import org.openrndr.math.Spherical
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TestTypedPropertiesAndMath {
    private fun eval(expression: String, vararg constants: Pair<String, Any>) =
        evaluateTypedExpression(expression, { name -> constants.toMap()[name] })

    @Test
    fun `circles, rectangles, polar and spherical coordinates have properties`() {
        val circle = Circle(10.0, 20.0, 5.0)
        assertEquals(5.0, eval("it.radius", "it" to circle))
        assertEquals(Vector2(10.0, 20.0), eval("it.center", "it" to circle))
        assertEquals(20.0, eval("it.y", "it" to circle))
        assertEquals(PI * 25.0, eval("it.area", "it" to circle) as Double, 1E-9)

        val rectangle = Rectangle(1.0, 2.0, 30.0, 40.0)
        assertEquals(listOf(1.0, 2.0, 30.0, 40.0, 1200.0), listOf("x", "y", "width", "height", "area").map { eval("it.$it", "it" to rectangle) })
        assertEquals(Vector2(16.0, 22.0), eval("it.center", "it" to rectangle))
        assertEquals(Vector2(1.0, 2.0), eval("it.corner", "it" to rectangle))

        val polar = Polar(90.0, 2.0)
        assertEquals(90.0, eval("it.theta", "it" to polar))
        assertEquals(polar.cartesian, eval("it.cartesian", "it" to polar))
        val spherical = Spherical(30.0, 60.0, 3.0)
        assertEquals(listOf(30.0, 60.0, 3.0), listOf("theta", "phi", "radius").map { eval("it.$it", "it" to spherical) })
        assertEquals(spherical.cartesian, eval("it.cartesian", "it" to spherical))

        // in a filter over a list, as a host would write it
        val circles = listOf(Circle(0.0, 0.0, 5.0), Circle(0.0, 0.0, 15.0), Circle(0.0, 0.0, 25.0))
        assertEquals(circles.drop(1), eval("circles.filter { it.radius > 10.0 }", "circles" to circles))
        assertFailsWith<Exception> { eval("it.weight", "it" to circle) }
    }

    @Test
    fun `hosts add properties of their own, after the built-in ones`() {
        class Particle(val mass: Double)
        val functions = TypedFunctionExtensions(properties = { receiver, name ->
            when {
                receiver is Particle && name == "mass" -> receiver.mass
                receiver is Circle && name == "diameter" -> receiver.radius * 2.0
                else -> null
            }
        })
        fun evalWith(expression: String, value: Any) = evaluateTypedExpression(expression, { if (it == "it") value else null }, functions)
        assertEquals(3.0, evalWith("it.mass * 1.5", Particle(2.0)))
        assertEquals(10.0, evalWith("it.diameter", Circle(0.0, 0.0, 5.0)))
        assertEquals(5.0, evalWith("it.radius", Circle(0.0, 0.0, 5.0)))
        assertEquals(0.5, evalWith("it.r", ColorRGBa(0.5, 0.0, 0.0)))
    }

    @Test
    fun `math functions of numbers and of the components of vectors`() {
        val cases = mapOf(
            "floor(2.7)" to 2.0, "ceil(2.2)" to 3.0, "fract(2.25)" to 0.25, "fract(-0.25)" to 0.75,
            "pow(2.0, 10.0)" to 1024.0, "clamp(1.5, 0.0, 1.0)" to 1.0, "clamp(-0.5, 0.0, 1.0)" to 0.0,
            "smoothstep(0.0, 1.0, 0.5)" to 0.5,
            "sign(-3.0)" to -1.0, "mod(-1.0, 3.0)" to 2.0, "exp(0.0)" to 1.0, "log(1.0)" to 0.0,
            "saturate(1.5)" to 1.0, "degrees(PI)" to 180.0
        )
        for ((expression, expected) in cases) {
            assertEquals(expected, eval(expression) as Double, 1E-9, expression)
        }
        assertEquals(kotlin.math.tan(0.5), eval("tan(0.5)") as Double, 1E-12)
        assertEquals(kotlin.math.atan2(1.0, -1.0), eval("atan2(1.0, -1.0)") as Double, 1E-12)
        // vectors component by component, numbers stand for every component
        assertEquals(Vector2(1.0, -2.0), eval("floor(vec2(1.5, -1.5))"))
        assertEquals(Vector3(0.0, 0.5, 1.0), eval("clamp(vec3(-1.0, 0.5, 2.0), 0.0, 1.0)"))
        assertEquals(Vector2(9.0, 16.0), eval("pow(vec2(3.0, 4.0), 2.0)"))
        assertFailsWith<Exception> { eval("floor(vec2(1.0, 1.0), vec3(1.0, 1.0, 1.0))") }
        assertFailsWith<Exception> { eval("clamp(vec2(1.0, 1.0), vec3(0.0, 0.0, 0.0), 1.0)") }
    }

    @Test
    fun `a filter by index, with truth as numbers and a constant named it outside lambdas`() {
        val values = (0 until 9).map { it.toDouble() }
        // filterIndexed as a host does it: it, i and n as constants per element
        val kept = values.filterIndexed { i, v ->
            val r = evaluateTypedExpression("floor(i / 3.0) % 2.0 == 0.0 && it < 7.0 || !(n == 9.0)", { name ->
                when (name) { "it" -> v; "i" -> i.toDouble(); "n" -> values.size.toDouble(); else -> null }
            })
            assertTrue(r == 1.0 || r == 0.0, "comparisons and joins give 1.0 or 0.0, not $r")
            r == 1.0
        }
        assertEquals(listOf(0.0, 1.0, 2.0, 6.0), kept)
        // inside a lambda, it is the lambda's own
        assertEquals(listOf(10.0, 20.0), evaluateTypedExpression("[1.0, 2.0].map { it * 10.0 }", { if (it == "it") 99.0 else null }))
    }

    @Test
    fun `negative and positive zero are equal, also in vectors and colors`() {
        assertEquals(1.0, eval("-0.0 == 0.0"))
        assertEquals(-0.0, eval("round(-0.3)"))
        assertEquals(1.0, eval("round(-0.3) == 0.0"))
        assertEquals(1.0, eval("vec2(-0.0, 1.0) == vec2(0.0, 1.0)"))
        assertEquals(1.0, eval("vec3(-0.0, -0.0, 1.0) == vec3(0.0, 0.0, 1.0)"))
        assertEquals(1.0, eval("rgba(-0.0, 0.5, 0.5, 1.0) == rgba(0.0, 0.5, 0.5, 1.0)"))
        assertEquals(0.0, eval("0.1 == 0.2"))
        assertEquals(0.0, eval("vec2(0.0, 1.0) == vec2(0.0, 2.0)"))
        assertEquals(1.0, eval("\"ab\" == \"ab\""))
    }
}
