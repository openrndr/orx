import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.openrndr.extra.crashhandler.CrashHandler
import org.openrndr.extra.crashhandler.SlackReporter
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

/**
 * Test SlackReporter okhttp3 integration
 */
class TestSlackReporter {
    private lateinit var crashHandler: CrashHandler
    private lateinit var slackReporter: SlackReporter

    @BeforeTest
    fun setup() {
        crashHandler = CrashHandler()
        slackReporter = SlackReporter(crashHandler)
        slackReporter.channelId = "test-channel"
        slackReporter.authToken = "test-token"
    }

    @Test
    fun `test OkHttpClient can be created`() {
        OkHttpClient()
    }

    @Test
    fun `test Request can be built`() {
        val request = Request.Builder()
            .url("https://slack.com/api/chat.postMessage")
            .method("POST", "{}".toRequestBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer test-token")
            .build()

        assertEquals("https://slack.com/api/chat.postMessage", request.url.toString())
        assertEquals("POST", request.method)
        assertEquals("application/json", request.headers["Content-Type"])
        assertEquals("Bearer test-token", request.headers["Authorization"])
    }

    @Test
    fun `test SlackReporter has correct channel and token`() {
        assertEquals("test-channel", slackReporter.channelId)
        assertEquals("test-token", slackReporter.authToken)
    }

    @Test
    fun `test OkHttpClient builder pattern`() {
        OkHttpClient.Builder().build()
    }

    @Test
    fun `test OkHttpClient newBuilder pattern`() {
        OkHttpClient().newBuilder().build()
    }

    @Test
    fun `test okhttp3 Response can be created`() {
        val response = Response.Builder()
            .code(200)
            .message("OK")
            .protocol(Protocol.HTTP_1_1)
            .request(Request.Builder().url("https://example.com").build())
            .build()

        assertEquals(200, response.code)
        assertEquals("OK", response.message)
        assertEquals(true, response.isSuccessful)
    }

    @Test
    fun `test okhttp3 toMediaType`() {
        val mediaType = "application/json".toMediaType()
        assertEquals("application/json", mediaType.toString())
    }

    @Test
    fun `test okhttp3 toRequestBody`() {
        val body = "test body".toRequestBody("application/json".toMediaType())
    }

    @Serializable
    private data class TestRequest(
        val channel: String,
        val text: String? = null
    )

    @Test
    fun `test JSON serialization works with okhttp3`() {
        val json = Json {
            ignoreUnknownKeys = true
        }

        val request = TestRequest(channel = "test-channel", text = "test message")
        val jsonString = json.encodeToString(request)

        assertContains(jsonString, "test-channel")
        assertContains(jsonString, "test message")

        // Test deserialization
        val decoded = json.decodeFromString<TestRequest>(jsonString)
        assertEquals("test-channel", decoded.channel)
        assertEquals("test message", decoded.text)
    }

    @Test
    fun `test Request with JSON body`() {
        val json = Json { ignoreUnknownKeys = true }
        val message = mapOf("channel" to "test-channel", "text" to "hello")
        val body = json.encodeToString(message)
        val requestBody = body.toRequestBody("application/json".toMediaType())

        val url = "https://slack.com/api/chat.postMessage"
        val method = "POST"

        val request = Request.Builder()
            .url(url)
            .method(method, requestBody)
            .build()

        assertEquals(method, request.method)
        assertEquals(url, request.url.toString())
    }
}
