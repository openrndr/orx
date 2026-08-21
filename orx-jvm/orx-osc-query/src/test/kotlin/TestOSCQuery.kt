import com.google.gson.JsonParser
import com.illposed.osc.OSCMessage
import com.illposed.osc.transport.OSCPortOut
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.oscquery.OSCQuery
import org.openrndr.extra.parameters.ActionParameter
import org.openrndr.extra.parameters.ColorParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import java.net.InetAddress
import java.net.URI
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestOSCQuery {

    private val port = 19347

    private val settings = @Description("Settings") object {
        @DoubleParameter("radius", 0.0, 100.0, order = 10)
        var radius = 5.0

        @IntParameter("sides", 3, 12, order = 20)
        var sides = 6

        @ColorParameter("fill", order = 30)
        var color = ColorRGBa.WHITE

        var actionCalls = 0

        @ActionParameter("trigger", order = 40)
        fun trigger() {
            actionCalls++
        }
    }

    private val oscQuery = OSCQuery(oscPort = port, httpPort = port, name = "test").also {
        it.add(settings)
    }

    @AfterTest
    fun tearDown() = oscQuery.stop()

    private fun httpGet(path: String): String =
        URI("http://127.0.0.1:$port$path").toURL().readText()

    @Test
    fun `serves the full namespace with the expected paths`() {
        val root = JsonParser.parseString(httpGet("/")).asJsonObject
        assertEquals("/", root["FULL_PATH"].asString)
        val settingsNode = root["CONTENTS"].asJsonObject["Settings"].asJsonObject
        val contents = settingsNode["CONTENTS"].asJsonObject
        assertTrue(contents.has("radius"))
        assertTrue(contents.has("sides"))
        assertTrue(contents.has("fill"))
        assertTrue(contents.has("trigger"))

        val radius = contents["radius"].asJsonObject
        assertEquals("/Settings/radius", radius["FULL_PATH"].asString)
        assertEquals("f", radius["TYPE"].asString)
        assertEquals(3, radius["ACCESS"].asInt)
        assertEquals(5.0, radius["VALUE"].asJsonArray[0].asDouble)
        val range = radius["RANGE"].asJsonArray[0].asJsonObject
        assertEquals(0.0, range["MIN"].asDouble)
        assertEquals(100.0, range["MAX"].asDouble)

        assertEquals("i", contents["sides"].asJsonObject["TYPE"].asString)
        assertEquals("r", contents["fill"].asJsonObject["TYPE"].asString)
        // an action is write-only and impulse-typed
        assertEquals("N", contents["trigger"].asJsonObject["TYPE"].asString)
        assertEquals(2, contents["trigger"].asJsonObject["ACCESS"].asInt)
    }

    @Test
    fun `serves HOST_INFO`() {
        val info = JsonParser.parseString(httpGet("/?HOST_INFO")).asJsonObject
        assertEquals("test", info["NAME"].asString)
        assertEquals(port, info["OSC_PORT"].asInt)
        assertEquals("UDP", info["OSC_TRANSPORT"].asString)
        assertTrue(info["EXTENSIONS"].asJsonObject["VALUE"].asBoolean)
    }

    @Test
    fun `serves a single VALUE attribute reflecting live changes`() {
        settings.radius = 42.0
        val value = JsonParser.parseString(httpGet("/Settings/radius?VALUE")).asJsonArray
        assertEquals(42.0, value[0].asDouble)
    }

    @Test
    fun `applies incoming OSC updates to bound properties`() {
        val sender = OSCPortOut(InetAddress.getByName("127.0.0.1"), port)
        sender.connect()

        sender.send(OSCMessage("/Settings/radius", listOf(77.5f)))
        sender.send(OSCMessage("/Settings/sides", listOf(9)))
        sender.send(OSCMessage("/Settings/fill", listOf(1.0f, 0.0f, 0.0f, 1.0f)))
        sender.send(OSCMessage("/Settings/trigger", emptyList<Any>()))

        // give the OSC receiver a moment to dispatch
        val deadline = System.currentTimeMillis() + 2000
        while (System.currentTimeMillis() < deadline &&
            (settings.radius != 77.5 || settings.sides != 9 || settings.actionCalls == 0)
        ) {
            Thread.sleep(10)
        }
        sender.close()

        assertEquals(77.5, settings.radius, 1e-6)
        assertEquals(9, settings.sides)
        assertEquals(1.0, settings.color.r, 1e-6)
        assertEquals(0.0, settings.color.g, 1e-6)
        assertEquals(1, settings.actionCalls)
    }
}
