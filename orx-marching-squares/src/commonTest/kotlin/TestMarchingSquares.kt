import org.openrndr.extra.marchingsquares.findContours
import org.openrndr.extra.marchingsquares.findContoursAdaptive
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import kotlin.math.PI
import kotlin.math.cos
import kotlin.test.Test
import kotlin.test.assertEquals

class TestMarchingSquares {

    @Test
    fun testUniformVsAdaptive() {

        val bounds = Rectangle(0.0, 0.0, 720.0, 720.0)

        fun f(v:Vector2) = cos((v.distanceTo(bounds.center)*0.1).coerceAtMost(6* PI) )

        val contours = findContours(::f, bounds, 16.0)
        val shape = Shape(contours)

        val contoursAdaptive = findContoursAdaptive(::f, bounds, 5)
        val shapeAdaptive = Shape(contoursAdaptive)

        assertEquals(contours.size, contoursAdaptive.size)

    }

    @Test
    fun testUniformVsAdaptive2() {

        val bounds = Rectangle(0.0, 0.0, 720.0, 720.0)

        fun f(v:Vector2) = cos((v.distanceTo(bounds.center)*0.1).coerceAtMost(14* PI) )

        val contours = findContours(::f, bounds, 16.0)
        val shape = Shape(contours)

        val contoursAdaptive = findContoursAdaptive(::f, bounds, 6)
        val shapeAdaptive = Shape(contoursAdaptive)



        val contoursSorted = contours.sortedBy { it.length }
        val contoursAdaptiveSorted = contoursAdaptive.sortedBy { it.length }

        for (i in 0 until contours.size) {
            println("$i ${contoursSorted[i].length} ${contoursAdaptiveSorted[i].length}")
            assertEquals(contoursSorted[i].length, contoursAdaptiveSorted[i].length, 9.0)
        }


    }
}