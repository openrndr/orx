import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.shadestyles.presets.*
import org.openrndr.extra.testing.AbstractApplicationTestFixture
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4
import org.openrndr.math.transforms.buildTransform
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals

class TestPaintUnits : AbstractApplicationTestFixture() {
    /** a source whose color is the coordinate it is read at: red is x, green is y */
    private class CoordinateSource : SourcePreset() {
        override val dynamicParameter = glslFunction<Vector4>("vec2 p", "return vec4(p, 0.0, 1.0);")
    }

    private val target by lazy {
        renderTarget(200, 100, contentScale = 1.0) { colorBuffer(type = ColorType.FLOAT32); depthBuffer() }
    }

    /** draws with [draw] into the target, and gives the coordinate the paint read at the point [x], [y] of the drawer */
    private fun paint(x: Int, y: Int, draw: Drawer.() -> Unit): Vector2 {
        val drawer = program.drawer
        drawer.isolatedWithTarget(target) {
            drawer.ortho(target)
            drawer.view = Matrix44.IDENTITY
            drawer.model = Matrix44.IDENTITY
            drawer.clear(ColorRGBa.TRANSPARENT)
            drawer.stroke = null
            drawer.fill = ColorRGBa.WHITE
            drawer.draw()
        }
        val buffer = target.colorBuffer(0)
        buffer.shadow.download()
        // the rows of the buffer of a render target run from the top down, like the y of the drawer
        val c = buffer.shadow[x, y]
        return Vector2(c.r, c.g)
    }

    private fun assertNear(expected: Vector2, actual: Vector2, message: String, tolerance: Double = 0.75) {
        assertEquals(true, abs(expected.x - actual.x) < tolerance && abs(expected.y - actual.y) < tolerance, "$message: $actual, not $expected")
    }

    private fun units(units: PaintUnits, fit: PaintFit = PaintFit.STRETCH) =
        PaintStyle(PaintLayer(CoordinateSource(), units, fit = fit), null)

    @Test
    fun `screen units are points of the target from its bottom left corner, with y up`() {
        val p = paint(30, 20) { shadeStyle = units(PaintUnits.SCREEN); rectangle(0.0, 0.0, 200.0, 100.0) }
        assertNear(Vector2(30.5, 100.0 - 20.5), p, "screen")
    }

    @Test
    fun `object, world and view units follow the model and the view transform, for shapes and in batches`() {
        val translate = buildTransform { translate(40.0, 10.0) }
        val view = buildTransform { translate(5.0, 30.0) }
        fun Drawer.transformed() {
            model = translate
            this.view = view
        }
        // a point of a circle at 30, 20 of the object: 70, 30 in the world, 75, 60 in the view, which is where it
        // is drawn
        for ((units, expected) in listOf(PaintUnits.OBJECT to Vector2(30.5, 20.5), PaintUnits.WORLD to Vector2(70.5, 30.5), PaintUnits.VIEW to Vector2(75.5, 60.5))) {
            val single = paint(75, 60) { transformed(); shadeStyle = units(units); circle(30.0, 20.0, 15.0) }
            assertNear(expected, single, "$units, a circle")
            val batch = paint(75, 60) { transformed(); shadeStyle = units(units); circles(listOf(Vector2(30.0, 20.0), Vector2(150.0, 0.0)), 15.0) }
            assertNear(expected, batch, "$units, circles")
            val rectangles = paint(75, 60) { transformed(); shadeStyle = units(units); rectangles(listOf(Rectangle(25.0, 15.0, 10.0, 10.0), Rectangle(0.0, 0.0, 5.0, 5.0))) }
            assertNear(expected, rectangles, "$units, rectangles")
            val contour = paint(75, 60) { transformed(); shadeStyle = units(units); contour(Circle(30.0, 20.0, 15.0).contour) }
            assertNear(expected, contour, "$units, a contour")
        }
    }

    @Test
    fun `a paint in bounds units stretches, fits or fills bounds that are not square`() {
        // a rectangle of 200 by 100, the point at its right edge, halfway down, and at its top, halfway across
        fun at(fit: PaintFit, x: Int, y: Int) = paint(x, y) { shadeStyle = units(PaintUnits.BOUNDS, fit); rectangle(0.0, 0.0, 200.0, 100.0) }
        val tolerance = 0.02
        assertNear(Vector2(1.0, 0.5), at(PaintFit.STRETCH, 199, 50), "stretch, right", tolerance)
        // fit: the square is 100 wide in the middle, the right edge is half a square past it
        assertNear(Vector2(1.5, 0.5), at(PaintFit.FIT, 199, 50), "fit, right", tolerance)
        assertNear(Vector2(0.5, 0.0), at(PaintFit.FIT, 100, 0), "fit, top", tolerance)
        // fill: the square is 200 high, the top edge is a quarter of it in
        assertNear(Vector2(1.0, 0.25), at(PaintFit.FILL, 199, 0), "fill, top right", tolerance)
        // circles have square bounds, fitting changes nothing
        val circle = paint(100, 50) { shadeStyle = units(PaintUnits.BOUNDS, PaintFit.FIT); circle(100.0, 50.0, 40.0) }
        assertNear(Vector2(0.5, 0.5), circle, "fit, a circle", tolerance)
    }
}
