package org.openrndr.extra.crashhandler

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response


@Serializable
private data class BlockResponse(
    val type: String,
    val text: TextElementResponse? = null,
    val elements: List<BlockElementResponse> = listOf()
)

@Serializable
private data class TextElementResponse(
    val text: String,
    val type: String,
    val emoji: Boolean = false
)

@Serializable
private data class BlockElementResponse(
    val type: String,
    val text: TextElementResponse,
    val url: String? = null,
    val style: String = "primary",
)

@Serializable
private data class ChatPostMessageRequest(
    val channel: String,
    val blocks: List<BlockResponse>? = null,
    val text: String? = null,
    val thread_ts: String? = null
)

@Serializable
private data class ChatPostMessageResponse(
    val ok: Boolean,
    val ts: String? = null,
    val channel: String? = null
)


private val logger = KotlinLogging.logger { }

class SlackReporter(handler: CrashHandler) : Reporter(handler) {

    var channelId: String = ""
    var authToken: String = ""

    private val monitorJson = Json {
        ignoreUnknownKeys = true
    }

    private fun makeRequest(client: OkHttpClient, messageRequest: ChatPostMessageRequest): Response {
        val body = monitorJson.encodeToString(messageRequest)
        val requestBody = body.toRequestBody("application/json".toMediaType())

        val replySlackRequest: Request = Request.Builder()
            .url("https://slack.com/api/chat.postMessage")
            .method("POST", requestBody)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $authToken")
            .build()

        val response = client.newCall(replySlackRequest).execute()
        require(response.isSuccessful) {
            "request failed: ${response.code} ${response.message}"
        }

        return response
    }

    private fun plainText(text: String): TextElementResponse {
        return TextElementResponse(text, "plain_text", false)
    }

    private fun slackMessage(client: OkHttpClient, endpoint: String, error: Boolean = false, errorLog: String? = null) {

        val messageRequest = if (error) {
            ChatPostMessageRequest(channel = channelId!!, thread_ts = null,
                blocks = listOf(
                    BlockResponse("section", plainText("There is a problem with $endpoint. Please check.")),
                    BlockResponse("actions", elements = listOfNotNull(

                        if (handler.vncHost != null) {
                            BlockElementResponse(
                                type = "button",
                                text = plainText("VNC into $endpoint"),
                                url = "vnc://${handler.vncHost}"
                            )
                        } else { null }
                    )
                    )
                )
            )
        } else {
            ChatPostMessageRequest(channel = channelId!!, thread_ts = null,
                blocks = listOf(BlockResponse("section", plainText("$endpoint is back online!")))
            )
        }

        val response = makeRequest(client, messageRequest)


        if (error && response.isSuccessful) {
            val cmr = monitorJson.decodeFromString<ChatPostMessageResponse>(response.body?.string() ?: "")

            val logMessage = errorLog ?: "No log could be retrieved. Machine is likely unreachable"

            val replyRequest =  ChatPostMessageRequest(channel = channelId, thread_ts = cmr.ts,
                blocks = listOf(
                    BlockResponse(
                        type = "section",
                        text = TextElementResponse("```${logMessage}```", "mrkdwn", false)
                    )
                ),
            )

            makeRequest(client, replyRequest)
        }
    }


    override fun reportCrash(throwable: Throwable) {
        logger.info { "reporting " }
        val client = OkHttpClient().newBuilder().build()
        slackMessage(client, handler.name ?: "no name", true, throwable.stackTraceToString())
    }
}

fun CrashHandler.slack(config: SlackReporter.() -> Unit) {
    reporters.add(SlackReporter(this).apply(config))
}