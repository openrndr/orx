
package org.openrndr.extra.fluidsim


import org.openrndr.color.ColorRGBa
import org.openrndr.draw.BufferMultisample
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.Drawer
import org.openrndr.draw.RenderTarget
import org.openrndr.draw.RenderTargetBuilder
import org.openrndr.draw.Session
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.renderTarget


class PingPongBuffer(
        width: Int,
        height: Int,
        contentScale: Double = 1.0,
        format: ColorFormat = ColorFormat.RGBa,
        type: ColorType = ColorType.UINT8,
        multisample: BufferMultisample = BufferMultisample.Disabled,
        levels: Int = 1,
        session: Session? = Session.active
) {
    private var buffer1 = colorBuffer(width, height, contentScale, format, type, multisample, levels, session)
    private var buffer2 = colorBuffer(width, height, contentScale, format, type, multisample, levels, session)

    private var curr = buffer2
    private var prev = buffer1

    fun swap() {
        val temp = curr
        curr = prev
        prev = temp
    }

    fun fill(color: ColorRGBa) {
        curr.fill(color)
        prev.fill(color)
    }

    fun src() = prev

    fun dst() = curr
}
