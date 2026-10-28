import org.junit.jupiter.api.Test
import org.openrndr.draw.loadFont
import org.openrndr.extra.testing.AbstractApplicationTestFixture
import org.openrndr.extra.textwriter.writer

class TestTextWriter: AbstractApplicationTestFixture() {

    @Test
    fun test() {
        val drawer = program.drawer
        drawer.fontMap = program.loadFont("../demo-data/fonts/IBMPlexMono-Regular.ttf", 32.0)
        val h0 = program.writer {
            val sy = cursor.y
            newLine()
            text("Hello World")
            cursor.y - sy
        }
    }
}