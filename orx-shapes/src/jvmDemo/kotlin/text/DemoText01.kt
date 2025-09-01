package text

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.font.loadFace
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.shapes.bounds.bounds
import org.openrndr.extra.shapes.text.shapesFromText

fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val face =
            loadFace("https://github.com/IBM/plex/raw/master/packages/plex-mono/fonts/complete/otf/IBMPlexMono-Bold.otf")
        val shapes = shapesFromText(face, "SUCH\nVECTOR\nSUCH\nTEXT", 150.0)

        val bounds = shapes.bounds
        extend(Camera2D())
        extend {
            drawer.clear(ColorRGBa.PINK)
            drawer.translate(-bounds.corner)
            drawer.translate((width - bounds.width) / 2.0, (height - bounds.height) / 2.0)
            drawer.shapes(shapes)
        }
    }
}
