import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.font.loadFace
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.shapes.splines.catmullRom
import org.openrndr.extra.shapes.splines.toPath3D
import org.openrndr.extra.textoncontour.layoutTextOnPath3D
import org.openrndr.extra.textwriter.ContourOverflow
import org.openrndr.extra.textwriter.SlugTextDrawer
import org.openrndr.extra.textwriter.TextSpan
import org.openrndr.extra.textwriter.TextStyle
import org.openrndr.math.Vector3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Demonstrates [layoutTextOnPath3D]: text along a 3D path, here a helix, the 3D counterpart of text on a contour.
 *
 * Every glyph gets a transform that places its outline in the frame of the path where it stands, the frames of
 * PosePath3D, which start with their y-axis up and turn as little as possible, so the text runs along the helix
 * without twisting. The text is drawn with a [SlugTextDrawer] from the glyphs of the layout, and slides along the
 * path over time. Drag with the mouse to orbit around.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val face = loadFace("demo-data/fonts/IBMPlexMono-Regular.ttf").withSize(1.0)
        // the helix winds so that the faces of the glyphs, which look along up × the direction of the path, look out
        val helix = List(49) { i -> Vector3(cos(-i * PI / 8) * 6.0, i * 0.25 - 6.0, sin(-i * PI / 8) * 6.0) }
            .catmullRom(0.5, false).toPath3D()
        val text = "Text along a 3D path winds around the helix without twisting. "
        val style = TextStyle.Defaults.copy(face = face, sizeInEm = 1.0, fill = ColorRGBa.fromHex("#5ab0ff"))

        extend(Orbital()) {
            eye = Vector3(0.0, 4.0, 24.0)
            fov = 50.0
        }
        extend {
            drawer.clear(ColorRGBa.fromHex("#16181c"))
            drawer.stroke = ColorRGBa.WHITE.opacify(0.25)
            drawer.strokeWeight = 1.0
            drawer.path(helix)

            val layout = layoutTextOnPath3D(
                listOf(TextSpan(text.repeat(3), null)), helix, up = Vector3.UNIT_Y, offset = 0.15,
                style = style, startDistance = -(seconds * 2.0) % (text.length * 0.6),
                // glyphs beyond the ends of the open helix are not placed
                overflow = ContourOverflow.HIDE
            )
            val slug = SlugTextDrawer()
            slug.addGlyphs(layout)
            slug.draw(drawer)
        }
    }
}
