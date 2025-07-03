import io.kotest.assertions.throwables.shouldThrowUnit
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.parameters.*
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import kotlin.test.assertEquals

val a = object {
    @DoubleParameter("a double scalar", 0.0, 1.0, order = 0)
    var d = 1.0

    @IntParameter("an integer scalar", 1, 100, order = 1)
    var i = 1

    @BooleanParameter("a boolean parameter", order = 2)
    var b = false

    @ActionParameter("an action parameter", order = 3)
    fun a() {
        error("this is to test if this function can be called")
    }

    @TextParameter("a text parameter", order = 4)
    var t = "test"

    @ColorParameter("a color parameter", order = 5)
    var c = ColorRGBa.WHITE

    @XYParameter("an XY parameter", order = 6)
    var xy = Vector2.ZERO

    @DoubleListParameter("a double list parameter", order = 7)
    var dl = mutableListOf<Double>()

    @Vector2Parameter("a vector 2 parameter", order = 8)
    var v2 = Vector2.ZERO

    @Vector3Parameter("a vector 3 parameter", order = 9)
    var v3 = Vector3.ZERO

    @Vector4Parameter("a vector 4 parameter", order = 10)
    var v4 = Vector4.ZERO

    @OptionParameter("an option parameter", order = 11)
    var o = ParameterType.Option

    @PathParameter("a path parameter", order = 12)
    var p = "bla.png"

}

class TestAnnotations : DescribeSpec({
    describe("an annotated object") {
        it("has listable parameters") {
            val list = a.listParameters()
            list.size shouldBeEqual 13

            list[0].property?.name shouldBe "d"
            list[0].parameterType shouldBeEqual ParameterType.Double
            list[0].label shouldBeEqual "a double scalar"
            list[0].doubleRange?.let {
                it.start.shouldBe(0.0.plusOrMinus(0.001))
                it.endInclusive.shouldBe(1.0.plusOrMinus(0.001))
            }
            list[0].precision?.shouldBeEqual(3)

            list[1].property?.name?.shouldBeEqual("i")
            list[1].parameterType shouldBeEqual ParameterType.Int
            list[1].label shouldBeEqual "an integer scalar"
            list[1].intRange?.let {
                it.first shouldBeEqual 1
                it.last shouldBeEqual 100
            }

            list[2].property?.name?.shouldBeEqual("b")
            list[2].parameterType shouldBeEqual ParameterType.Boolean
            list[2].label shouldBeEqual "a boolean parameter"
            list[2].precision?.shouldBeNull()

            list[3].parameterType shouldBeEqual ParameterType.Action
            list[3].property?.shouldBeNull()
            list[3].label shouldBeEqual "an action parameter"

            /*  test if we can call the annotated function, this is performed by raising an expected exception in the
                function in the annotated function.
            */
            shouldThrowUnit<IllegalStateException> {

                try {
                    list[3].function?.call(a)
                } catch (e: java.lang.reflect.InvocationTargetException) {
                    /* this unpacks the exception that is wrapped in the ITExc */
                    throw (e.targetException)
                }
            }


            list[4].parameterType shouldBeEqual ParameterType.Text
            list[4].property?.name?.shouldBeEqual("t")
            list[4].label shouldBeEqual "a text parameter"

            list[5].parameterType shouldBeEqual ParameterType.Color
            list[5].property?.name?.shouldBeEqual("c")
            list[5].label shouldBeEqual "a color parameter"

            list[6].parameterType shouldBeEqual ParameterType.XY
            list[6].property?.name?.shouldBeEqual("xy")
            list[6].label shouldBeEqual "an XY parameter"

            list[7].parameterType shouldBeEqual ParameterType.DoubleList
            list[7].property?.name?.shouldBeEqual("dl")
            list[7].label shouldBeEqual "a double list parameter"

            list[8].parameterType shouldBeEqual ParameterType.Vector2
            list[8].property?.name?.shouldBeEqual("v2")
            list[8].label shouldBeEqual "a vector 2 parameter"

            list[9].parameterType shouldBeEqual ParameterType.Vector3
            list[9].property?.name?.shouldBeEqual("v3")
            list[9].label shouldBeEqual "a vector 3 parameter"

            list[10].parameterType shouldBeEqual ParameterType.Vector4
            list[10].property?.name?.shouldBeEqual("v4")
            list[10].label shouldBeEqual "a vector 4 parameter"

            list[11].parameterType shouldBeEqual ParameterType.Option
            list[11].property?.name?.shouldBeEqual("o")
            list[11].label shouldBeEqual "an option parameter"

            assertEquals(ParameterType.Path, list[12].parameterType)
            assertEquals("p", list[12].property?.name)
            assertEquals("a path parameter", list[12].label)
            assertEquals(false, list[12].absolutePath)
            assertEquals("null", list[12].pathContext)

        }
    }
})
