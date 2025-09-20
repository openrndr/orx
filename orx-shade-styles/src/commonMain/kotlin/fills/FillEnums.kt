package org.openrndr.extra.shadestyles.fills


/**
 * Specifies how to fill shapes with the gradient
 */
enum class FillFit {
    /** Deforms the gradient to match the bounds of the shape */
    STRETCH,

    /** Resizes the gradient to cover the bounds of the shape */
    COVER,

    /** Resizes the gradient to fit inside the bounds of the shape */
    CONTAIN
}

/**
 * Specifies what units are coordinates given in
 */
enum class FillUnits {
    /** Normalized coordinates, with (0.5, 0.5) at the center of the gradient. */
    BOUNDS,

    /** Screen coordinates in pixels */
    WORLD,

    VIEW,

    SCREEN,
}

/**
 * Specifies how to extend a gradient when outside the normalized range
 */
enum class SpreadMethod {
    /** Stretches the edge color */
    PAD,

    /** Mirrors the color in a ping-pong fashion, as if traveling through the gradient back and forth */
    REFLECT,

    /** Loops through the gradient as needed */
    REPEAT
}
