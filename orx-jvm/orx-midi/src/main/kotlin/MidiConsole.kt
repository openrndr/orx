package org.openrndr.extra.midi

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.loadFont
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import java.io.File

class MidiConsole: Extension {
    override var enabled = true

    var box = Rectangle(0.0, 0.0, 130.0, 200.0)
    val messages = mutableListOf<String>()
    var historySize = 2

    val demoFont = File("demo-data/fonts/IBMPlexMono-Regular.ttf").exists()

    fun register(transceiver: MidiTransceiver) {
        transceiver.controlChanged.listen {
            synchronized(messages) {
                messages.add("CC ${it.control}: ${it.value}")
                if (messages.size > historySize) {
                    messages.removeAt(0)
                }
            }
        }
        transceiver.noteOn.listen {
            synchronized(messages) {
                messages.add("NOTE ON ${it.note}: ${it.velocity}")
                if (messages.size > historySize) {
                    messages.removeAt(0)
                }
            }
        }

        transceiver.noteOff.listen {
            synchronized(messages) {
                messages.add("NOTE OFF ${it.note}")
                if (messages.size > historySize) {
                    messages.removeAt(0)
                }
            }
        }
    }

    override fun afterDraw(drawer: Drawer, program: Program)  {
        drawer.defaults()
        synchronized(messages) {
            box = Rectangle(drawer.width - box.width, 0.0, box.width, drawer.height * 1.0)
            val positions = List(messages.size) { index ->
                Vector2(box.x, box.y + index * 16.0 + 16.0)
            }
            if (demoFont) {
                drawer.fontMap = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 16.0)
            }
            drawer.fill = ColorRGBa.WHITE
            drawer.texts(messages, positions)
        }
    }
}