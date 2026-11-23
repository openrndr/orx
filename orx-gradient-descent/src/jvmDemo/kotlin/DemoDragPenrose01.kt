import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

/**
 * A Penrose tiling as the projection of five dimensions: de Bruijn's pentagrid, five families of parallel lines
 * shifted by a vector γ of five dimensions, gives a rhombus for every crossing of two lines, and every corner of the
 * rhombi a point K of the lattice of five dimensions, which the projection P, of five vectors in the plane, puts at
 * P K. The rhombi of the families of lines next to each other are thick, the others thin.
 *
 * The parameters are the projection, the five vectors, ten numbers: any corner of any rhombus can be dragged, which
 * changes the projection as little as it can to put the corner at the mouse, see [DragParameters], and so all edges
 * of the same family together; the five vectors themselves are the star at the lower left. The shift γ changes the
 * tiling itself, not smoothly, so it is not dragged: the space bar moves it through five dimensions, and the rhombi
 * flip where the lines cross differently.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(4)
    }
    program {
        val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 16.0)
        val scale = 32.0
        val lines = 6
        val directions = (0 until 5).map { Vector2(cos(2 * PI * it / 5), sin(2 * PI * it / 5)) }

        /** a rhombus: its corners as points of the lattice of five dimensions, and whether it is thick */
        class Rhombus(val corners: List<IntArray>, val thick: Boolean)

        /** the rhombi of the pentagrid shifted by [gamma] */
        fun pentagrid(gamma: DoubleArray): List<Rhombus> {
            val rhombi = mutableListOf<Rhombus>()
            for (r in 0 until 5) for (s in r + 1 until 5) {
                val er = directions[r]
                val es = directions[s]
                val det = er.x * es.y - er.y * es.x
                for (nr in -lines..lines) for (ns in -lines..lines) {
                    // the crossing of line nr of family r and line ns of family s
                    val br = nr - gamma[r]
                    val bs = ns - gamma[s]
                    val z = Vector2((br * es.y - bs * er.y) / det, (er.x * bs - es.x * br) / det)
                    val k = IntArray(5) { j -> ceil(z.dot(directions[j]) + gamma[j]).toInt() }
                    val corners = listOf(0 to 0, 1 to 0, 1 to 1, 0 to 1).map { (dr, ds) ->
                        k.copyOf().also { it[r] = nr + dr; it[s] = ns + ds }
                    }
                    rhombi.add(Rhombus(corners, (s - r) % 5 == 1 || (s - r) % 5 == 4))
                }
            }
            return rhombi
        }

        // a shift of which the parts add up to 0, which makes the tiling a Penrose tiling
        val gamma = doubleArrayOf(0.2, -0.1, 0.33, -0.27, -0.16)
        var rhombi = pentagrid(gamma)
        var corners = rhombi.flatMap { it.corners }.distinctBy { it.toList() }

        /** where the projection [x] puts the point [k] of the lattice */
        fun project(k: IntArray, x: DoubleArray): Vector2 {
            var p = Vector2.ZERO
            for (j in 0 until 5) p += Vector2(x[2 * j], x[2 * j + 1]) * k[j].toDouble()
            return p * scale
        }

        val star = Vector2(-280.0, 280.0)
        val drag = extend(DragParameters(directions.flatMap { listOf(it.x, it.y) }.toDoubleArray(), drawer.bounds.center) { x ->
            (0 until 5).map { star + Vector2(x[2 * it], x[2 * it + 1]) * 60.0 } + corners.map { project(it, x) }
        })
        // a point of the star is one of the vectors
        drag.affects = { if (it < 5) intArrayOf(2 * it, 2 * it + 1) else null }
        drag.damping = 500.0

        var phason = false
        keyboard.keyDown.listen { if (it.name == "space") phason = !phason }
        var last = seconds

        extend {
            // the shift moves through five dimensions, but not while a corner is dragged
            if (phason && drag.dragged < 0) {
                val dt = seconds - last
                val step = DoubleArray(5) { j -> dt * 0.15 * cos(j * 2.0 + seconds * 0.1) }
                // the parts of the shift keep adding up to 0
                for (j in 0 until 5) gamma[j] += step[j] - step.average()
                rhombi = pentagrid(gamma)
                corners = rhombi.flatMap { it.corners }.distinctBy { it.toList() }
            }
            last = seconds

            drawer.translate(drag.origin)
            val x = drag.parameters
            for (thick in listOf(true, false)) {
                drawer.fill = ColorRGBa.fromHex(if (thick) "#f0a030" else "#5ab0ff")
                drawer.stroke = ColorRGBa.BLACK
                drawer.contours(rhombi.filter { it.thick == thick }.map { r ->
                    ShapeContour.fromPoints(r.corners.map { project(it, x) }, closed = true)
                })
            }
            // the projection: five vectors, the edges of the five families
            drawer.fill = ColorRGBa.BLACK.opacify(0.7)
            drawer.stroke = null
            drawer.circle(star, 80.0)
            drawer.stroke = ColorRGBa.WHITE
            drawer.strokeWeight = 2.0
            for (j in 0 until 5) drawer.lineSegment(star, star + Vector2(x[2 * j], x[2 * j + 1]) * 60.0)
            drawer.strokeWeight = 1.0
            drag.drawPoints(drawer, ColorRGBa.TRANSPARENT, ColorRGBa.WHITE)
            drawer.stroke = null
            drawer.fill = ColorRGBa.WHITE
            drawer.circles((0 until 5).map { star + Vector2(x[2 * it], x[2 * it + 1]) * 60.0 }, 5.0)

            drawer.fontMap = font
            drawer.text("space: ${if (phason) "stop" else "move"} the shift through five dimensions", -340.0, -330.0)
        }
    }
}
