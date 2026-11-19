import org.openrndr.MouseButton
import org.openrndr.MouseEvent
import org.openrndr.MouseEventType
import org.openrndr.PinchEvent
import org.openrndr.ProgramImplementation
import org.openrndr.ScrollSource
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.camera.GeneralizedOrbital
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.camera.ProjectionType
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** cameras zoom on the pinches of a touchpad and ignore its scrolls, a wheel keeps zooming */
class TestPinchZoom {
    private val program = ProgramImplementation()

    private fun pinch(scale: Double, position: Vector2 = Vector2.ZERO, cancelled: Boolean = false) {
        val event = PinchEvent(scale, position)
        if (cancelled) event.cancelPropagation()
        program.gestures.pinchUpdated.trigger(event)
        program.gestures.pinchUpdated.deliver()
    }

    private fun scroll(y: Double, source: ScrollSource, position: Vector2 = Vector2.ZERO) {
        program.mouse.scrolled.trigger(MouseEvent(position, Vector2(0.0, y), Vector2.ZERO, MouseEventType.SCROLLED, MouseButton.NONE, emptySet(), source))
        program.mouse.scrolled.deliver()
    }

    @Test
    fun `Camera2D zooms around the pinch`() {
        val camera = Camera2D()
        camera.setup(program)
        pinch(2.0, Vector2(100.0, 50.0))
        // the point of the pinch stays where it is
        assertEquals(Vector2(100.0, 50.0), (camera.view * Vector2(100.0, 50.0).xy01).xy)
        assertEquals(Vector2(-100.0, -50.0), (camera.view * Vector2.ZERO.xy01).xy)
        pinch(0.5, Vector2(100.0, 50.0))
        assertEquals(Matrix44.IDENTITY, camera.view)

        scroll(1.0, ScrollSource.TOUCHPAD)
        assertEquals(Matrix44.IDENTITY, camera.view)
        for (source in listOf(ScrollSource.WHEEL, ScrollSource.UNKNOWN)) {
            camera.view = Matrix44.IDENTITY
            scroll(1.0, source)
            assertEquals(0.97, camera.view.c0r0, 1E-9)
        }

        camera.view = Matrix44.IDENTITY
        pinch(2.0, cancelled = true)
        camera.userInteraction = false
        pinch(2.0)
        assertEquals(Matrix44.IDENTITY, camera.view)
    }

    @Test
    fun `Orbital dollies on a pinch, and scales its orthographic view`() {
        val orbital = Orbital().apply { eye = Vector3(0.0, 0.0, 10.0) }
        orbital.setup(program)
        val camera = orbital.camera
        fun settle() = repeat(2000) { camera.update(1 / 60.0) }

        pinch(2.0)
        settle()
        assertEquals(5.0, camera.spherical.radius, 1E-6)
        scroll(1.0, ScrollSource.TOUCHPAD)
        pinch(2.0, cancelled = true)
        settle()
        assertEquals(5.0, camera.spherical.radius, 1E-6)
        scroll(1.0, ScrollSource.WHEEL)
        settle()
        assertTrue(camera.spherical.radius < 5.0)

        camera.projectionType = ProjectionType.ORTHOGONAL
        val magnitude = camera.magnitudeEnd
        pinch(2.0)
        assertEquals(magnitude / 2.0, camera.magnitudeEnd, 1E-9)
        scroll(1.0, ScrollSource.TOUCHPAD)
        assertEquals(magnitude / 2.0, camera.magnitudeEnd, 1E-9)
    }

    @Test
    fun `GeneralizedOrbital dollies on a pinch`() {
        val camera = GeneralizedOrbital(eye = Vector3(0.0, 0.0, 10.0))
        camera.setup(program)
        fun settle() = repeat(2000) { camera.update(1 / 60.0) }

        pinch(0.5)
        settle()
        assertEquals(20.0, camera.spherical.radius, 1E-6)
        scroll(1.0, ScrollSource.TOUCHPAD)
        settle()
        assertEquals(20.0, camera.spherical.radius, 1E-6)
        scroll(1.0, ScrollSource.UNKNOWN)
        settle()
        assertTrue(camera.spherical.radius < 20.0)
    }
}
