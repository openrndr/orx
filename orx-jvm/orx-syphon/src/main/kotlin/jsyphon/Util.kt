package org.openrndr.extra.syphon.jsyphon
class NSSize (var x: Int, var y: Int)
class NSPoint(var x: Int, var y: Int)

class NSRect(var origin: NSPoint, var size: NSSize) {
    constructor(startX: Int, xLength: Int, startY: Int, yLength: Int) : this(
            NSPoint(startX, startY),
            NSSize(xLength, yLength)
    )
}