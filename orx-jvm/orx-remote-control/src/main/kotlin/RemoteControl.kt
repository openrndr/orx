package org.openrndr.extra.remotecontrol

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CompletableDeferred
import org.openrndr.Extension
import org.openrndr.KeyEvent
import org.openrndr.KeyEventType
import org.openrndr.KeyModifier
import org.openrndr.MouseButton
import org.openrndr.MouseEvent
import org.openrndr.MouseEventType
import org.openrndr.Program
import org.openrndr.draw.BufferMultisample
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Drawer
import org.openrndr.draw.ImageFileFormat
import org.openrndr.draw.MagnifyingFilter
import org.openrndr.draw.RenderTarget
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.renderTarget
import org.openrndr.internal.KeyboardDriver
import org.openrndr.math.Vector2
import java.util.Base64
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Exposes a [program] over a local HTTP server: a screenshot of the current frame can be
 * requested, and keyboard/mouse events can be injected as if a real user produced them --
 * intended for driving an OPENRNDR program from an external tool (a test suite, an agent)
 * that can't attach to the window directly.
 *
 * All routes are served on [network]:[port] (default `127.0.0.1:9000`, i.e. not reachable
 * from outside the machine by default -- change [network] deliberately if that's not what
 * you want).
 *
 * ### Routes
 * - `GET /screenshot` -- waits for the next frame and returns it as a PNG.
 * - `POST /keyboard/down?name=<key>`, `POST /keyboard/up?name=<key>`,
 *   `POST /keyboard/repeat?name=<key>` -- inject a key event. `name` is the layout-sensitive
 *   key name as used by [org.openrndr.KeyEvent.name] (e.g. `a`, `spacebar`, `arrow-left`).
 *   An optional `modifiers` query parameter takes a comma-separated list of `SHIFT`, `CTRL`,
 *   `ALT`, `SUPER`.
 * - `POST /mouse/move?x=<x>&y=<y>` -- move the mouse to an absolute window position.
 * - `POST /mouse/button/down?x=<x>&y=<y>&button=<LEFT|RIGHT|CENTER>`,
 *   `POST /mouse/button/up?...` -- press/release a mouse button, optionally moving to `x`/`y`
 *   first (omit `x`/`y` to use the current position).
 * - `POST /mouse/scroll?dx=<dx>&dy=<dy>` -- inject a scroll event.
 * - `POST /clock?time=<seconds>` -- freeze [Program.seconds] at a fixed value, e.g. for
 *   deterministic, reproducible screenshots of an animation at a specific point in time.
 *   `POST /clock/reset` restores the real (or otherwise previously active) clock.
 *
 * All events are delivered through the same (thread-safe, postponed) event queue real input
 * uses, applied on the program's own thread right before the next frame is drawn -- so this
 * is safe to call from any thread, including ktor's own request-handling threads.
 */
class RemoteControl : Extension {
    override var enabled: Boolean = true

    /** the network interface to bind to; the default only accepts connections from localhost */
    var network: String = "127.0.0.1"
    var port: Int = 9000


    /**
     * `true` while [Program.clock] is overridden to a fixed value requested through
     * `POST /clock`; reading this reflects the extension's current state, setting it has no
     * effect (use the `/clock` and `/clock/reset` routes instead).
     */
    var useStaticClock = false
        private set

    /** the clock as it was before `/clock` first overrode it, restored by `/clock/reset` */
    private var originalClock: (() -> Double)? = null

    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null

    /** pending keyboard/mouse actions, applied (on the program's thread) in [beforeDraw] */
    private val actionQueue = ConcurrentLinkedQueue<() -> Unit>()

    /** pending screenshot requests; one is serviced per frame */
    private val screenshotQueue = ConcurrentLinkedQueue<CompletableDeferred<ByteArray>>()

    private var activeScreenshot: CompletableDeferred<ByteArray>? = null
    private var screenshotTarget: RenderTarget? = null
    private var screenshotResolved: ColorBuffer? = null

    override fun setup(program: Program) {
        server = embeddedServer(Netty, port = port, host = network) {
            routing {
                get("/screenshot") {
                    val result = CompletableDeferred<ByteArray>()
                    screenshotQueue.add(result)
                    program.window.requestDraw()
                    call.respondBytes(result.await(), ContentType.Image.PNG)
                }

                post("/keyboard/{action}") {
                    val name = call.request.queryParameters["name"]
                    if (name == null) {
                        call.respond(HttpStatusCode.BadRequest, "missing 'name' query parameter")
                        return@post
                    }
                    val modifiers = parseModifiers(call.request.queryParameters["modifiers"])
                    val keyId = keyIdOf(name)
                    val type = when (call.parameters["action"]) {
                        "down" -> KeyEventType.KEY_DOWN
                        "up" -> KeyEventType.KEY_UP
                        "repeat" -> KeyEventType.KEY_REPEAT
                        else -> {
                            call.respond(HttpStatusCode.BadRequest, "unknown action, expected down/up/repeat")
                            return@post
                        }
                    }
                    actionQueue.add {
                        val event = KeyEvent(type, keyId, name, modifiers)
                        when (type) {
                            KeyEventType.KEY_DOWN -> program.keyboard.keyDown.trigger(event)
                            KeyEventType.KEY_UP -> program.keyboard.keyUp.trigger(event)
                            KeyEventType.KEY_REPEAT -> program.keyboard.keyRepeat.trigger(event)
                        }
                    }
                    program.window.requestDraw()
                    call.respond(HttpStatusCode.OK)
                }

                post("/mouse/move") {
                    val position = call.request.queryParameters.vector2("x", "y")
                    if (position == null) {
                        call.respond(HttpStatusCode.BadRequest, "missing 'x'/'y' query parameters")
                        return@post
                    }
                    actionQueue.add {
                        val previous = program.mouse.position
                        // Program.mouse is typed as the read-only MouseEvents interface, so the
                        // actual position has to be set through Application instead.
                        program.application.cursorPosition = position
                        program.mouse.moved.trigger(
                            MouseEvent(position, Vector2.ZERO, position - previous, MouseEventType.MOVED, MouseButton.NONE, emptySet())
                        )
                    }
                    program.window.requestDraw()
                    call.respond(HttpStatusCode.OK)
                }

                post("/mouse/button/{action}") {
                    val type = when (call.parameters["action"]) {
                        "down" -> MouseEventType.BUTTON_DOWN
                        "up" -> MouseEventType.BUTTON_UP
                        else -> {
                            call.respond(HttpStatusCode.BadRequest, "unknown action, expected down/up")
                            return@post
                        }
                    }
                    val buttonName = call.request.queryParameters["button"] ?: "LEFT"
                    val button = try {
                        MouseButton.valueOf(buttonName.uppercase())
                    } catch (e: IllegalArgumentException) {
                        call.respond(HttpStatusCode.BadRequest, "unknown button, expected LEFT/RIGHT/CENTER")
                        return@post
                    }
                    val requestedPosition = call.request.queryParameters.vector2("x", "y")
                    val modifiers = parseModifiers(call.request.queryParameters["modifiers"])
                    actionQueue.add {
                        if (requestedPosition != null) {
                            program.application.cursorPosition = requestedPosition
                        }
                        val event = MouseEvent(program.mouse.position, Vector2.ZERO, Vector2.ZERO, type, button, modifiers)
                        when (type) {
                            MouseEventType.BUTTON_DOWN -> program.mouse.buttonDown.trigger(event)
                            MouseEventType.BUTTON_UP -> program.mouse.buttonUp.trigger(event)
                            else -> Unit
                        }
                    }
                    program.window.requestDraw()
                    call.respond(HttpStatusCode.OK)
                }

                post("/mouse/scroll") {
                    val dx = call.request.queryParameters["dx"]?.toDoubleOrNull() ?: 0.0
                    val dy = call.request.queryParameters["dy"]?.toDoubleOrNull() ?: 0.0
                    actionQueue.add {
                        program.mouse.scrolled.trigger(
                            MouseEvent(program.mouse.position, Vector2(dx, dy), Vector2.ZERO, MouseEventType.SCROLLED, MouseButton.NONE, emptySet())
                        )
                    }
                    program.window.requestDraw()
                    call.respond(HttpStatusCode.OK)
                }

                post("/clock") {
                    val time = call.request.queryParameters["time"]?.toDoubleOrNull()
                    if (time == null) {
                        call.respond(HttpStatusCode.BadRequest, "missing 'time' query parameter")
                        return@post
                    }
                    actionQueue.add {
                        // Only remember the clock that was active before we started overriding
                        // it, so repeated /clock calls (or a /clock after one that's already
                        // fixed) don't end up "restoring" a previously-fixed time instead of the
                        // real one.
                        if (originalClock == null) {
                            originalClock = program.clock
                        }
                        program.clock = { time }
                        // drawImpl() calls this itself once per frame using whichever clock is
                        // current at the time, so overriding program.clock alone is enough to
                        // keep it fixed going forward -- this call just makes the new time take
                        // effect immediately, for the frame currently being processed.
                        program.updateFrameSecondsFromClock()
                        useStaticClock = true
                    }
                    program.window.requestDraw()
                    call.respond(HttpStatusCode.OK)
                }

                post("/clock/reset") {
                    actionQueue.add {
                        originalClock?.let {
                            program.clock = it
                            program.updateFrameSecondsFromClock()
                        }
                        originalClock = null
                        useStaticClock = false
                    }
                    program.window.requestDraw()
                    call.respond(HttpStatusCode.OK)
                }
            }
        }.also { it.start(wait = false) }
    }

    override fun beforeDraw(drawer: Drawer, program: Program) {
        while (true) {
            val action = actionQueue.poll() ?: break
            action()
        }

        if (activeScreenshot == null) {
            activeScreenshot = screenshotQueue.poll()
            activeScreenshot?.let {
                val width = RenderTarget.active.width
                val height = RenderTarget.active.height
                val contentScale = program.window.contentScale
                val multisample = program.window.multisample.bufferEquivalent()

                screenshotTarget = renderTarget(width, height, contentScale = contentScale, multisample = multisample) {
                    colorBuffer()
                    depthBuffer()
                }
                screenshotResolved = when (multisample) {
                    BufferMultisample.Disabled -> null
                    is BufferMultisample.SampleCount -> colorBuffer(width, height, contentScale = contentScale)
                }
                screenshotTarget?.bind()
            }
        }
    }

    override fun afterDraw(drawer: Drawer, program: Program) {
        val result = activeScreenshot ?: return
        val target = screenshotTarget ?: return

        drawer.shadeStyle = null
        target.unbind()
        drawer.defaults()

        val resolved = screenshotResolved
        val captured = if (resolved == null) {
            target.colorBuffer(0)
        } else {
            target.colorBuffer(0).copyTo(resolved, 0, 0, MagnifyingFilter.NEAREST)
            resolved
        }
        // put the captured frame back on screen so requesting a screenshot doesn't blank it
        drawer.image(captured, captured.bounds, drawer.bounds)

        val dataUrl = captured.toDataUrl(ImageFileFormat.PNG)
        val bytes = Base64.getDecoder().decode(dataUrl.substring(dataUrl.indexOf(',') + 1))
        result.complete(bytes)

        target.destroy()
        resolved?.destroy()
        activeScreenshot = null
        screenshotTarget = null
        screenshotResolved = null
    }

    override fun shutdown(program: Program) {
        server?.stop(gracePeriodMillis = 0, timeoutMillis = 200)
        server = null
    }

    private fun keyIdOf(name: String): Int = try {
        KeyboardDriver.instance.getKeyId(name)
    } catch (e: Exception) {
        0
    }
}

private fun parseModifiers(value: String?): Set<KeyModifier> =
    value?.split(',')
        ?.mapNotNull { part -> part.trim().takeIf { it.isNotEmpty() } }
        ?.mapNotNull { name -> runCatching { KeyModifier.valueOf(name.uppercase()) }.getOrNull() }
        ?.toSet()
        ?: emptySet()

private fun io.ktor.http.Parameters.vector2(xKey: String, yKey: String): Vector2? {
    val x = this[xKey]?.toDoubleOrNull()
    val y = this[yKey]?.toDoubleOrNull()
    return if (x != null && y != null) Vector2(x, y) else null
}
