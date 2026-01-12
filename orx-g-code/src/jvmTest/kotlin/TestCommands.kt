package test

import kotlin.test.*
import org.openrndr.extra.gcode.Command
import org.openrndr.extra.gcode.withoutDuplicates

class TestCommands {

    @Test
    fun `empty commands`() {
        assertContentEquals(emptyList<Command>(), emptyList<Command>().withoutDuplicates())
    }

    @Test
    fun `should remove duplicate commands, keeping comments`() {
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