import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldBeInRange
import org.openrndr.extra.parameters.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

val a = object {
    @DoubleParameter("a double scalar", 0.0, 1.0, order = 0)
    var d = 1.0

    @IntParameter("an integer scalar", 1, 100, order = 1)
    var i = 1

    @BooleanParameter("a boolean parameter", order = 2)
    var b = false

    @ButtonParameter("a button parameter", order = 3)
    var f = {}
}

object TestAnnotations : Spek({
    describe("an annotated object") {
        it("has listable parameters") {
            val list = a.listParameters()
            list.size `should be equal to` 4
            list[0].property.name `should be equal to` "d"
            list[0].parameterType `should be equal to` ParameterType.Double
            list[0].label `should be equal to` "a double scalar"
            list[0].doubleRange?.let {
                it.start shouldBeInRange 0.0 .. 0.0001
                it.endInclusive shouldBeInRange 0.999 .. 1.001
            }
            list[0].precision `should be equal to` 3

            list[1].property.name `should be equal to` "i"
            list[1].parameterType `should be equal to` ParameterType.Int
            list[1].label `should be equal to` "an integer scalar"
            list[1].intRange?.let {
                it.start `should be equal to` 1
                it.endInclusive `should be equal to` 100
            }

            list[2].property.name `should be equal to` "b"
            list[2].parameterType `should be equal to` ParameterType.Boolean
            list[2].label `should be equal to` "a boolean parameter"
            list[2].precision `should be equal to` null

            list[3].parameterType `should be equal to` ParameterType.Button
            list[3].property.name `should be equal to` "f"
        }
    }
})
