import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.loadImage
import org.openrndr.draw.renderTarget
import org.openrndr.extra.runway.*

/**
 * This demonstrates the body estimation model of PoseNet
 * This example requires a `runway/PoseNet` model active in Runway.
 */
suspend fun main() = application {
    configure {
        width = 512
        height = 512
    }

    program {
        val rt = renderTarget(512, 512) {
            colorBuffer()
        }
        val image = loadImage("demo-data/images/peopleCity01.jpg")

        drawer.isolatedWithTarget(rt) {
            drawer.ortho(rt)
            drawer.clear(ColorRGBa.BLACK)
            drawer.image(image, (rt.width - image.width) / 2.0, (rt.height - image.height) / 2.0)
        }

        extend {
            val result: PoseNetResponse = runwayQuery("http://localhost:8000/query", PoseNetRequest(rt.colorBuffer(0).toData()))
            val poses = result.poses
            val scores = result.scores

            drawer.image(image, 0.0, 0.0, 512.0, 512.0)

            poses.forEach { poses ->
                poses.forEach { pose ->
                        drawer.circle(pose[0]*512.0, pose[1]*512.0, 10.0)
                }
            }
        }
    }
}