import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.extra.gradientdescent.minimize
import org.openrndr.math.Vector2

/**
 * Direct manipulation of a drawing through its parameters, in the style of g9 (https://omrelli.ug/g9/): [points]
 * computes the points of the drawing that can be dragged from [parameters]; dragging one of them changes the
 * parameters with [minimize], so that the point follows the mouse and the rest of the drawing follows the parameters.
 * Points are relative to [origin], the position in the window that the drawing is translated to; [affects] can
 * limit the parameters that dragging a point changes, and [grab] lets the drawing be dragged by any other place too.
 */
class DragParameters(
    val parameters: DoubleArray,
    val origin: Vector2 = Vector2.ZERO,
    val points: (DoubleArray) -> List<Vector2>
) : Extension {
    override var enabled = true

    /** the index of the point that is dragged, or -1 */
    var dragged = -1
        private set

    /** the index of the point under the mouse, or -1 */
    var hovered = -1
        private set

    /** how close the mouse has to be to a point to drag it */
    var reach = 12.0

    /** the most iterations of the minimization for one move of the mouse */
    var maxIterations = 50

    /**
     * the indices of the parameters that dragging the point of the given index changes, the others stay as they are;
     * null for all of them
     */
    var affects: (Int) -> IntArray? = { null }

    /**
     * the weight of the change of the parameters against the squared distance of the point to the mouse: above 0 the
     * smallest change that brings the point to the mouse is preferred, which keeps a drawing of more parameters than
     * the point needs from jumping between the many that put it there
     */
    var damping = 0.0

    /**
     * a term of the parameters that is minimized with the distance to the mouse, 0 when the drawing is as it should be:
     * constraints that dragging keeps, like the links of a linkage that stay joined
     */
    var penalty: (DoubleArray) -> Double = { 0.0 }

    /**
     * what is grabbed when the mouse goes down at the given position away from the points: a function of the
     * parameters that tells where the grabbed place of the drawing is, which dragging brings to the mouse like a
     * point; null when nothing can be grabbed there
     */
    var grab: (Vector2) -> ((DoubleArray) -> Vector2)? = { null }

    /** what is grabbed and dragged, away from the points */
    private var grabbed: ((DoubleArray) -> Vector2)? = null

    override fun setup(program: Program) {
        program.mouse.moved.listen { hovered = nearest(it.position - origin) }
        program.mouse.buttonDown.listen {
            dragged = nearest(it.position - origin)
            grabbed = if (dragged < 0) grab(it.position - origin) else null
        }
        program.mouse.dragged.listen { event ->
            val target = event.position - origin
            val k = dragged
            if (k >= 0) follow(target, affects(k)) { points(it)[k] }
            else grabbed?.let { follow(target, null, it) }
        }
        program.mouse.buttonUp.listen {
            dragged = -1
            grabbed = null
            hovered = nearest(it.position - origin)
        }
    }

    private fun nearest(p: Vector2): Int = points(parameters).withIndex()
        .filter { (_, q) -> q.distanceTo(p) < reach }
        .minByOrNull { (_, q) -> q.distanceTo(p) }?.index ?: -1

    /**
     * changes the parameters of [affected], all when null, such that the place of the drawing that [place] tells is as
     * close to [target] as they can bring it
     */
    private fun follow(target: Vector2, affected: IntArray?, place: (DoubleArray) -> Vector2) {
        val indices = affected ?: IntArray(parameters.size) { it }
        if (indices.isEmpty()) return
        val x = parameters.copyOf()
        try {
            val start = DoubleArray(indices.size) { parameters[indices[it]] }
            val result = minimize(start, maxIterations = maxIterations) { y ->
                var change = 0.0
                for (i in indices.indices) {
                    x[indices[i]] = y[i]
                    change += (y[i] - start[i]) * (y[i] - start[i])
                }
                place(x).squaredDistanceTo(target) + damping * change + penalty(x)
            }
            if (result.solution.all { it.isFinite() }) {
                for (i in indices.indices) parameters[indices[i]] = result.solution[i]
            }
        } catch (e: IllegalArgumentException) {
            // the drawing is not defined everywhere, like a circle through points on a line: the parameters stay
        }
    }

    /** draws the points that can be dragged, the one under the mouse or dragged larger */
    fun drawPoints(drawer: Drawer, color: ColorRGBa = ColorRGBa.WHITE, highlight: ColorRGBa = ColorRGBa.PINK) {
        val active = if (dragged >= 0) dragged else hovered
        drawer.stroke = null
        for ((i, p) in points(parameters).withIndex()) {
            if (!p.x.isFinite() || !p.y.isFinite()) continue
            drawer.fill = if (i == active) highlight else color
            drawer.circle(p, if (i == active) 7.0 else 4.0)
        }
    }
}
