import boofcv.alg.filter.binary.BinaryImageOps
import boofcv.alg.filter.binary.GThresholdImageOps
import boofcv.alg.filter.binary.ThresholdImageOps
import boofcv.struct.ConnectRule
import boofcv.struct.image.GrayU8
import org.openrndr.application
import org.openrndr.boofcv.binding.toGrayF32
import org.openrndr.boofcv.binding.toShapeContours
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.math.CatmullRomChain2
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.simplify
import org.openrndr.shape.toContour

fun main() {
    application {
        program {
            // Create a buffer where to draw something for boofcv
            val rt = renderTarget(width, height) {
                colorBuffer()
                depthBuffer()
            }
            // Draw some shapes on that buffer
            drawer.isolatedWithTarget(rt) {
                clear(ColorRGBa.BLACK)
                fill = ColorRGBa.WHITE
                stroke = null
                rectangle(Rectangle.fromCenter(bounds.position(0.33, 0.5),
                        150.0, 150.0))
                translate(bounds.position(0.62, 0.5))
                rotate(30.0)
                rectangle(Rectangle.fromCenter(Vector2.ZERO, 200.0, 200.0))
                rectangle(0.0, -200.0, 60.0, 60.0)
                circle(0.0, 190.0, 60.0)
            }
            // Convert the bitmap buffer into ShapeContours
            val vectorized = imageToContours(rt.colorBuffer(0))

            // Show amount of segments in each shape (high number)
            vectorized.forEachIndexed { i, it ->
                println("boofcv shape $i: ${it.segments.size} segments")
            }

            // Make a simplified list of points
            val simplePoints = vectorized.map {
                simplify(it.adaptivePositions(), 4.0)
            }.filter { it.size >= 3 }

            // Use the simplified list to make a smooth contour
            val smooth = simplePoints.map {
                CatmullRomChain2(it, 0.0, true).toContour()
            }

            // Use the simplified list to make a polygonal contour
            val polygonal = simplePoints.map {
                ShapeContour.fromPoints(it, true)
            }

            // Show amount of segments in simplified shapes (low number).
            // Note: `smooth` and `polygonal` have the same number of segments
            smooth.forEachIndexed { i, it ->
                println("simplified shape $i: ${it.segments.size} segments")
            }

            extend {
                drawer.run {
                    fill = null // ColorRGBa.PINK.opacify(0.15)
                    stroke = ColorRGBa.PINK.opacify(0.7)
                    contours(polygonal)
                    contours(smooth)
                }
            }
        }
    }
}

fun imageToContours(input: ColorBuffer): List<ShapeContour> {
    val bitmap = input.toGrayF32()
    // BoofCV: calculate a good threshold for the loaded image
    val threshold = GThresholdImageOps.computeOtsu(bitmap, 0.0, 255.0)

    // BoofCV: use the threshold to convert the image to black and white
    val binary = GrayU8(bitmap.width, bitmap.height)
    ThresholdImageOps.threshold(bitmap, binary, threshold.toFloat(), false)

    // BoofCV: Contract and expand the white areas to remove noise
    var filtered = BinaryImageOps.erode8(binary, 1, null)
    filtered = BinaryImageOps.dilate8(filtered, 1, null)

    // BoofCV: Calculate contours as vector data
    val contours = BinaryImageOps.contour(filtered, ConnectRule.EIGHT, null)

    // orx-boofcv: convert vector data to OPENRNDR ShapeContours
    return contours.toShapeContours(true, internal = true, external = true)
}
