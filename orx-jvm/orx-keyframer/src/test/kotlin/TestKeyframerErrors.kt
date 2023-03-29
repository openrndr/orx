import org.amshove.kluent.`should throw`
import org.amshove.kluent.invoking
import org.openrndr.extra.expressions.ExpressionException
import kotlin.test.Test

import org.openrndr.extra.keyframer.Keyframer
import org.openrndr.extra.keyframer.KeyframerFormat
import java.io.File
import kotlin.IllegalStateException

private fun testFile(path: String): File {
    val test = File(".")
    return if (test.absolutePath.replace("\\","/").endsWith("orx-keyframer/.")) {
        File(path)
    } else {
        File("orx-keyframer/$path")
    }
}

class TestKeyframerErrors {
    class Animation : Keyframer() {
        val position by Vector2Channel(arrayOf("x", "y"))
    }

    @Test
    fun `loading a faulty json`() {
        val animation = Animation()
        val json = """
        """
        invoking { animation.loadFromJsonString(json) } `should throw` (IllegalStateException::class)
   }

    @Test
    fun `loading a non existing json`() {
        val animation = Animation()

        invoking { animation.loadFromJson(testFile("this-does-not-exist")) } `should throw` (IllegalArgumentException::class)

    }
    @Test
    fun `loading a json with a faulty time expression (1)`() {

        File(".").apply {
            println(this.absolutePath)
        }


        val animation = Animation()

        invoking {
            animation.loadFromJson(
                testFile("src/test/resources/error-reporting/time-01.json"),
                format = KeyframerFormat.SIMPLE
            )
        } `should throw` ExpressionException::class //`with message` "Error loading from '${testName("src/test/resources/error-reporting/time-01.json")}': error in keys[0].'time': parser error in expression: ')('; [line: 1, character: 0 , near: [@0,0:0=')',<21>,1:0] ]"

    }

    // Paths.sep
    //
    //Expected <Error loading from 'orx-keyframer/src\test\resources\error-reporting\time-01.json': error in keys[0].'time': parser error in expression: ')('; [line: 1, character: 0 , near: [@0,0:0=')',<21>,1:0] ]>,
    //  actual <Error loading from 'orx-keyframer\src\test\resources\error-reporting\time-01.json': error in keys[0].'time': parser error in expression: ')('; [line: 1, character: 0 , near: [@0,0:0=')',<21>,1:0] ]>.

    @Test
    fun `loading a json with a faulty time expression (2) `() {
        val animation = Animation()
        invoking {
            animation.loadFromJson(
                testFile("src/test/resources/error-reporting/time-02.json"),
                format = KeyframerFormat.SIMPLE
            )
        } `should throw` ExpressionException::class //`with message` "Error loading from '${testName("src/test/resources/error-reporting/time-02.json")}': error in keys[0].'time': error in evaluation of 'doesNotExist': unresolved variable: 'doesNotExist'"

    }
    @Test
    fun `loading a json with a non-existing easing`() {
        val animation = Animation()
        invoking {
            animation.loadFromJson(
                testFile("src/test/resources/error-reporting/easing.json"),
                format = KeyframerFormat.SIMPLE
            )
        } `should throw` ExpressionException::class //`with message` "Error loading from '${testName("src/test/resources/error-reporting/easing.json")}': error in keys[0].'easing': unknown easing name 'garble'"
    }

    @Test
    fun `loading a json with a faulty value (1)`() {
        val animation = Animation()

        invoking {
            animation.loadFromJson(
                testFile("src/test/resources/error-reporting/value-01.json"),
                format = KeyframerFormat.SIMPLE
            )
        } `should throw` ExpressionException::class //`with message` "Error loading from '${testName("src/test/resources/error-reporting/value-01.json")}': error in keys[0].'x': error in evaluation of 'garble': unresolved variable: 'garble'"
    }
}
