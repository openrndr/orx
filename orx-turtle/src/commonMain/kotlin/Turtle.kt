package org.openrndr.extra.turtle

import org.openrndr.math.*
import org.openrndr.math.transforms.rotateZ
import org.openrndr.shape.*

class Turtle(initialPosition: Vector2) {
    val cb = ContourBuilder(multipleContours = true).apply {
        moveTo(initialPosition)
    }

    var orientation = Matrix44.fromColumnVectors(Vector4.UNIT_X, -Vector4.UNIT_Y, Vector4.UNIT_Z, Vector4.UNIT_W)
    private val orientationStack = ArrayDeque<Matrix44>()
    fun pushOrientation() {
        orientationStack.addLast(orientation)
    }

    fun popOrientation() {
        orientation = orientationStack.removeLastOrNull() ?: error("orientation stack underflow")
    }

    var position: Vector2
        get() {
            return cb.cursor
        }
        set(value) {
            cb.moveTo(value)
        }

    private val positionStack = ArrayDeque<Vector2>()
    fun pushPosition() {
        positionStack.addLast(position)
    }

    fun popPosition() {
        position = positionStack.removeLastOrNull() ?: error("position stack underflow")
    }

    fun close() {
        cb.close()
    }

    fun resetOrientation() {
        orientation = Matrix44.fromColumnVectors(Vector4.UNIT_X, -Vector4.UNIT_Y, Vector4.UNIT_Z, Vector4.UNIT_W)
    }

    var direction: Vector2
        get() = (orientation * Vector4.UNIT_X).xy
        set(value) {
            val directionNormalized = value.normalized
            orientation = Matrix44.fromColumnVectors(
                directionNormalized.xy00,
                directionNormalized.perpendicular().xy00,
                Vector4.UNIT_Z,
                Vector4.UNIT_W
            )
        }

    var isPenDown = true

    fun penUp() {
        isPenDown = false
    }

    fun penDown() {
        isPenDown = true
    }

    fun push() {
        pushOrientation()
        pushPosition()
    }

    fun pop() {
        popPosition()
        popOrientation()
    }

    fun rotate(degrees: Double) {
        orientation *= Matrix44.rotateZ(degrees)
    }

    fun forward(distance: Double) {
        if (distance >= 1E-6) {
            if (isPenDown) {
                cb.lineTo(position + direction * distance)
            } else {
                cb.moveTo(position + direction * distance)
            }
        }
    }
}

fun turtle(initalPosition: Vector2, program: Turtle.() -> Unit): List<ShapeContour> {
    return Turtle(initalPosition).apply(program).cb.result
}
