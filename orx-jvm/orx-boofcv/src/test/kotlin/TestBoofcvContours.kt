import boofcv.alg.filter.binary.BinaryImageOps
import boofcv.alg.filter.binary.GThresholdImageOps
import boofcv.alg.filter.binary.ThresholdImageOps
import boofcv.io.image.UtilImageIO
import boofcv.struct.ConnectRule
import boofcv.struct.image.GrayF32
import boofcv.struct.image.GrayU8
import org.openrndr.application
import org.openrndr.boofcv.binding.toShapeContours
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TestBoofcvContours {
    @Test
    fun `load an image and find contours`() {
        application {
            program {
                extend {
                    val imgSrc = "../../demo-data/images/image-001.png"
                    val input = UtilImageIO.loadImage(imgSrc, GrayF32::class.java)

                    assertNotNull(input, "Test image $imgSrc not found")

                    val threshold = GThresholdImageOps.computeOtsu(input, 0.0, 255.0)
                    val binary = GrayU8(input.width, input.height)
                    ThresholdImageOps.threshold(input, binary, threshold.toFloat(), false)

                    var filtered = BinaryImageOps.erode8(binary, 1, null)
                    filtered = BinaryImageOps.dilate8(filtered, 1, null)

                    val boofContours = BinaryImageOps.contour(filtered, ConnectRule.EIGHT, null)
                    val contours = boofContours.toShapeContours(true)

                    assertTrue(
                        abs(contours.size - 292) < 10,
                        "~292 contours expected in $imgSrc, found ${contours.size}"
                    )

                    application.exit()
                }
            }
        }
    }
}