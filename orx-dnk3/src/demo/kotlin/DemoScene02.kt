import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.dnk3.*

import org.openrndr.extra.dnk3.gltf.buildSceneNodes
import org.openrndr.extra.dnk3.gltf.loadGltfFromFile
import org.openrndr.extra.dnk3.gltf.loadGltfFromGlbFile
import org.openrndr.extras.camera.Orbital
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

        val gltf = loadGltfFromFile(File("demo-data/gltf-models/complex02/scene.gltf"))
//        val gltf = loadGltfFromGlbFile(File("demo-data/gltf-models/splash-sss.glb"))
        val scene = Scene(SceneNode())

        // -- add some lights
        val lightNode = SceneNode()
        lightNode.transform = transform {
            translate(0.0, 10.0, 0.0)
            rotate(Vector3.UNIT_X, -90.0)
        }
        lightNode.entities.add(DirectionalLight())
        scene.root.entities.add(HemisphereLight().apply {
            upColor = ColorRGBa.WHITE.shade(1.0)
            downColor = ColorRGBa.WHITE.shade(0.1)
            })
        scene.root.children.add(lightNode)
        scene.root.children.addAll(gltf.buildSceneNodes().scenes.first())


        // -- create a renderer
        val renderer = dryRenderer()
        extend(Orbital()) {
            far = 500.0
            lookAt = Vector3(0.0, 0.7, 0.0)
            eye = Vector3(3.0, 0.7, -2.0)
            fov = 30.0
        }
        extend {
            drawer.clear(ColorRGBa.PINK)
            renderer.draw(drawer, scene)
        }
    }
}