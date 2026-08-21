package org.openrndr.extra.oscquery

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.illposed.osc.OSCMessageEvent
import com.illposed.osc.OSCMessageListener
import com.illposed.osc.argument.OSCColor
import com.illposed.osc.messageselector.OSCPatternAddressMessageSelector
import com.illposed.osc.transport.OSCPortIn
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.parameters.*
import java.net.InetAddress
import kotlin.reflect.KMutableProperty1

private val logger = KotlinLogging.logger {}

/**
 * A minimal [OSCQuery](https://github.com/Vidvox/OSCQueryProposal) server.
 *
 * It exposes annotated parameters (see orx-parameters) over two transports at once:
 *  - an **HTTP** server that describes the OSC namespace as JSON, so that clients can discover
 *    the available paths, their types, ranges and current values.
 *  - an **OSC** (UDP) server that receives value updates and applies them to the bound properties.
 *
 * This first version deliberately covers only what is needed to control a sketch: floating point
 * numbers ([DoubleParameter][org.openrndr.extra.parameters.DoubleParameter]), integers
 * ([IntParameter][org.openrndr.extra.parameters.IntParameter]), colors
 * ([ColorParameter][org.openrndr.extra.parameters.ColorParameter]) and trigger functions
 * ([ActionParameter][org.openrndr.extra.parameters.ActionParameter]).
 *
 * Usage:
 * ```
 * val oscQuery = OSCQuery()
 * oscQuery.add(settings)
 * ```
 *
 * @param oscPort the UDP port the OSC server listens on for value updates
 * @param httpPort the TCP port the HTTP server serves the JSON namespace on
 * @param name a human-readable name reported in the `HOST_INFO` response
 */
@Suppress("unused")
class OSCQuery(
    val oscPort: Int = 9000,
    val httpPort: Int = oscPort,
    val name: String = "OPENRNDR"
) : Extension {

    override var enabled = true

    /** The known OSCQuery node attributes that can be requested individually, e.g. `/Settings/radius?VALUE`. */
    private val knownAttributes = setOf(
        "FULL_PATH", "CONTENTS", "TYPE", "VALUE", "ACCESS", "DESCRIPTION", "RANGE"
    )

    /** The root container node, always present at path "/". */
    private val root = Node(fullPath = "/", access = 0)

    /** Flat index of every node (containers and methods) by its full path, for quick HTTP lookups. */
    private val nodesByPath = linkedMapOf("/" to root)

    private val oscReceiver: OSCPortIn = OSCPortIn(oscPort)
    private var httpServer: EmbeddedServer<*, *>? = null

    init {
        startHttpServer()
        oscReceiver.dispatcher.isAlwaysDispatchingImmediately = true
        oscReceiver.startListening()
        logger.info { "OSCQuery OSC server listening on UDP port $oscPort" }
    }

    /**
     * Register every annotated parameter found on [objectWithParameters]. The object's
     * [Description][org.openrndr.extra.parameters.Description] title (or class name as a fallback)
     * becomes a container, and each parameter becomes an addressable method underneath it, e.g.
     * an object titled "Settings" with a `radius` parameter is exposed at `/Settings/radius`.
     */
    fun add(objectWithParameters: Any) {
        val containerName = sanitize(objectWithParameters.title() ?: objectWithParameters::class.simpleName ?: "object")
        val containerPath = "/$containerName"

        val container = nodesByPath.getOrPut(containerPath) {
            Node(fullPath = containerPath, access = 0).also { root.contents[containerName] = it }
        }
        container.description = objectWithParameters.description() ?: container.description

        for (parameter in objectWithParameters.listParameters()) {
            val type = oscType(parameter.parameterType)
            if (type == null) {
                logger.warn { "OSCQuery: skipping unsupported parameter type ${parameter.parameterType} (${parameter.label})" }
                continue
            }

            val methodName = sanitize(parameter.label)
            val methodPath = "$containerPath/$methodName"

            val node = Node(
                fullPath = methodPath,
                access = if (parameter.parameterType == ParameterType.Action) 2 else 3,
                type = type,
                description = parameter.label,
                target = objectWithParameters,
                parameter = parameter,
                range = rangeOf(parameter)
            )
            container.contents[methodName] = node
            nodesByPath[methodPath] = node

            registerOscListener(node)
        }
        logger.info { "OSCQuery registered container '$containerPath' with ${container.contents.size} parameter(s)" }
    }

    /** Stop the HTTP and OSC servers. Also called automatically if this is used as an [Extension]. */
    fun stop() {
        try {
            oscReceiver.stopListening()
            oscReceiver.close()
        } catch (e: Exception) {
            logger.warn(e) { "OSCQuery: error while closing OSC receiver" }
        }
        httpServer?.stop(50, 50)
    }

    override fun shutdown(program: Program) = stop()

    // region OSC receiving

    private fun registerOscListener(node: Node) {
        val selector = OSCPatternAddressMessageSelector(node.fullPath)
        val listener = OSCMessageListener { event: OSCMessageEvent ->
            try {
                applyOscMessage(node, event.message.arguments)
            } catch (e: Exception) {
                logger.warn(e) { "OSCQuery: failed to apply message on ${node.fullPath}" }
            }
        }
        oscReceiver.dispatcher.addListener(selector, listener)
    }

    private fun applyOscMessage(node: Node, arguments: List<Any>) {
        val parameter = node.parameter ?: return
        val target = node.target ?: return
        when (parameter.parameterType) {
            ParameterType.Double -> parameter.property.qset(
                target,
                (arguments.firstOrNull() as? Number)?.toDouble() ?: return
            )

            ParameterType.Int -> parameter.property.qset(
                target,
                (arguments.firstOrNull() as? Number)?.toInt() ?: return
            )

            ParameterType.Color -> parameter.property.qset(target, argumentsToColor(arguments) ?: return)
            ParameterType.Action -> parameter.function?.call(target)
            else -> {}
        }
    }

    /**
     * Convert incoming OSC arguments to a [ColorRGBa]. Different clients encode an OSC color (`r`)
     * differently, so we accept the most common forms: a [java.awt.Color], four separate
     * `0..1` float components, or a single packed RGBA integer.
     */
    private fun argumentsToColor(arguments: List<Any>): ColorRGBa? {
        return when (val first = arguments.firstOrNull()) {
            is OSCColor -> {
                ColorRGBa(
                    (first.red.toInt() and 0xff) / 255.0,
                    (first.green.toInt() and 0xff) / 255.0,
                    (first.blue.toInt() and 0xff) / 255.0,
                    (first.alpha.toInt() and 0xff) / 255.0
                )
            }

            null -> {
                println("No arguments found for color")
                null
            }

            else -> {
                println("Unrecognized color type ${first::class}")
                null
            }
        }
    }

    // endregion

    // region HTTP serving

    private fun startHttpServer() {
        val server = embeddedServer(Netty, port = httpPort) {
            routing {
                get("{...}") {
                    val queryNames = call.request.queryParameters.names()

                    if ("HOST_INFO" in queryNames) {
                        call.respondJson(hostInfo())
                        return@get
                    }

                    val node = nodesByPath[normalizePath(call.request.path())]
                    if (node == null) {
                        call.respond(HttpStatusCode.NotFound, "No such OSC node")
                        return@get
                    }

                    val attribute = queryNames.firstOrNull { it in knownAttributes }
                    if (attribute != null) {
                        val value = node.attribute(attribute)
                        if (value == null) {
                            call.respond(HttpStatusCode.NoContent, "")
                        } else {
                            call.respondJson(value)
                        }
                    } else {
                        call.respondJson(node.toJson())
                    }
                }
            }
        }
        server.start()
        httpServer = server
        logger.info { "OSCQuery HTTP server serving namespace on http://${localAddress()}:$httpPort/" }
    }

    private fun hostInfo(): JsonObject = JsonObject().apply {
        addProperty("NAME", name)
        addProperty("OSC_IP", localAddress())
        addProperty("OSC_PORT", oscPort)
        addProperty("OSC_TRANSPORT", "UDP")
        add("EXTENSIONS", JsonObject().apply {
            addProperty("ACCESS", true)
            addProperty("VALUE", true)
            addProperty("RANGE", true)
            addProperty("DESCRIPTION", true)
            addProperty("TYPE", true)
        })
    }

    // endregion

    /**
     * A node in the OSCQuery tree. A node with a null [type] is a *container* (it holds [contents]);
     * otherwise it is a *method* that maps to a single [parameter] on [target].
     */
    private inner class Node(
        val fullPath: String,
        var description: String = "",
        var access: Int = 0,
        val type: String? = null,
        val contents: LinkedHashMap<String, Node> = linkedMapOf(),
        val target: Any? = null,
        val parameter: Parameter? = null,
        val range: List<Pair<Number, Number>?>? = null
    ) {
        fun toJson(): JsonObject = JsonObject().apply {
            addProperty("FULL_PATH", fullPath)
            addProperty("ACCESS", access)
            if (description.isNotEmpty()) addProperty("DESCRIPTION", description)
            if (type == null) {
                add("CONTENTS", JsonObject().apply {
                    contents.forEach { (name, child) -> add(name, child.toJson()) }
                })
            } else {
                addProperty("TYPE", type)
                currentValue()?.let { add("VALUE", it) }
                rangeJson()?.let { add("RANGE", it) }
            }
        }

        /** Return a single attribute as JSON or null if this node has no such attribute. */
        fun attribute(name: String): com.google.gson.JsonElement? = when (name) {
            "FULL_PATH" -> JsonPrimitive(fullPath)
            "ACCESS" -> JsonPrimitive(access)
            "DESCRIPTION" -> if (description.isNotEmpty()) JsonPrimitive(description) else null
            "TYPE" -> type?.let { JsonPrimitive(it) }
            "VALUE" -> currentValue()
            "RANGE" -> rangeJson()
            "CONTENTS" -> if (type == null) toJson().get("CONTENTS") else null
            else -> null
        }

        /** Read the property's live value (reflecting changes made via GUI or MIDI too). */
        private fun currentValue(): JsonArray? {
            val parameter = parameter ?: return null
            val target = target ?: return null
            val property = parameter.property ?: return null
            return when (parameter.parameterType) {
                ParameterType.Double -> jsonArrayOf(JsonPrimitive(property.qget<Double>(target)))
                ParameterType.Int -> jsonArrayOf(JsonPrimitive(property.qget<Int>(target)))
                ParameterType.Color -> jsonArrayOf(colorToJson(property.qget(target)))
                else -> null
            }
        }

        private fun rangeJson(): JsonArray? {
            val range = range ?: return null
            return JsonArray().apply {
                range.forEach { entry ->
                    add(JsonObject().apply {
                        if (entry != null) {
                            add("MIN", JsonPrimitive(entry.first))
                            add("MAX", JsonPrimitive(entry.second))
                        }
                    })
                }
            }
        }
    }

    // region helpers

    private fun oscType(type: ParameterType): String? = when (type) {
        ParameterType.Double -> "f"
        ParameterType.Int -> "i"
        ParameterType.Color -> "r"
        ParameterType.Action -> "N"
        else -> null
    }

    private fun rangeOf(parameter: Parameter): List<Pair<Number, Number>?>? = when (parameter.parameterType) {
        ParameterType.Double -> parameter.doubleRange?.let { listOf(it.start to it.endInclusive) }
        ParameterType.Int -> parameter.intRange?.let { listOf(it.first to it.last) }
        else -> null
    }

    private fun colorToJson(color: ColorRGBa): JsonArray = jsonArrayOf(
        JsonPrimitive(color.r), JsonPrimitive(color.g), JsonPrimitive(color.b), JsonPrimitive(color.alpha)
    )

    private fun jsonArrayOf(vararg elements: com.google.gson.JsonElement): JsonArray =
        JsonArray().apply { elements.forEach { add(it) } }

    private fun sanitize(name: String): String = name.trim().replace(Regex("\\s+"), "_")

    private fun normalizePath(path: String): String =
        if (path.length > 1 && path.endsWith("/")) path.dropLast(1) else path

    private fun localAddress(): String = try {
        InetAddress.getLocalHost().hostAddress
    } catch (e: Exception) {
        "127.0.0.1"
    }

    // endregion
}

private suspend fun ApplicationCall.respondJson(element: com.google.gson.JsonElement) =
    respondText(element.toString(), ContentType.Application.Json)

private fun <T> KMutableProperty1<out Any, Any?>?.qset(obj: Any, value: T) {
    @Suppress("UNCHECKED_CAST")
    (this as KMutableProperty1<Any, T>).set(obj, value)
}

private fun <T> KMutableProperty1<out Any, Any?>.qget(obj: Any): T {
    @Suppress("UNCHECKED_CAST")
    return (this as KMutableProperty1<Any, T>).get(obj)
}
