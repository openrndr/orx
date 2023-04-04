import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.dnk3.*
import org.openrndr.extra.dnk3.gltf.buildSceneNodes
import org.openrndr.extra.dnk3.gltf.loadGltfFromFile
import org.openrndr.extra.dnk3.renderers.dryRenderer
import org.openrndr.math.*
import java.io.File

fun main() = application {
    configure {
        width = 1280
        height = 720
    }

    program {
        val gltf = loadGltfFromFile(File("demo-data/gltf-models/camera/Scene.glb"))
        val scene = Scene(SceneNode())

        scene.root.entities.add(HemisphereLight().apply {
            upColor = ColorRGBa(0.1, 0.1, 0.4)
            downColor = ColorRGBa(0.1, 0.0, 0.0)
        })

        val sceneData = gltf.buildSceneNodes()
        scene.root.children.addAll(sceneData.scenes.first())

        // -- create a renderer
        val renderer = dryRenderer()

        val cameras = scene.root.findContent { this as? PerspectiveCamera }

        extend {
            sceneData.animations[0].applyToTargets(seconds.mod_(sceneData.animations[0].duration))
            drawer.view = cameras[0].content.viewMatrix
            drawer.projection = cameras[0].content.projectionMatrix
            drawer.clear(ColorRGBa.PINK)
            renderer.draw(drawer, scene)
        }
    }
}