package org.openrndr.extra.shapes

import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun regularStar(points: Int, innerRadius: Double, outerRadius: Double, center: Vector2 = Vector2.ZERO, phase: Double = 0.0): ShapeContour {
    return contour {
        val theta = Math.toRadians(phase)
        val phi = PI * 2.0 / (points * 2)
        for (i in 0 until points * 2 step 2) {
            val outerPoint = Vector2(cos(i * phi + theta), sin(i * phi + theta)) * outerRadius + center
            val innerPoint = Vector2(cos((i + 1) * phi + theta), sin((i + 1) * phi + theta)) * innerRadius + center
            moveOrLineTo(outerPoint)
            lineTo(innerPoint)
        }
        close()
    }
}

fun regularStarRounded(points: Int, innerRadius: Double, outerRadius: Double,
                       innerFactor: Double, outerFactor: Double,
                       center: Vector2 = Vector2.ZERO,
                       phase: Double = 0.0): ShapeContour {
    return contour {
        val theta = Math.toRadians(phase)
        val phi = PI * 2.0 / (points * 2)
        for (i in 0 until points * 2 step 2) {
            val outerPoint0 = Vector2(cos(i * phi + theta), sin(i * phi + theta)) * outerRadius + center
            val innerPoint = Vector2(cos((i + 1) * phi + theta), sin((i + 1) * phi + theta)) * innerRadius + center
            val outerPoint1 = Vector2(cos((i + 2) * phi + theta), sin((i + 2) * phi + theta)) * outerRadius + center
            val innerPoint1 = Vector2(cos((i + 3) * phi + theta), sin((i + 3) * phi + theta)) * innerRadius + center

            val fo = (outerFactor * 0.5)
            val fi = (innerFactor * 0.5)

            val p0 = innerPoint - (innerPoint - outerPoint0) * fi
            val p1 = innerPoint + (outerPoint1 - innerPoint) * fi
            val p2 = outerPoint1 - (outerPoint1 - innerPoint) * fo
            val p3 = outerPoint1 + (innerPoint1 - outerPoint1) * fo

            moveOrLineTo(p0)
            curveTo(innerPoint, p1)
            lineTo(p2)
            curveTo(outerPoint1, p3)
        }
        close()
    }
}

fun regularStarBeveled(points: Int, innerRadius: Double, outerRadius: Double,
                       innerFactor: Double, outerFactor: Double,
                       center: Vector2 = Vector2.ZERO,
                       phase: Double = 0.0): ShapeContour {
    return contour {
        val theta = Math.toRadians(phase)
        val phi = PI * 2.0 / (points * 2)
        for (i in 0 until points * 2 step 2) {
            val outerPoint0 = Vector2(cos(i * phi + theta), sin(i * phi + theta)) * outerRadius + center
            val innerPoint = Vector2(cos((i + 1) * phi + theta), sin((i + 1) * phi + theta)) * innerRadius + center
            val outerPoint1 = Vector2(cos((i + 2) * phi + theta), sin((i + 2) * phi + theta)) * outerRadius + center
            val innerPoint1 = Vector2(cos((i + 3) * phi + theta), sin((i + 3) * phi + theta)) * innerRadius + center

            val fo = (outerFactor * 0.5)
            val fi = (innerFactor * 0.5)

            val p0 = innerPoint - (innerPoint - outerPoint0) * fi
            val p1 = innerPoint + (outerPoint1 - innerPoint) * fi
            val p2 = outerPoint1 - (outerPoint1 - innerPoint) * fo
            val p3 = outerPoint1 + (innerPoint1 - outerPoint1) * fo

            moveOrLineTo(p0)
            lineTo(p1)
            lineTo(p2)
            lineTo(p3)
        }
        close()
    }
}