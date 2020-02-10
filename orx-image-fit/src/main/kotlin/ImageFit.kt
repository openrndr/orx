package org.openrndr.extras.imageFit

import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Drawer
import org.openrndr.math.map
import org.openrndr.shape.Rectangle


enum class FitMethod {
    Cover,
    Contain
}

fun Drawer.imageFit(img: ColorBuffer, x: Double = 0.0, y: Double = 0.0, w: Double, h: Double, fitMethod:FitMethod = FitMethod.Cover, horizontalPosition:Double = 0.0, verticalPosition:Double = 0.0) {
    val sourceWidth = img.width.toDouble()
    val sourceHeight = img.height.toDouble()

    var targetX = x
    var targetY = y

    var targetWidth: Double
    var targetHeight: Double

    val source: Rectangle
    val target: Rectangle

    when (fitMethod) {
        FitMethod.Contain -> {
            targetWidth = w
            targetHeight = h

            if (w <= targetWidth) {
                targetWidth = w
                targetHeight = (sourceHeight / sourceWidth) * w
            }

            if (h <= targetHeight) {
                targetHeight = h
                targetWidth = (sourceWidth / sourceHeight) * h
            }

            val left = x
            val right = x + w - targetWidth
            val top = y
            val bottom = y + h - targetHeight

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
                targetHeight = (h / w) * sourceWidth
            }

            if (sourceHeight <= targetHeight) {
                targetHeight = sourceHeight
                targetWidth = (w / h) * sourceHeight
            }

            val left = 0.0
            val right = sourceWidth - targetWidth
            val top = 0.0
            val bottom = sourceHeight - targetHeight

            targetX = map(-1.0, 1.0, left, right, horizontalPosition)
            targetY = map(-1.0, 1.0, top, bottom, verticalPosition)

            source = Rectangle(targetX, targetY, targetWidth, targetHeight)
            target = Rectangle(x, y, w, h)
        }
    }

    image(img, source, target)
}