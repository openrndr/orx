import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.dnk3.*
import org.openrndr.extra.dnk3.gltf.buildSceneNodes
import org.openrndr.extra.dnk3.gltf.loadGltfFromFile
import org.openrndr.extra.dnk3.renderers.dryRenderer
import org.openrndr.extra.camera.Orbital
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.transform
import java.io.File

fun main() = application {
    configure {
        width = 1280
        height = 720
        //multisample = WindowMultisample.SampleCount(8)
    }

    program {
        if (System.getProperty("takeScreenshot") == "true") {
            extend(SingleScreenshot()) {
                this.outputFile = System.getProperty("screenshotPath")
            }
        }

        val gltf = loadGltfFromFile(File("demo-data/gltf-models/suzanne/Suzanne.gltf"))
        val scene = Scene(SceneNode())

        // -- add some lights
        val lightNode = SceneNode()
        lightNode.transform = transform {
            translate(0.0, 10.0, 0.0)
            rotate(Vector3.UNIT_X, -65.0)
        }
        lightNode.entities.add(DirectionalLight())
        scene.root.entities.add(HemisphereLight().apply {
            upColor = ColorRGBa.BLUE.shade(0.4)
            downColor = ColorRGBa.GRAY.shade(0.1)
            })
        scene.root.children.add(lightNode)
        scene.root.children.addAll(gltf.buildSceneNodes().scenes.first())

        // -- create a renderer
        val renderer = dryRenderer()
        extend(Orbital()) {
            far = 50.0
            eye = Vector3(1.5, 0.0, 3.0)
            fov = 40.0
        }
        extend {
            drawer.clear(ColorRGBa.PINK)
            renderer.draw(drawer, scene)
        }
    }
}