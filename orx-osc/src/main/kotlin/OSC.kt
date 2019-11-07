package org.openrndr.extra.osc

import com.illposed.osc.OSCMessage
import com.illposed.osc.OSCMessageListener
import com.illposed.osc.messageselector.OSCPatternAddressMessageSelector
import com.illposed.osc.transport.udp.OSCPort
import com.illposed.osc.transport.udp.OSCPortIn
import com.illposed.osc.transport.udp.OSCPortOut
import java.net.InetAddress
import java.net.PortUnreachableException

private typealias OSCListener = Pair<OSCPatternAddressMessageSelector, OSCMessageListener>

class OSC (
        address: InetAddress = InetAddress.getLocalHost(),
        portIn: Int = OSCPort.DEFAULT_SC_OSC_PORT,
        portOut: Int = portIn
) {
    private val receiver: OSCPortIn = OSCPortIn(portIn)
    private val sender: OSCPortOut = OSCPortOut(address, portOut)
    private val listeners: MutableMap<String, OSCListener> = mutableMapOf()

    fun <T> send(channel: String, vararg message: T) {
        if (!sender.isConnected) sender.connect()

        val msg = OSCMessage(channel, message.toList())

        try {
            sender.send(msg)
        } catch (ex: Exception) {
            when(ex) {
                is PortUnreachableException -> System.err.println("Error: Could not connect to OUT port")
                is IllegalStateException -> System.err.println("Error: Couldn't send message to channel: $channel")
            }
            // ex.printStackTrace() - Would rather keep this behind a debug flag
        }
    }

    fun listen(channel: String, callback: (List<Any>) -> Unit) {
        val selector = OSCPatternAddressMessageSelector(channel);

        val cb = OSCMessageListener {
            callback(it.message.arguments)
        }

        receiver.dispatcher.addListener(selector, cb)

        listeners[channel] = Pair(selector, cb)

        if (!receiver.isListening) this.startListening()
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

        if (receiver.isListening) println("OSC is listening..")
    }
}
