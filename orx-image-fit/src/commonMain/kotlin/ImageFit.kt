package org.openrndr.extra.imageFit

import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Drawer
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.transform
import org.openrndr.shape.Rectangle
import kotlin.math.max
import kotlin.math.min

/**
 * Available `object-fit` methods (borrowed from CSS)
 */
enum class FitMethod {
    /** Cover target area. Crop the source image if needed. */
    Cover,

    /** Fit image in target area. Add margins if needed. */
    Contain,

    /** Deform source image to match the target area. */
    Fill,

    /** Maintain original image scale, crop to target area size. */
    None

    /** Not implemented */
    // ScaleDown
}

/**
 * Transforms [src] and [dest] into a Pair in which one of the
 * two rectangles is modified to conform with the [fitMethod]. It uses
 * [horizontalPosition] and [verticalPosition] to control positioning / cropping.
 */
fun fitRectangle(
    src: Rectangle,
    dest: Rectangle,
    horizontalPosition: Double = 0.0,
    verticalPosition: Double = 0.0,
    fitMethod: FitMethod = FitMethod.Cover
): Pair<Rectangle, Rectangle> {
    val positionNorm = Vector2(horizontalPosition, verticalPosition) * 0.5 + 0.5
    val (scaleX, scaleY) = dest.dimensions / src.dimensions

    return when (fitMethod) {
        FitMethod.Cover -> {
            val actualDimensions = dest.dimensions / max(scaleX, scaleY)
            val actualSrc = Rectangle(
                src.corner + (src.dimensions - actualDimensions) * positionNorm,
                actualDimensions.x, actualDimensions.y
            )
            Pair(actualSrc, dest)
        }

        FitMethod.Contain -> {
            val actualDimensions = src.dimensions * min(scaleX, scaleY)
            val actualDest = Rectangle(
                dest.corner + (dest.dimensions - actualDimensions) * positionNorm,
                actualDimensions.x, actualDimensions.y
            )
            Pair(src, actualDest)
        }

        FitMethod.Fill -> Pair(src, dest)
        FitMethod.None -> {
            val actualSrc = Rectangle(
                src.corner + (src.dimensions - dest.dimensions) * positionNorm,
                dest.width, dest.height
            )
            Pair(actualSrc, dest)
        }
    }
}

/**
 * Helper function that calls [fitRectangle] and returns a [Matrix44] instead
 * of a `Pair<Rectangle, Rectangle>`. The returned matrix can be used to draw
 * scaled `Shape` or `ShapeContour` objects.
 *
 * Example scaling and centering a collection of ShapeContours inside
 * `drawer.bounds` leaving a margin of 50 pixels:
 *
 * val src = shapeContours.map { it.bounds }.bounds
 * val dest = drawer.bounds.offsetEdges(-50.0)
 * val mat = src.fit(dest, fitMethod = FitMethod.Contain)
 * drawer.view *= mat
 * drawer.contours(shapeContours)
 */
fun Rectangle.fit(
    dest: Rectangle,
    horizontalPosition: Double = 0.0,
    verticalPosition: Double = 0.0,
    fitMethod: FitMethod = FitMethod.Cover
): Matrix44 {
    val (source, target) = fitRectangle(
        this,
        dest,
        horizontalPosition,
        verticalPosition,
        fitMethod
    )
    return transform {
        translate(target.corner)
        scale((target.dimensions / source.dimensions).vector3(z = 1.0))
        translate(-source.corner)
    }
}

/**
 * Draws [img] into the bounding box defined by [x], [y], [width] and [height]
 * using the specified [fitMethod]
 * and aligned or cropped using [horizontalPosition] and [verticalPosition].
 */
fun Drawer.imageFit(
    img: ColorBuffer,
    x: Double = 0.0,
    y: Double = 0.0,
    width: Double = img.width.toDouble(),
    height: Double = img.height.toDouble(),
    horizontalPosition: Double = 0.0,
    verticalPosition: Double = 0.0,
    fitMethod: FitMethod = FitMethod.Cover
) = imageFit(
    img,
    Rectangle(x, y, width, height),
    horizontalPosition,
    verticalPosition,
    fitMethod
)

/**
 * Draws [img] into the bounding box defined by [bounds]
 * using the specified [fitMethod]
 * and aligned or cropped using [horizontalPosition] and [verticalPosition].
 */
fun Drawer.imageFit(
    img: ColorBuffer,
    bounds: Rectangle = img.bounds,
    horizontalPosition: Double = 0.0,
    verticalPosition: Double = 0.0,
    fitMethod: FitMethod = FitMethod.Cover
): Pair<Rectangle, Rectangle> {
    val (source, target) = fitRectangle(
        img.bounds,
        bounds,
        horizontalPosition,
        verticalPosition,
        fitMethod
    )

    image(img, source, target)
    return Pair(source, target)
}