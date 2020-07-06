import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.dnk3.*
import org.openrndr.extra.dnk3.gltf.buildSceneNodes
import org.openrndr.extra.dnk3.gltf.loadGltfFromFile
import org.openrndr.extra.dnk3.renderers.dryRenderer
import org.openrndr.extras.camera.Orbital
import org.openrndr.math.*
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

        val gltf = loadGltfFromFile(File("demo-data/gltf-models/directional-light/Scene.glb"))
        val scene = Scene(SceneNode())

        scene.root.entities.add(HemisphereLight().apply {
            upColor = ColorRGBa(0.1, 0.1, 0.4)
            downColor = ColorRGBa(0.1, 0.0, 0.0)
        })

        val sceneData = gltf.buildSceneNodes()
        scene.root.children.addAll(sceneData.scenes.first())

        // -- create a renderer
        val renderer = dryRenderer()
        val orb = extend(Orbital()) {
            camera.setView(Vector3(-0.49, -0.24, 0.20), Spherical(26.56, 90.0, 6.533), 40.0)
        }

        extend {
            sceneData.animations[0].applyToTargets(seconds.mod_(sceneData.animations[0].duration))
            drawer.clear(ColorRGBa.PINK)
            renderer.draw(drawer, scene)
        }
    }
}