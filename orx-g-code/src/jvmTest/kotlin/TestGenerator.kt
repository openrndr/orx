package test

import org.amshove.kluent.`should be equal to`
import org.openrndr.extra.gcode.Command
import org.openrndr.extra.gcode.roundedTo
import org.openrndr.extra.gcode.withoutDuplicates
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.test.assertContentEquals

object TestGenerator : Spek({

    describe("Double.roundedTo") {

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
            it("${it.value} with ${it.decimals} decimals -> ${it.want}") {
                it.value.roundedTo(it.decimals) `should be equal to` it.want
            }
        }
    }


    describe("Commands.withoutDuplicates") {

        it("empty commands") {
            assertContentEquals(emptyList<Command>(), emptyList<Command>().withoutDuplicates())
        }

        it("should remove duplicate commands, keeping comments") {

            val commands = listOf(
                "G0 X1 Y2 F3",
                "G0 X2 Y2 F3",
                "G0 X2 Y2 F3",
                ";G0 X2 Y2 F3",
                "(G0 X2 Y2 F3)",
                "G0 X2 Y2 F3",
                "G0 X3 Y2 F3",
            )

            val expected = listOf(
                "G0 X1 Y2 F3",
                "G0 X2 Y2 F3",
                ";G0 X2 Y2 F3",
                "(G0 X2 Y2 F3)",
                "G0 X3 Y2 F3",
            )

            assertContentEquals(expected, commands.withoutDuplicates())
        }
    }
})