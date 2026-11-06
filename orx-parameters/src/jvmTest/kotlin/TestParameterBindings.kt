import org.openrndr.color.ColorRGBa
import org.openrndr.extra.parameters.*
import org.openrndr.math.Vector2
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

enum class Shape { CIRCLE, SQUARE }

/** a list with a selected element, like the SelectableMutableList of orx-panel */
class Choices(elements: List<String>, override var selectedIndex: Int = 0) : ArrayList<String>(elements), Selectable

@Description("Settings")
class Settings {
    @DoubleParameter("radius", 0.0, 100.0)
    var radius = 10.0

    @IntParameter("count", 1, 10)
    var count = 3

    @ColorParameter("color")
    var color = ColorRGBa.RED

    @XYParameter("position", minX = 0.0, maxX = 10.0, minY = 0.0, maxY = 10.0)
    var position = Vector2(5.0, 5.0)

    @OptionParameter("shape")
    var shape = Shape.CIRCLE

    @MutableListParameter("choice")
    var choices = Choices(listOf("a", "b", "c"))

    var pressed = 0

    @ActionParameter("press")
    fun press() {
        pressed++
    }

    @NestedParameter("inner")
    var inner: Inner? = Inner()
}

class TestParameterBindings {
    @Test
    fun bindsObjectsAndNestedObjectsByLabel() {
        val bindings = ParameterBindings()
        val settings = Settings()
        bindings.add(settings)
        bindings.add(Settings())
        bindings.add(Any())
        assertEquals(listOf("Settings", "Settings / 2"), bindings.bindings.map { it.label })
        assertEquals(
            listOf("Settings", "Settings/inner", "Settings/inner/deeper", "Settings / 2", "Settings / 2/inner", "Settings / 2/inner/deeper"),
            bindings.all().map { it.label }
        )
        assertSame(settings.inner, bindings.binding("Settings/inner")?.obj)
        assertEquals(3, bindings["Settings/inner/deeper", "count"])
        bindings.remove(settings)
        assertEquals(listOf("Settings / 2"), bindings.bindings.map { it.label })
    }

    @Test
    fun setsValuesAndSendsChanges() {
        val bindings = ParameterBindings()
        val settings = Settings()
        bindings.add(settings, "s")
        val changes = mutableListOf<String>()
        bindings.changed.listen { changes.add("${it.binding.label}.${it.parameter.name}=${it.value}") }
        bindings["s", "radius"] = 20.0
        bindings["s/inner", "amount"] = 0.25
        bindings["s", "choices"] = "c"
        bindings.invoke("s", "press")
        assertEquals(20.0, settings.radius)
        assertEquals(0.25, settings.inner!!.amount)
        assertEquals(2, settings.choices.selectedIndex)
        assertEquals(1, settings.pressed)
        assertEquals(listOf("s.radius=20.0", "s/inner.amount=0.25", "s.choices=[a, b, c]", "s.press=null"), changes)
    }

    @Test
    fun rebindsNestedObjects() {
        val bindings = ParameterBindings()
        val settings = Settings()
        bindings.add(settings, "s")
        val before = bindings.binding("s/inner")
        settings.inner = Inner()
        val rebound = bindings.rebindNested()
        assertEquals(listOf("s/inner"), rebound.map { it.label })
        assertNotSame(before, bindings.binding("s/inner"))
        assertSame(settings.inner, bindings.binding("s/inner")?.obj)
        settings.inner = null
        bindings.rebindNested()
        assertNull(bindings.binding("s/inner"))
        assertEquals(listOf("s"), bindings.all().map { it.label })
    }

    @Test
    fun savesAndLoadsValues() {
        val bindings = ParameterBindings()
        val settings = Settings()
        bindings.add(settings, "s")
        settings.radius = 42.0
        settings.color = ColorRGBa.BLUE
        settings.shape = Shape.SQUARE
        settings.choices.add("d")
        settings.choices.selectedIndex = 3
        settings.inner!!.deeper.count = 7
        val json = bindings.toJson()

        val loaded = ParameterBindings()
        val other = Settings()
        loaded.add(other, "s")
        var loads = 0
        loaded.loaded.listen { loads++ }
        loaded.fromJson(json)
        assertEquals(42.0, other.radius)
        assertEquals(ColorRGBa.BLUE, other.color)
        assertEquals(Shape.SQUARE, other.shape)
        assertEquals(listOf("a", "b", "c", "d"), other.choices)
        assertEquals(3, other.choices.selectedIndex)
        assertEquals(7, other.inner!!.deeper.count)
        assertEquals(1, loads)
    }

    @Test
    fun readsTheFilesOfTheGui() {
        // the format that orx-gui saved
        val json = """{"s":{"radius":{"doubleValue":12.5,"minValue":0.0,"maxValue":100.0},"shape":{"optionValue":"SQUARE"},"unknown":{"intValue":1}},"missing":{}}"""
        val bindings = ParameterBindings()
        val settings = Settings()
        bindings.add(settings, "s")
        bindings.fromJson(json)
        assertEquals(12.5, settings.radius)
        assertEquals(Shape.SQUARE, settings.shape)
    }

    @Test
    fun writesAndReadsTheJsonOfGson() {
        // as Gson wrote it, with HTML characters escaped
        val json = """{"s \u003c1\u003e":{"color":{"colorValue":{"r":0.1,"g":0.2,"b":0.3,"alpha":1.0,"linearity":"LINEAR"}},""" +
                """"choices":{"stringListValue":["a \u0026 b","c"],"selectedIndex":1,"listElementType":"String"},""" +
                """"position":{"vector2Value":{"x":1.0,"y":2.0}},"radius":{"doubleValue":1.0E-7,"minValue":0.0,"maxValue":100.0}}}"""
        val bindings = ParameterBindings()
        val settings = Settings()
        bindings.add(settings, "s <1>")
        bindings.fromJson(json)
        assertEquals(ColorRGBa(0.1, 0.2, 0.3), settings.color)
        assertEquals(listOf("a & b", "c"), settings.choices)
        assertEquals(1, settings.choices.selectedIndex)
        assertEquals(Vector2(1.0, 2.0), settings.position)
        assertEquals(1E-7, settings.radius)
        // default values are written, null values are not
        val written = bindings.toJson()
        assertTrue(""""colorValue":{"r":0.1,"g":0.2,"b":0.3,"alpha":1.0,"linearity":"LINEAR"}""" in written)
        assertTrue(""""radius":{"doubleValue":1.0E-7,"minValue":0.0,"maxValue":100.0}""" in written)
        assertTrue(""""press":{}""" in written)
        assertTrue("null" !in written)
    }

    @Test
    fun randomizesWithinRanges() {
        val bindings = ParameterBindings()
        val settings = Settings()
        bindings.add(settings, "s")
        repeat(20) { bindings.randomize(1.0, Random(it)) }
        assertTrue(settings.radius in 0.0..100.0)
        assertTrue(settings.count in 1..10)
        assertTrue(settings.position.x in 0.0..10.0 && settings.position.y in 0.0..10.0)
        assertTrue(settings.inner!!.amount in 0.0..1.0)
        // a strength of 0 keeps the values
        val radius = settings.radius
        bindings.randomize(0.0)
        assertEquals(radius, settings.radius)
        // only the parameters that pass the filter
        val count = settings.count
        bindings.randomize(1.0, Random(99)) { _, parameter -> parameter.name == "radius" }
        assertEquals(count, settings.count)
    }
}
