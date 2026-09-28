package org.openrndr.extra.camera

import org.openrndr.Extension
import org.openrndr.KEY_ARROW_DOWN
import org.openrndr.KEY_ARROW_LEFT
import org.openrndr.KEY_ARROW_RIGHT
import org.openrndr.KEY_ARROW_UP
import org.openrndr.KEY_PAGE_DOWN
import org.openrndr.KEY_PAGE_UP
import org.openrndr.KeyEvent
import org.openrndr.MouseButton
import org.openrndr.MouseEvent
import org.openrndr.Program
import org.openrndr.draw.DepthTestPass
import org.openrndr.draw.Drawer
import org.openrndr.events.Event
import org.openrndr.extra.camera.projection.generalizedProjection
import org.openrndr.math.Spherical
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.asDegrees
import org.openrndr.math.asRadians
import org.openrndr.math.transforms.lookAt as lookAt_
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

/**
 * An orbital camera and its mouse/keyboard controls in a single [Extension] -- the way
 * [Orbital] combines [OrbitalCamera] and [OrbitalControls], except there's no separate
 * camera/controls pair to wire together here, and the projection matrix comes from
 * [generalizedProjection] instead of [Drawer.perspective]/[Drawer.ortho].
 *
 * That swap replaces [OrbitalCamera]'s discrete [ProjectionType] -- PERSPECTIVE vs ORTHOGONAL,
 * each with its own size field ([fov] vs a separate ortho `magnitude`) -- with a single
 * continuous [perspective] blend (`0.0` fully parallel, `1.0` fully perspective) applied to one
 * viewport rectangle, sized by [fov] at the current orbit radius (the eye-to-[lookAt]
 * distance) rather than at [near]. For a full perspective projection that choice of reference
 * distance doesn't matter -- generalizedProjection's perspective divide rescales the rectangle
 * by each point's *actual* distance regardless of where it was measured, the usual
 * similar-triangles invariance of a frustum -- but it matters a great deal once [perspective]
 * drops below `1.0`: with no such rescaling left, an orthographic projection uses the
 * rectangle's size directly as the visible width of the whole scene, at every depth, so it has
 * to already be in scale with the orbit rather than a sliver measured a fraction of a unit from
 * the eye. One consequence: [dolly] (mouse wheel) changes what's visible in *both* modes now,
 * since it changes that reference distance -- there's no separate ortho magnitude/near/far
 * bookkeeping, and no mode where scrolling does nothing.
 */
class GeneralizedOrbital(
    eye: Vector3 = Vector3.UNIT_Z * 10.0,
    lookAt: Vector3 = Vector3.ZERO,
    var fov: Double = 90.0,
    /**
     * The near clip plane's distance from the eye at [perspective] = `1.0`; the *effective*
     * near clip distance tapers linearly to `0.0` as [perspective] drops toward `0.0` -- see
     * [beforeDraw].
     */
    var near: Double = 0.1,
    var far: Double = 1000.0
) : Extension, ChangeEvents {

    override var enabled: Boolean = true
    override val changed = Event<Unit>()
    override val hasChanged: Boolean get() = dirty

    /**
     * `0.0` = fully parallel (orthographic), `1.0` = fully perspective; see
     * [generalizedProjection]. Damped like the other orbit properties -- set it directly for
     * an instant change, or animate it smoothly with [perspectiveTo].
     */
    var perspective = 1.0

    /**
     * Shifts [generalizedProjection]'s `viewportCenter` in x/y, in the same world units as the
     * viewport rectangle itself -- an off-axis (sheared) frustum shift, the way a lens-shift
     * lens or a tiled/CAVE display's off-center screen would, *without* moving or rotating the
     * eye. `Vector2.ZERO` is the ordinary on-axis frustum. Damped like the other orbit
     * properties -- set it directly for an instant change, or animate it smoothly with
     * [offsetTo].
     */
    var offset = Vector2.ZERO

    var userInteraction = true
    var keySpeed = 1.0
    var zoomSpeed = 1.0

    /** Damping factor for camera motion, set to 0 for no damping. */
    var dampingFactor = 0.1
    var depthTest = true

    var lookAt = lookAt
        private set
    var spherical = Spherical.fromVector(eye - this.lookAt).makeSafe()
        private set
    var fovEnd = fov
    var perspectiveEnd = perspective
    var offsetEnd = offset

    /** The camera's world-space position; setting it re-derives [spherical] from [lookAt]. */
    var eye: Vector3
        get() = Vector3.fromSpherical(spherical) + lookAt
        set(value) = rotateTo(value, instant = true)

    private var sphericalEnd = spherical
    private var lookAtEnd = this.lookAt
    private var dirty: Boolean = true
        set(value) {
            if (value && !field) {
                changed.trigger(Unit)
            }
            field = value
        }
    private var lastSeconds = -1.0

    val eyeDefault = eye
    val lookAtDefault = this.lookAt.copy()
    val fovDefault = fov
    val perspectiveDefault = perspective
    val offsetDefault = offset

    private lateinit var program: Program
    private var mouseState = MouseState.NONE
    private lateinit var lastMousePosition: Vector2

    private enum class MouseState { NONE, ROTATE, PAN }

    /** Reinitialize the camera to its initial (construction-time) state. */
    fun defaults(instant: Boolean = false) {
        panTo(lookAtDefault, instant)
        rotateTo(eyeDefault, instant)
        zoomTo(fovDefault, instant)
        perspectiveTo(perspectiveDefault, instant)
        offsetTo(offsetDefault, instant)
    }

    fun setView(
        lookAt: Vector3,
        spherical: Spherical,
        fov: Double,
        perspective: Double,
        offset: Vector2 = Vector2.ZERO
    ) {
        this.lookAt = lookAt
        this.lookAtEnd = lookAt
        this.spherical = spherical
        this.sphericalEnd = spherical
        this.fov = fov
        this.fovEnd = fov
        this.perspective = perspective
        this.perspectiveEnd = perspective
        this.offset = offset
        this.offsetEnd = offset
    }

    fun rotate(degreesX: Double, degreesY: Double, instant: Boolean = false) {
        sphericalEnd = (sphericalEnd + Spherical(degreesX, degreesY, 0.0)).makeSafe()
        if (instant) spherical = sphericalEnd
        dirty = true
    }

    fun rotateTo(degreesX: Double, degreesY: Double, instant: Boolean = false) {
        sphericalEnd = sphericalEnd.copy(theta = degreesX, phi = degreesY).makeSafe()
        if (instant) spherical = sphericalEnd
        dirty = true
    }

    fun rotateTo(eye: Vector3, instant: Boolean = false) {
        sphericalEnd = Spherical.fromVector(eye - lookAt).makeSafe()
        if (instant) spherical = sphericalEnd
        dirty = true
    }

    fun dolly(distance: Double, instant: Boolean = false) {
        sphericalEnd += Spherical(0.0, 0.0, distance)
        if (instant) spherical = sphericalEnd
        dirty = true
    }

    fun dollyTo(distance: Double, instant: Boolean = false) {
        sphericalEnd = sphericalEnd.copy(radius = distance)
        if (instant) spherical = sphericalEnd
        dirty = true
    }

    /**
     * Dollies to [distance] the way `DemoGeneralizedPerspective01` establishes a dolly zoom --
     * compensating [fov] as the eye moves so whatever sits at [lookAt] keeps the same apparent
     * size, leaving only the depth-dependent stretch/compression (the Vertigo effect) visible
     * elsewhere in the scene. The demo derives its viewport size from `subjectDistance /
     * distance`; since [fov] plays the same role here that viewport size did there (both feed
     * the *same* `2 * distance * tan(fov / 2)` viewport-height formula [beforeDraw] uses --
     * see the class doc), the equivalent invariant to hold is `distance * tan(fov / 2) =
     * constant`. That constant is taken from the *current target* ([sphericalEnd]'s radius and
     * [fovEnd]) at the moment this is called, not [spherical]/[fov] directly, so chained or
     * repeated calls compose the way [dolly]/[zoom] do instead of resetting to whatever framing
     * happened to be on screen.
     *
     * [distance] and the compensated [fov] are both damped like any other orbit property --
     * during the transition the invariant is only approximate (each eases independently, at
     * the same rate), settling to exact once both reach [distance] and the new [fovEnd].
     */
    fun dollyZoomTo(distance: Double, instant: Boolean = false) {
        val referenceHalfFov = fovEnd.asRadians / 2.0
        val newHalfFov = atan(sphericalEnd.radius * tan(referenceHalfFov) / distance)
        dollyTo(distance, instant)
        zoomTo((newHalfFov * 2.0).asDegrees, instant)
    }

    fun dollyIn(amount: Double = 1.0, instant: Boolean = false) {
        val zoomScale = (1.0 - abs(amount * 0.05)).pow(zoomSpeed)
        dolly(sphericalEnd.radius * zoomScale - sphericalEnd.radius, instant)
    }

    fun dollyOut(amount: Double = 1.0, instant: Boolean = false) {
        val zoomScale = (1.0 - abs(amount * 0.05)).pow(zoomSpeed)
        dolly(sphericalEnd.radius / zoomScale - sphericalEnd.radius, instant)
    }

    fun pan(x: Double, y: Double, z: Double, instant: Boolean = false) {
        val view = viewMatrix()
        val xColumn = Vector3(view.c0r0, view.c1r0, view.c2r0) * x
        val yColumn = Vector3(view.c0r1, view.c1r1, view.c2r1) * y
        val zColumn = Vector3(view.c0r2, view.c1r2, view.c2r2) * z
        lookAtEnd += xColumn + yColumn + zColumn
        if (instant) lookAt = lookAtEnd
        dirty = true
    }

    fun panTo(target: Vector3, instant: Boolean = false) {
        lookAtEnd = target
        if (instant) lookAt = lookAtEnd
        dirty = true
    }

    fun zoom(degrees: Double, instant: Boolean = false) {
        fovEnd += degrees
        if (instant) fov = fovEnd
        dirty = true
    }

    fun zoomTo(degrees: Double, instant: Boolean = false) {
        fovEnd = degrees
        if (instant) fov = fovEnd
        dirty = true
    }

    fun perspectiveTo(value: Double, instant: Boolean = false) {
        perspectiveEnd = value
        if (instant) perspective = perspectiveEnd
        dirty = true
    }

    fun offsetTo(target: Vector2, instant: Boolean = false) {
        offsetEnd = target
        if (instant) offset = offsetEnd
        dirty = true
    }

    private fun update(timeDelta: Double) {
        if (!dirty) return
        dirty = false
        updateStep(timeDelta)
    }

    private fun updateStep(timeDelta: Double) {
        val sphericalDelta = sphericalEnd - spherical
        val lookAtDelta = lookAtEnd - lookAt
        val fovDelta = fovEnd - fov
        val perspectiveDelta = perspectiveEnd - perspective
        val offsetDelta = offsetEnd - offset
        if (abs(sphericalDelta.radius) > EPSILON || abs(sphericalDelta.theta) > EPSILON ||
            abs(sphericalDelta.phi) > EPSILON || abs(lookAtDelta.x) > EPSILON ||
            abs(lookAtDelta.y) > EPSILON || abs(lookAtDelta.z) > EPSILON || abs(fovDelta) > EPSILON ||
            abs(perspectiveDelta) > EPSILON || abs(offsetDelta.x) > EPSILON || abs(offsetDelta.y) > EPSILON
        ) {
            // Exact exponential decay -- `value = target + (value - target) * exp(-rate * dt)`,
            // applied once with the real frame delta -- instead of OrbitalCamera's fixed 1/60s
            // substep loop. Repeatedly lerping by a fixed fraction each frame is framerate
            // *dependent*: more frames per second means more compounding lerps per second of
            // wall-clock time, so the camera visibly settles faster at 240fps than at 60fps.
            // The exponential form doesn't have that problem -- it's exact for any dt, so a
            // single step per frame already gives framerate-independent motion.
            val factor = when {
                dampingFactor <= 0.0 -> 1.0
                dampingFactor >= 1.0 -> 1.0
                else -> {
                    // `dampingFactor` keeps its old meaning: the fraction of the remaining
                    // distance closed in one 1/60s frame. Solve for the continuous decay rate
                    // that matches it at dt = 1/60, then apply that rate over the actual dt.
                    val decayRate = -60.0 * ln(1.0 - dampingFactor)
                    1.0 - exp(-decayRate * timeDelta)
                }
            }
            fov += fovDelta * factor
            perspective += perspectiveDelta * factor
            offset += offsetDelta * factor
            spherical = (spherical + sphericalDelta * factor).makeSafe()
            lookAt += lookAtDelta * factor
            dirty = true
        } else {
            spherical = sphericalEnd.copy()
            lookAt = lookAtEnd.copy()
            fov = fovEnd
            perspective = perspectiveEnd
            offset = offsetEnd
        }
    }

    private fun viewMatrix() = lookAt_(Vector3.fromSpherical(spherical) + lookAt, lookAt, Vector3.UNIT_Y)

    // ---- Extension ----

    override fun setup(program: Program) {
        this.program = program
        program.mouse.moved.listen { onMouseMoved(it) }
        program.mouse.buttonDown.listen { onMouseButtonDown(it) }
        program.mouse.buttonUp.listen { mouseState = MouseState.NONE }
        program.mouse.scrolled.listen { onMouseScrolled(it) }
        program.keyboard.keyDown.listen { onKeyPressed(it) }
        program.keyboard.keyRepeat.listen { onKeyPressed(it) }
    }

    override fun beforeDraw(drawer: Drawer, program: Program) {
        drawer.pushTransforms()

        if (lastSeconds == -1.0) lastSeconds = program.seconds
        val delta = program.seconds - lastSeconds
        lastSeconds = program.seconds
        update(delta)

        drawer.view = viewMatrix()

        // The viewport rectangle is measured at the orbit radius (the eye-to-lookAt distance),
        // not at `near` -- for a pure perspective projection (perspective = 1.0) the choice of
        // reference distance is a wash, since generalizedProjection's perspective divide
        // rescales by the object's *actual* distance regardless of where the rectangle was
        // measured (the usual similar-triangles invariance of a frustum). But there's no such
        // rescaling once `perspective` drops below 1.0 -- an orthographic (perspective = 0.0)
        // projection uses the rectangle's size directly as the visible width of the whole
        // scene, at every depth. Measuring it at `near` (a fraction of a unit from the eye)
        // would make that width microscopic; measuring it at the radius keeps it in scale with
        // whatever's actually in view, and doubles as `dolly`'s effect on the ortho size.
        val aspect = drawer.width.toDouble() / drawer.height
        val distance = spherical.radius
        val viewportHeight = 2.0 * distance * tan(fov.asRadians / 2.0)
        val viewportWidth = viewportHeight * aspect

        // A perspective camera needs `near` to stay close to the eye only to avoid clipping
        // things that are actually there -- but with no perspective divide left to speak of at
        // low `perspective`, there's also no reason left to keep the near plane out that far.
        // Unlike shrinking `near` in perspective mode -- where depth precision concentrates
        // near the eye and a smaller `near` starves precision at `distance`, worsening
        // z-fighting -- generalizedProjection's orthographic mapping is linear across
        // near..far, so tapering `near` towards 0 as `perspective` drops towards 0.0 is close
        // to free precision-wise, and removes the near clip plane as a source of surprise
        // clipping right where an orthographic view has the least reason to expect it.
        val effectiveNear = near * perspective.coerceIn(0.0, 1.0)

        drawer.projection = generalizedProjection(
            eye = Vector3.ZERO,
            viewportCenter = Vector3(offset.x, offset.y, -distance),
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
            near = -effectiveNear,
            far = -far,
            perspective = perspective
        )

        if (depthTest) {
            drawer.drawStyle.depthWrite = true
            drawer.drawStyle.depthTestPass = DepthTestPass.LESS_OR_EQUAL
        }
    }

    override fun afterDraw(drawer: Drawer, program: Program) {
        drawer.popTransforms()
    }

    // ---- controls ----

    private fun onMouseScrolled(event: MouseEvent) {
        if (!userInteraction || event.propagationCancelled) return
        val rot = event.rotation
        if (abs(rot.x) > abs(rot.y)) return
        when {
            rot.y > 0 -> dollyIn(rot.y)
            rot.y < 0 -> dollyOut(rot.y)
        }
    }

    private fun onMouseMoved(event: MouseEvent) {
        if (!userInteraction || event.propagationCancelled) return
        if (mouseState == MouseState.NONE) return
        val delta = lastMousePosition - event.position
        lastMousePosition = event.position

        if (mouseState == MouseState.PAN) {
            val offset = Vector3.fromSpherical(spherical) - lookAt
            // half of the fov is center to top of screen
            val targetDistance = offset.length * tan(fov.asRadians / 2)
            val panX = 2 * delta.x * targetDistance / program.width
            val panY = 2 * delta.y * targetDistance / program.height
            pan(panX, -panY, 0.0)
        } else {
            val rotX = 360.0 * delta.x / program.width
            val rotY = 360.0 * delta.y / program.height
            rotate(rotX, rotY)
        }
    }

    private fun onMouseButtonDown(event: MouseEvent) {
        if (!userInteraction || event.propagationCancelled) return
        val previousState = mouseState
        mouseState = when (event.button) {
            MouseButton.LEFT -> MouseState.ROTATE
            MouseButton.RIGHT -> MouseState.PAN
            else -> mouseState
        }
        if (previousState == MouseState.NONE) lastMousePosition = event.position
    }

    private fun onKeyPressed(keyEvent: KeyEvent) {
        if (!userInteraction || keyEvent.propagationCancelled) return
        when (keyEvent.key) {
            KEY_ARROW_RIGHT -> pan(keySpeed, 0.0, 0.0)
            KEY_ARROW_LEFT -> pan(-keySpeed, 0.0, 0.0)
            KEY_ARROW_UP -> pan(0.0, keySpeed, 0.0)
            KEY_ARROW_DOWN -> pan(0.0, -keySpeed, 0.0)
            KEY_PAGE_UP -> zoom(keySpeed)
            KEY_PAGE_DOWN -> zoom(-keySpeed)
        }
        when (keyEvent.name) {
            "q" -> pan(0.0, -keySpeed, 0.0)
            "e" -> pan(0.0, keySpeed, 0.0)
            "w" -> pan(0.0, 0.0, -keySpeed)
            "s" -> pan(0.0, 0.0, keySpeed)
            "a" -> pan(-keySpeed, 0.0, 0.0)
            "d" -> pan(keySpeed, 0.0, 0.0)
        }
    }

    companion object {
        private const val EPSILON = 0.000001
    }
}
