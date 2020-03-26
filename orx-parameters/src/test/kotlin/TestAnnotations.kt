import org.amshove.kluent.*
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.parameters.*
import org.openrndr.math.Vector2
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

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
}

object TestAnnotations : Spek({
    describe("an annotated object") {
        it("has listable parameters") {
            val list = a.listParameters()
            list.size `should be equal to` 8

            list[0].property?.name `should be equal to` "d"
            list[0].parameterType `should be equal to` ParameterType.Double
            list[0].label `should be equal to` "a double scalar"
            list[0].doubleRange?.let {
                it.start shouldBeInRange 0.0..0.0001
                it.endInclusive shouldBeInRange 0.999..1.001
            }
            list[0].precision `should be equal to` 3

            list[1].property?.name `should be equal to` "i"
            list[1].parameterType `should be equal to` ParameterType.Int
            list[1].label `should be equal to` "an integer scalar"
            list[1].intRange?.let {
                it.first `should be equal to` 1
                it.last `should be equal to` 100
            }

            list[2].property?.name `should be equal to` "b"
            list[2].parameterType `should be equal to` ParameterType.Boolean
            list[2].label `should be equal to` "a boolean parameter"
            list[2].precision `should be equal to` null

            list[3].parameterType `should be equal to` ParameterType.Action
            list[3].property `should be equal to` null
            list[3].label `should be equal to` "an action parameter"

            /*  test if we can call the annotated function, this is performed by raising an expected exception in the
                function in the annotated function.
            */
            invoking {
                try {
                    list[3].function?.call(a)
                } catch (e: java.lang.reflect.InvocationTargetException) {
                    /* this unpacks the exception that is wrapped in the ITExc */
                    throw(e.targetException)
                }
            } `should throw` IllegalStateException::class withMessage "this is to test if this function can be called"

            list[4].parameterType `should be equal to` ParameterType.Text
            list[4].property?.name `should be equal to` "t"
            list[4].label `should be equal to` "a text parameter"

            list[5].parameterType `should be equal to` ParameterType.Color
            list[5].property?.name `should be equal to` "c"
            list[5].label `should be equal to` "a color parameter"

            list[6].parameterType `should be equal to` ParameterType.XY
            list[6].property?.name `should be equal to` "xy"
            list[6].label `should be equal to` "an XY parameter"

            list[7].parameterType `should be equal to` ParameterType.DoubleList
            list[7].property?.name `should be equal to` "dl"
            list[7].label `should be equal to` "a double list parameter"
        }
    }
})
