package test

import org.openrndr.extra.gcode.roundedTo
import kotlin.test.Test
import kotlin.test.assertEquals

class TestDouble {
    @Test
    fun roundedTo() {
        data class Test(val decimals: Int, val value: Double, val want: String)

        listOf(
            Test(3, 0.0, "0.0"),
            Test(0, 0.0, "0"),
            Test(3, 123.0, "123.0"),
            Test(0, 123.0, "123"),
            Test(3, 123.1234567890, "123.123"),
            Test(0, 123.1234567890, "123"),
            Test(3, 123.1239, "123.124"),
            Test(3, -123.1239, "-123.124"),
            Test(0, -123.1239, "-123"),
            Test(-2, -123.1239, "-123.12"),
        ).forEach {
            assertEquals(it.want, it.value.roundedTo(it.decimals))
        }
    }
}