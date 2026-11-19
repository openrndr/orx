import org.openrndr.application
import org.openrndr.extra.mediapipe.MediaPipeModels
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.draw.loadImage
import org.openrndr.extra.mediapipe.FaceStylizer
import org.openrndr.extra.mediapipe.StylizedFace
import org.openrndr.extra.mediapipe.toColorBuffer
import org.openrndr.extra.mediapipe.toRgbaImage

/**
 * Stylizes the face in an image of MediaPipe as a color sketch, in color ink and as an oil painting, and draws the
 * image, the three stylized faces, and the image with the sketch in place of the face.
 * The models and images are downloaded from [orx-models](https://github.com/openrndr/orx-models) when first used.
 */
fun main() = application {
    configure {
        // the image and the image with the sketch, 256 pixels wide, and the stylized faces
        width = 256 * 2 + 320 * 3
        height = 320
    }
    program {
        val image = loadImage(MediaPipeModels.image("portrait.jpg"))
        val pixels = image.toRgbaImage()
        val faces = listOf("color_sketch", "color_ink", "oil_painting").map { style ->
            FaceStylizer(MediaPipeModels.model("face_stylizer_$style.onnx")).use { it.stylize(pixels)!! }
        }
        val stylized = faces.map { it.image.toColorBuffer() }
        extend {
            val side = height.toDouble()
            drawer.image(image, 0.0, 0.0, side * image.width / image.height, side)
            for ((i, face) in stylized.withIndex()) {
                drawer.image(face, 256.0 + i * side, 0.0, side, side)
            }
            // the sketch in the square around the face
            drawer.isolated {
                drawer.translate(256.0 + 3 * side, 0.0)
                drawer.scale(side / image.height)
                drawer.image(image)
                drawStylizedFace(faces[0], stylized[0])
            }
        }
    }
}

/** draws the stylized face in the square around the face that it was cut from */
fun Drawer.drawStylizedFace(face: StylizedFace, image: ColorBuffer) {
    isolated {
        translate(face.center)
        rotate(Math.toDegrees(face.rotation))
        image(image, -face.size / 2.0, -face.size / 2.0, face.size, face.size)
    }
}
