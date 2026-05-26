import boofcv.abst.feature.detect.interest.ConfigPointDetector
import boofcv.abst.feature.detect.interest.PointDetectorTypes
import boofcv.abst.tracker.PointTrackerKltPyramid
import boofcv.alg.filter.derivative.DerivativeType
import boofcv.alg.filter.derivative.GImageDerivativeOps
import boofcv.alg.tracker.klt.ConfigPKlt
import boofcv.factory.tracker.FactoryPointTracker
import boofcv.struct.image.GrayF32
import boofcv.struct.image.ImageGray
import boofcv.struct.pyramid.ConfigDiscreteLevels
import org.openrndr.application
import org.openrndr.boofcv.binding.toGrayF32
import org.openrndr.boofcv.binding.toVector2
import org.openrndr.color.ColorHSVa
import org.openrndr.ffmpeg.VideoPlayerFFMPEG
import kotlin.system.exitProcess

/**
 * A simple way to create a Kanade-Lucas-Tomasi (KLT) tracker.
 */
fun createKLT(): PointTrackerKltPyramid<GrayF32?, out ImageGray<*>?>? {
    val configKlt = ConfigPKlt().also {
        it.templateRadius = 3
        it.pyramidLevels = ConfigDiscreteLevels.levels(4)
    }

    val configDetector = ConfigPointDetector().also {
        it.type = PointDetectorTypes.SHI_TOMASI
        it.general.maxFeatures = 600
        it.general.radius = 6
        it.general.threshold = 1f
    }

    val imageType = GrayF32::class.java
    val derivType = GImageDerivativeOps.getDerivativeType(imageType)

    return FactoryPointTracker.klt(
        configKlt,
        DerivativeType.PREWITT,
        configDetector,
        imageType,
        derivType
    )
}

/**
 * Video point-tracking demo based on https://boofcv.org/index.php?title=Example_Track_Point_Features
 *
 * The KLT parameters are highly configurable. The current approach uses a grayscale input for tracking.
 *
 * This demo uses the first available webcam and works best for tracking points based on camera movement.
 *
 */
fun main() = application {
    // skip this demo on CI
    if (System.getProperty("takeScreenshot") == "true") {
        exitProcess(0)
    }
    program {
        val webcam = VideoPlayerFFMPEG.fromDevice(null, imageWidth = 640, imageHeight = 480)
        webcam.play()

        val tracker = createKLT()
        val target = GrayF32(webcam.width, webcam.height)

        extend {
            tracker!!.process(webcam.colorBuffer?.toGrayF32(target))

            webcam.draw(drawer)

            val tracks = tracker.getActiveTracks(null)

            drawer.stroke = null

            tracks.forEach {
                drawer.fill = ColorHSVa((it.featureId * 30.0).mod(360.0), 0.8, 0.8).toRGBa()
                drawer.circle(it.pixel.toVector2(), 5.0)
            }

            // As tracked points are lost (when they move out of the window or are occluded by
            // something) and their number goes below 150, find new points to track.
            if (tracker.totalActive < 150) tracker.spawnTracks()
        }
    }
}