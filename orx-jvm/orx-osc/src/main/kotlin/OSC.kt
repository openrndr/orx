package org.openrndr.extra.osc

import com.illposed.osc.OSCMessage
import com.illposed.osc.OSCMessageListener
import com.illposed.osc.messageselector.OSCPatternAddressMessageSelector
import com.illposed.osc.transport.OSCPort
import com.illposed.osc.transport.OSCPortIn
import com.illposed.osc.transport.OSCPortOut
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.InetAddress
import java.net.PortUnreachableException
import kotlin.reflect.KMutableProperty0

private typealias OSCListener = Pair<OSCPatternAddressMessageSelector, OSCMessageListener>

private val logger = KotlinLogging.logger {}

@Suppress("unused")
class OSC (
    val address: InetAddress = InetAddress.getLocalHost(),
    val portIn: Int = OSCPort.DEFAULT_SC_OSC_PORT,
    val portOut: Int = portIn
) {
    private val receiver: OSCPortIn = OSCPortIn(portIn)
    private val sender: OSCPortOut = OSCPortOut(address, portOut)
    private val listeners: MutableMap<String, OSCListener> = mutableMapOf()

    fun <T> send(channel: String, vararg message: T) {
        if (!sender.isConnected) sender.connect()

        val msg = OSCMessage(channel, message.toList())

        try {
            sender.send(msg)
        } catch (ex: PortUnreachableException) {
            logger.error(ex) { "Error: Could not connect to OUT port" }
        } catch (ex: IllegalStateException) {
            logger.error(ex) { "Error: Couldn't send message to channel: $channel" }
        }
    }

    fun listen(channel: String, callback: (String, List<Any>) -> Unit) {
        val selector = OSCPatternAddressMessageSelector(channel);

        val cb = OSCMessageListener {
            callback(it.message.address, it.message.arguments)
        }

        receiver.dispatcher.addListener(selector, cb)

        listeners[channel] = Pair(selector, cb)

        if (!receiver.isListening) this.startListening()
    }

    infix fun String.bind(prop: KMutableProperty0<Double>) {
        val channel = this

        listen(channel) { address, it ->
            when (val message = it.first()) {
                is Double -> prop.set(message)
                is Float -> prop.set(message.toDouble())
            }
        }
    }

    // Cannot be called inside a listener's callback
    fun removeListener(channel: String?) {
        val listener = listeners[channel]

        if (listener != null) {
            receiver.dispatcher.removeListener(listener.first, listener.second)
        }
    }

    private fun startListening() {
        receiver.dispatcher.isAlwaysDispatchingImmediately = true;

        receiver.startListening()

        if (receiver.isListening) logger.info("OSC is listening on port: $portIn")
    }
}
