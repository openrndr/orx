import org.openrndr.extra.parameters.*
import kotlin.reflect.KProperty1
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class Inner {
    @DoubleParameter("amount", 0.0, 1.0)
    var amount = 0.5

    @NestedParameter("deeper")
    val deeper = Deepest()
}

class Deepest {
    @IntParameter("count", 1, 10)
    var count = 3
}

class Outer {
    @DoubleParameter("gain", 0.0, 1.0, order = 0)
    var gain = 1.0

    @NestedParameter("inner", order = 1)
    val inner = Inner()

    @NestedParameter("swappable", order = 2)
    var swappable: Inner? = null

    /* not public, not listed */
    @NestedParameter("hidden")
    private val hidden = Inner()

    /* not annotated, not listed */
    val plain = Inner()
}

class Cyclic {
    @NestedParameter("self")
    val self: Cyclic get() = this
}

class TestNestedParameters {
    private val outer = Outer()
    private val list = outer.listParameters()

    @Test
    fun listsNestedParametersInOrder() {
        assertEquals(
            listOf(ParameterType.Double, ParameterType.Nested, ParameterType.Nested),
            list.map { it.parameterType }
        )
        assertEquals("inner", list[1].label)
        assertNull(list[1].property)
        assertEquals("inner", list[1].nestedProperty?.name)
    }

    @Test
    fun listsTheParametersOfNestedObjectsRecursively() {
        val inner = list[1].nestedParameters!!
        assertEquals(setOf(ParameterType.Double, ParameterType.Nested), inner.map { it.parameterType }.toSet())
        val deeper = inner.first { it.parameterType == ParameterType.Nested }
        assertEquals("deeper", deeper.label)
        assertEquals(listOf("count"), deeper.nestedParameters!!.map { it.property?.name })
    }

    @Test
    fun readsTheNestedObjectThroughTheNestedProperty() {
        @Suppress("UNCHECKED_CAST")
        assertSame(outer.inner, (list[1].nestedProperty as KProperty1<Any, Any?>).get(outer))
    }

    @Test
    fun listsNoNestedParametersForNull() {
        assertTrue(list[2].nestedParameters!!.isEmpty())
    }

    @Test
    fun rejectsObjectsThatNestThemselves() {
        assertFailsWith<IllegalArgumentException> { Cyclic().listParameters() }
    }
}
