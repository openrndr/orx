import org.openrndr.extra.crashhandler.CrashHandler
import org.openrndr.extra.crashhandler.Reporter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for CrashHandler and Reporter base classes.
 */
class TestCrashHandler {

    @Test
    fun `test CrashHandler can be created`() {
        val handler = CrashHandler()
        handler.name = "test-handler"
        handler.vncHost = "localhost:5900"
        assertEquals("test-handler", handler.name)
        assertEquals("localhost:5900", handler.vncHost)
        assertTrue(handler.reporters.isEmpty())
    }

    @Test
    fun `test CrashHandler reporters list can have items added`() {
        val handler = CrashHandler()
        val reporter = object : Reporter(handler) {
            override fun reportCrash(throwable: Throwable) {
                // Test implementation
            }
        }

        handler.reporters.add(reporter)
        assertEquals(1, handler.reporters.size)
    }
}
