package org.openrndr.extras.imageFit

import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Drawer
import org.openrndr.math.map
import org.openrndr.shape.Rectangle


enum class FitMethod {
    Cover,
    Contain
}

fun fitRectangle(
        src: Rectangle,
        dest: Rectangle,
        horizontalPosition: Double = 0.0,
        verticalPosition: Double = 0.0,
        fitMethod: FitMethod = FitMethod.Cover
): Pair<Rectangle, Rectangle> {
    val sourceWidth = src.width
    val sourceHeight = src.height

    val targetX: Double
    val targetY: Double
    var targetWidth: Double
    var targetHeight: Double

    val source: Rectangle
    val target: Rectangle

    val (x, y) = dest.corner
    val width = dest.width
    val height = dest.height

    when (fitMethod) {
        FitMethod.Contain -> {
            targetWidth = width
            targetHeight = height

            if (width <= targetWidth) {
                targetWidth = width
                targetHeight = (sourceHeight / sourceWidth) * width
            }

            if (height <= targetHeight) {
                targetHeight = height
                targetWidth = (sourceWidth / sourceHeight) * height
            }

            val left = x
            val right = x + width - targetWidth
            val top = y
            val bottom = y + height - targetHeight

            targetX = map(-1.0, 1.0, left, right, horizontalPosition)
            targetY = map(-1.0, 1.0, top, bottom, verticalPosition)

            source = Rectangle(0.0, 0.0, sourceWidth, sourceHeight)
            target = Rectangle(targetX, targetY, targetWidth, targetHeight)
        }

        FitMethod.Cover -> {
            targetWidth = sourceWidth
            targetHeight = sourceHeight

            if (sourceWidth <= targetWidth) {
                targetWidth = sourceWidth
                targetHeight = (height / width) * sourceWidth
            }

            if (sourceHeight <= targetHeight) {
                targetHeight = sourceHeight
                targetWidth = (width / height) * sourceHeight
            }

            val left = 0.0
            val right = sourceWidth - targetWidth
            val top = 0.0
            val bottom = sourceHeight - targetHeight

            targetX = map(-1.0, 1.0, left, right, horizontalPosition)
            targetY = map(-1.0, 1.0, top, bottom, verticalPosition)

            source = Rectangle(targetX, targetY, targetWidth, targetHeight)
            target = Rectangle(x, y, width, height)
        }
    }

    return Pair(source, target)
}

fun Drawer.imageFit(
        img: ColorBuffer,
        x: Double = 0.0,
        y: Double = 0.0,
        width: Double = img.width.toDouble(),
        height: Double = img.height.toDouble(),
        horizontalPosition: Double = 0.0,
        verticalPosition: Double = 0.0,
        fitMethod: FitMethod = FitMethod.Cover
): Pair<Rectangle, Rectangle> {
    val (source, target) = fitRectangle(
            img.bounds,
            Rectangle(x, y, width, height),
            horizontalPosition,
            verticalPosition,
            fitMethod
    )

    image(img, source, target)
    return Pair(source, target)
}