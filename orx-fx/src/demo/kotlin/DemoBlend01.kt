package org.openrndr.extra.fx.demo

import org.openrndr.application
import org.openrndr.extra.fx.blend.*
fun main() {
    application {
        program {
            val add = Add()
            val colorBurn = ColorBurn()
            val colorDodge = ColorDodge()
            val darken = Darken()
            val destIn = DestinationIn()
            val destOut = DestinationOut()
            val destAtop = DestinationAtop()
            val hardLight = HardLight()
            val lighten = Lighten()
            val multiply = Multiply()
            val multiplyContrast = MultiplyContrast()
            val normal = Normal()
            val overlay = Overlay()
            val passthrough = Passthrough()
            val screen = Screen()
            val sourceIn = SourceIn()
            val sourceAtop = SourceAtop()
            val sourceOut = SourceOut()
            val subtract = Subtract()
            val xor = Xor()
            application.exit()
        }
    }
}