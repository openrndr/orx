import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A tree of branches that split in two, eight times: every branch is the attenuation times as long as the branch it
 * grows from, and turns the spread angle to the left or the right of it. Dragging the tip of any branch changes the
 * spread, the attenuation and the length of the trunk, see [DragParameters].
 */
fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(4)
    }
    program {
        val depth = 8
        val root = Vector2(0.0, 330.0)

        /** a branch from [start] to [tip], [level] branches from the root, 1 for the trunk */
        class Branch(val start: Vector2, val tip: Vector2, val level: Int)

        fun branches(spread: Double, attenuation: Double, length: Double): List<Branch> {
            val result = mutableListOf<Branch>()
            fun grow(start: Vector2, angle: Double, length: Double, level: Int) {
                val tip = start + Vector2(cos(angle), sin(angle)) * length
                result.add(Branch(start, tip, level))
                if (level < depth) {
                    grow(tip, angle - spread, length * attenuation, level + 1)
                    grow(tip, angle + spread, length * attenuation, level + 1)
                }
            }
            grow(root, -PI / 2, length, 1)
            return result
        }

        val drag = extend(DragParameters(doubleArrayOf(0.5, 0.72, 160.0), drawer.bounds.center) { (spread, attenuation, length) ->
            branches(spread, attenuation, length).map { it.tip }
        })
        extend {
            drawer.translate(drag.origin)
            val (spread, attenuation, length) = drag.parameters
            // the trunk and the first branches brown, the rest green, thinner the further out
            for (branch in branches(spread, attenuation, length)) {
                drawer.stroke = if (branch.level <= 3) ColorRGBa.fromHex("#a0703c") else ColorRGBa.fromHex("#6cc06c")
                drawer.strokeWeight = (depth + 1 - branch.level) * 1.2
                drawer.lineSegment(branch.start, branch.tip)
            }
            drawer.strokeWeight = 1.0
            // only the tip under the mouse is drawn, all of them can be dragged
            drag.drawPoints(drawer, ColorRGBa.TRANSPARENT)
        }
    }
}
