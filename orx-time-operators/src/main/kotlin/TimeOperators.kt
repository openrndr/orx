package org.openrndr.extra.timeoperators

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.draw.Drawer

interface TimeTools {
    fun tick(seconds: Double, deltaTime: Double, frameCount: Int)
}

class TimeOperators : Extension {
    override var enabled: Boolean = true

    private val operators = mutableSetOf<TimeTools>()

    fun track(vararg tools: TimeTools) {
        operators.addAll(tools)
    }

    override fun beforeDraw(drawer: Drawer, program: Program) {
        operators.forEach { it.tick(program.seconds, program.deltaTime, program.frameCount) }
    }
}
