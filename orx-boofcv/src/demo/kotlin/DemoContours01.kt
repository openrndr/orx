import boofcv.alg.filter.binary.BinaryImageOps
import boofcv.alg.filter.binary.GThresholdImageOps
import boofcv.alg.filter.binary.ThresholdImageOps
import boofcv.struct.ConnectRule
import boofcv.struct.image.GrayU8
import org.openrndr.application
import org.openrndr.boofcv.binding.toGrayF32
import org.openrndr.boofcv.binding.toShapeContours
import org.openrndr.color.ColorRGBa
import org.openrndr.color.rgb
import org.openrndr.draw.loadImage
import org.openrndr.extensions.SingleScreenshot
import kotlin.math.cos
import kotlin.math.sin

fun main() {
    application {
        program {

            // -- this block is for automation purposes only
            if (System.getProperty("takeScreenshot") == "true") {
                extend(SingleScreenshot()) {
                    this.outputFile = System.getProperty("screenshotPath")
                }
            }

            // Load an image, convert to BoofCV format using orx-boofcv
            val input = loadImage("demo-data/images/image-001.png").toGrayF32()

            // BoofCV: calculate a good threshold for the loaded image
            val threshold = GThresholdImageOps.computeOtsu(input, 0.0, 255.0)

            // BoofCV: use the threshold to convert the image to black and white
            val binary = GrayU8(input.width, input.height)
            ThresholdImageOps.threshold(input, binary, threshold.toFloat(), false)

            // BoofCV: Contract and expand the white areas to remove noise
            var filtered = BinaryImageOps.erode8(binary, 1, null)
            filtered = BinaryImageOps.dilate8(filtered, 1, null)

            // BoofCV: Calculate contours as vector data
            val contours = BinaryImageOps.contour(filtered, ConnectRule.EIGHT, null)

            // orx-boofcv: convert vector data to OPENRNDR ShapeContours
            val externalShapes = contours.toShapeContours(true,
                    internal = false, external = true)
            val internalShapes = contours.toShapeContours(true,
                    internal = true, external = false)

            extend {
                drawer.run {
                    // Zoom in and out over time
                    translate(bounds.center)
                    scale(1.5 + 0.5 * cos(seconds * 0.2))
                    translate(-bounds.center)

                    stroke = null

                    // Draw all external shapes
                    fill = rgb(0.2)
                    contours(externalShapes)

                    // Draw internal shapes one by one to set unique colors
                    internalShapes.forEachIndexed { i, shp ->
                        val shade = 0.2 + (i % 7) * 0.1 +
                                0.1 * sin(i + seconds)
                        fill = ColorRGBa.PINK.shade(shade)
                        contour(shp)
                    }
                }
            }
        }
    }
}