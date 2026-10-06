package fit

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.fit.fitCubicBeziers
import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour

/**
 * An interactive demonstration of the
 * [fitCubicBeziers](https://orx.openrndr.org/orx-shapes/org.openrndr.extra.shapes.fit/fit-cubic-beziers.html) method.
 * Drag the mouse to add points to a growing contour. `fitCubicBeziers` will convert the
 * collection of points into a smooth ShapeContour using Bézier curves.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }

    program {
        val points = mutableListOf<Vector2>()

        if (System.getProperty("takeScreenshot") == "true") {
            val demoPoints = listOf(116, 212, 116, 224, 117, 241, 117, 256, 117, 272, 116, 287, 116, 300, 115, 312, 114, 324, 114, 334, 113, 348, 112, 359, 109, 348, 110, 336, 112, 323, 115, 311, 118, 301, 121, 291, 127, 283, 133, 274, 141, 267, 152, 263, 164, 266, 171, 273, 177, 285, 179, 297, 180, 308, 180, 319, 180, 329, 179, 339, 178, 350, 176, 339, 179, 328, 183, 318, 188, 307, 193, 297, 204, 293, 214, 295, 224, 297, 236, 299, 247, 299, 258, 297, 267, 291, 271, 280, 270, 269, 265, 259, 257, 252, 245, 251, 235, 257, 229, 266, 223, 277, 217, 288, 213, 300, 213, 312, 216, 324, 221, 334, 229, 343, 238, 348, 250, 349, 260, 348, 270, 344, 281, 337, 291, 327, 298, 319, 304, 311, 313, 300, 321, 289, 325, 279, 330, 270, 334, 256, 338, 242, 341, 229, 342, 219, 342, 209, 336, 201, 325, 207, 318, 216, 312, 225, 308, 238, 305, 253, 304, 264, 304, 274, 305, 287, 308, 297, 311, 310, 313, 320, 317, 331, 322, 340, 328, 349, 338, 354, 349, 351, 361, 345, 369, 337, 378, 328, 387, 317, 396, 306, 404, 295, 411, 285, 418, 274, 423, 265, 427, 255, 431, 245, 434, 233, 434, 220, 432, 210, 424, 202, 413, 202, 402, 206, 390, 213, 382, 220, 375, 229, 369, 239, 365, 249, 362, 258, 360, 269, 359, 284, 359, 295, 361, 307, 364, 317, 370, 329, 376, 339, 384, 348, 394, 353, 407, 353, 417, 351, 427, 347, 435, 341, 442, 331, 447, 319, 451, 306, 456, 295, 462, 285, 471, 279, 483, 278, 495, 280, 506, 283, 494, 282, 483, 284, 472, 288, 463, 293, 453, 301, 445, 310, 438, 321, 436, 331, 439, 341, 450, 346, 462, 347, 475, 347, 486, 345, 497, 341, 507, 336, 515, 325, 521, 313, 523, 302, 523, 291, 518, 283, 509, 279, 499, 282, 493, 291, 493, 302, 503, 306, 513, 306, 524, 304, 534, 300, 544, 293, 554, 286, 564, 278)
                .map { it.toDouble() }.chunked(2).map { Vector2(it[0], it[1]) }
            points.addAll(demoPoints)
        }

        mouse.dragged.listen {
            if (points.isEmpty() || points.last().distanceTo(it.position) > 10.0) {
                points.add(mouse.position)
            }
        }

        extend {
            if (points.size > 2) {
                val segments = fitCubicBeziers(points, maxDepth = 15)
                val c = ShapeContour.fromSegments(segments, false)

                drawer.fill = null
                drawer.stroke = ColorRGBa.PINK
                drawer.drawStyle.lineJoin = org.openrndr.draw.LineJoin.ROUND
                drawer.contour(c)
                drawer.circles(c.segments.map { it.start }, 5.0)
            }
            drawer.fill = null
            drawer.stroke = ColorRGBa.PINK

            //drawer.circles(points, 5.0)
            drawer.circle(mouse.position, 5.0)
        }
    }
}
