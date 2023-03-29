import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNear
import org.openrndr.extra.expressions.FunctionExtensions
import org.openrndr.extra.expressions.evaluateExpression

import kotlin.test.Test
class TestExpressions {
    @Test
    fun `a value reference`() {
        val expression = "someValue"
        val result = evaluateExpression(expression, constants= mapOf("someValue" to 5.0))
        result?.shouldBeEqualTo(5.0)
    }

    @Test
    fun `a backticked value reference`() {
        val expression = "`some-value`"
        val result = evaluateExpression(expression, constants= mapOf("some-value" to 5.0))
        result?.shouldBeEqualTo(5.0)
    }


    @Test
    fun `a function call`() {
        val expression = "sqrt(4.0)"
        val result = evaluateExpression(expression)
        result?.shouldBeNear(2.0, 10E-6)
    }

    @Test
    fun `a function call with the name in backticks`() {
        val expression = "`sqrt`(4.0)"
        val result = evaluateExpression(expression)
        result?.shouldBeNear(2.0, 10E-6)
    }

    @Test
    fun `two function calls`() {
        val expression = "sqrt(4.0) * sqrt(4.0)"
        val result = evaluateExpression(expression)
        result?.shouldBeNear(4.0, 10E-6)
    }

    @Test
    fun `two argument max function call`() {
        val expression = "max(0.0, 4.0)"
        val result = evaluateExpression(expression)
        result?.shouldBeNear(4.0, 10E-6)
    }

    @Test
    fun `two argument min function call`() {
        val expression = "min(8.0, 4.0)"
        val result = evaluateExpression(expression)
        result?.shouldBeNear(4.0, 10E-6)
    }

    @Test
    fun `three argument function call`() {
        val expression = "mix(8.0, 4.0, 0.5)"
        val result = evaluateExpression(expression)
        result?.shouldBeNear(6.0, 10E-6)
    }

    @Test
    fun `five argument function call`() {
        val expression = "map(0.0, 1.0, 0.0, 8.0, 0.5)"
        val result = evaluateExpression(expression)
        result?.shouldBeNear(4.0, 10E-6)
    }

    @Test
    fun `two argument function call, where argument order matters`() {
        val expression = "pow(2.0, 3.0)"
        val result = evaluateExpression(expression)
        result?.shouldBeNear(8.0, 10E-6)
    }

    @Test
    fun `nested function call`() {
        val expression = "sqrt(min(8.0, 4.0))"
        val result = evaluateExpression(expression)
        result?.shouldBeNear(2.0, 10E-6)
    }

    @Test
    fun `extension function0 call`() {
        val expression = "extension()"
        val result = evaluateExpression(expression, functions = FunctionExtensions(functions0 = mapOf("extension" to { 2.0 })))
        result?.shouldBeNear(2.0, 10E-6)
    }

    @Test
    fun `extension function1 call`(){
        val expression = "extension(1.0)"
        val result = evaluateExpression(expression, functions = FunctionExtensions(functions1 = mapOf("extension" to { x -> x * 2.0 })))
        result?.shouldBeNear(2.0, 10E-6)
    }

    @Test
    fun `extension function1 call with dashed name in backticks`(){
        val expression = "`extension-function`(1.0)"
        val result = evaluateExpression(expression, functions = FunctionExtensions(functions1 = mapOf("extension-function" to { x -> x * 2.0 })))
        result?.shouldBeNear(2.0, 10E-6)
    }

    @Test
    fun `extension function2 call`() {
        val expression = "extension(1.0, 1.0)"
        val result = evaluateExpression(expression, functions = FunctionExtensions(functions2 = mapOf("extension" to { x, y -> x + y })))
        result?.shouldBeNear(2.0, 10E-6)
    }

    @Test
    fun `extension function3 call`() {
        val expression = "extension(1.0, 1.0, 1.0)"
        val result = evaluateExpression(expression, functions = FunctionExtensions(functions3 = mapOf("extension" to { x, y, z -> x + y + z})))
        result?.shouldBeNear(3.0, 10E-6)
    }

    @Test
    fun `extension function4 call`() {
        val expression = "extension(1.0, 1.0, 1.0, 1.0)"
        val result = evaluateExpression(expression, functions = FunctionExtensions(functions4 = mapOf("extension" to { x, y, z, w  -> x + y + z + w})))
        result?.shouldBeNear(4.0, 10E-6)
    }

    @Test
    fun `extension function5 call`() {
        val expression = "extension(1.0, 1.0, 1.0, 1.0, 1.0)"
        val result = evaluateExpression(expression, functions = FunctionExtensions(functions5 = mapOf("extension" to { x, y, z, w, u -> x + y + z + w + u})))
        result?.shouldBeNear(5.0, 10E-6)
    }
}