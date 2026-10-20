package org.openrndr.extra.turtle

import org.openrndr.math.Vector2
import org.openrndr.math.Vector4
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.tan

/**
 * Moves the turtle [length] forward while it turns as sharply as [curvature] prescribes: curvature(s) is the turning
 * rate, in radians per unit of length, after a distance s from 0 to [length]. Positive curvature turns the same way as
 * a positive [Turtle.rotate]. Where the pen is down the path is drawn; afterwards the turtle faces the way the path
 * ends.
 *
 * The heading is the integral of the curvature and the position the integral of the heading, a curvature turtle as in
 * Abelson and diSessa, "Turtle Geometry" (1981). Both are integrated with the classical Runge-Kutta method in steps
 * that turn by at most 0.002 radians, and the path is drawn as cubic Béziers that turn by at most π/16 each, with their
 * end points and end directions on the integrated path and handles of the length that is exact for a circular arc of
 * the same turn.
 */
fun Turtle.curve(length: Double, curvature: (s: Double) -> Double) {
    if (length < 1E-6) return

    // the turtle's own frame: x along its direction, y the way a positive rotation turns it
    val origin = position
    val ax = (orientation * Vector4.UNIT_X).xy
    val ay = (orientation * Vector4.UNIT_Y).xy
    fun world(x: Double, y: Double) = origin + ax * x + ay * y
    fun worldDirection(theta: Double) = ax * cos(theta) + ay * sin(theta)

    // enough steps for the total turn, estimated from samples of the curvature
    val samples = 256
    val turnEstimate = (0 until samples).sumOf { abs(curvature((it + 0.5) * length / samples)) } * length / samples
    val steps = max(64.0, ceil(turnEstimate / MAX_STEP_TURN)).coerceAtMost(1_000_000.0).toInt()
    val h = length / steps

    var x = 0.0
    var y = 0.0
    var theta = 0.0
    var pieceStart = world(0.0, 0.0)
    var pieceTheta = 0.0
    var pieceS = 0.0
    var pieceTurn = 0.0

    for (step in 1..steps) {
        val s = (step - 1) * h
        // RK4 for x' = cos θ, y' = sin θ, θ' = κ(s)
        val k1t = curvature(s)
        val k1x = cos(theta); val k1y = sin(theta)
        val t2 = theta + h / 2 * k1t
        val k2t = curvature(s + h / 2)
        val k2x = cos(t2); val k2y = sin(t2)
        val t3 = theta + h / 2 * k2t
        val k3t = k2t
        val k3x = cos(t3); val k3y = sin(t3)
        val t4 = theta + h * k3t
        val k4t = curvature(s + h)
        val k4x = cos(t4); val k4y = sin(t4)
        x += h / 6 * (k1x + 2 * k2x + 2 * k3x + k4x)
        y += h / 6 * (k1y + 2 * k2y + 2 * k3y + k4y)
        val newTheta = theta + h / 6 * (k1t + 2 * k2t + 2 * k3t + k4t)
        pieceTurn += abs(newTheta - theta)
        theta = newTheta

        if (pieceTurn >= MAX_PIECE_TURN || step == steps) {
            val end = world(x, y)
            if (isPenDown) {
                val pieceLength = step * h - pieceS
                val delta = theta - pieceTheta
                val handle = if (abs(delta) < 1E-9) pieceLength / 3.0 else 4.0 / 3.0 * tan(delta / 4.0) / delta * pieceLength
                cb.curveTo(
                    pieceStart + worldDirection(pieceTheta) * handle,
                    end - worldDirection(theta) * handle,
                    end
                )
            }
            pieceStart = end
            pieceTheta = theta
            pieceS = step * h
            pieceTurn = 0.0
        }
    }
    if (!isPenDown) position = world(x, y)
    rotate(theta * 180.0 / PI)
}

private const val MAX_STEP_TURN = 0.002
private const val MAX_PIECE_TURN = PI / 16

/**
 * Moves the turtle [length] forward along a circular arc of constant [curvature], in radians per unit of length: the
 * arc of radius 1 / |curvature| that turns by curvature · length radians, the same way as [Turtle.rotate] for positive
 * curvature. Zero curvature moves straight ahead. See [curve].
 */
fun Turtle.arc(length: Double, curvature: Double) = curve(length) { curvature }

/**
 * Moves the turtle [length] forward along a clothoid (Euler spiral): the curvature changes linearly with the distance
 * travelled, from [startCurvature] to [endCurvature], in radians per unit of length, so the turtle turns by
 * (startCurvature + endCurvature) / 2 · length radians in total. Clothoids join straight lines and arcs without jumps
 * in curvature, as in road and track design. See [curve].
 */
fun Turtle.clothoid(length: Double, startCurvature: Double, endCurvature: Double) =
    curve(length) { s -> startCurvature + (endCurvature - startCurvature) * s / length }
