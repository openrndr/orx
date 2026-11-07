import org.openrndr.extra.parameters.*
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class Described {
    @DoubleParameter("size", 0.0, 10.0, description = "the size of the thing")
    var size = 1.0

    @IntParameter("count", 1, 10)
    var count = 3

    @ActionParameter("go", description = "starts it")
    fun go() {}
}

class TestParameterHistory {
    @Test
    fun descriptionsAreRead() {
        val parameters = Described().listParameters()
        assertEquals("the size of the thing", parameters.first { it.label == "size" }.description)
        assertNull(parameters.first { it.label == "count" }.description)
        assertEquals("starts it", parameters.first { it.label == "go" }.description)
    }

    @Test
    fun parametersResetToTheirDefaults() {
        val bindings = ParameterBindings()
        val described = Described()
        val binding = bindings.add(described, "d")!!
        bindings["d", "size"] = 5.0
        bindings["d", "count"] = 7
        bindings.reset(binding, binding.parameter("size")!!)
        assertEquals(1.0, described.size)
        assertEquals(7, described.count)
        bindings.resetAll()
        assertEquals(3, described.count)
    }

    @Test
    fun changesAreUndoneAndRedoneInSteps() {
        var now = 0.0
        val bindings = ParameterBindings()
        val described = Described()
        bindings.add(described, "d")
        val history = ParameterHistory(bindings) { now }
        assertFalse(history.canUndo)

        // a drag: many changes to one parameter in quick succession, one step
        for (i in 1..10) {
            now += 0.1
            bindings["d", "size"] = 1.0 + i * 0.1
        }
        // later, another parameter: another step
        now += 5.0
        bindings["d", "count"] = 8
        now += 5.0
        bindings["d", "count"] = 9

        assertTrue(history.undo())
        assertEquals(8, described.count)
        assertTrue(history.undo())
        assertEquals(3, described.count)
        assertTrue(history.undo())
        assertEquals(1.0, described.size)
        assertFalse(history.undo())

        assertTrue(history.redo())
        assertEquals(2.0, described.size, 1E-9)
        // a new change forgets what can be redone
        now += 5.0
        bindings["d", "count"] = 2
        assertFalse(history.canRedo)
        assertTrue(history.undo())
        assertEquals(3, described.count)
    }

    @Test
    fun changesMadeAtOnceAreOneStep() {
        var now = 0.0
        val bindings = ParameterBindings()
        val described = Described()
        bindings.add(described, "d")
        val history = ParameterHistory(bindings) { now }
        bindings.randomize(1.0, Random(1))
        assertTrue(described.size != 1.0 || described.count != 3)
        history.undo()
        assertEquals(1.0, described.size)
        assertEquals(3, described.count)
        assertFalse(history.canUndo)
    }
}
