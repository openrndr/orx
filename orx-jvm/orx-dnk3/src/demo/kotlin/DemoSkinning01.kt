import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.dnk3.HemisphereLight
import org.openrndr.extra.dnk3.Scene
import org.openrndr.extra.dnk3.SceneNode
import org.openrndr.extra.dnk3.gltf.buildSceneNodes
import org.openrndr.extra.dnk3.gltf.loadGltfFromFile
import org.openrndr.extra.dnk3.renderers.dryRenderer
import org.openrndr.math.Vector3
import java.io.File

fun main() = application {
    configure {
        width = 1280
        height = 720
        //multisample = WindowMultisample.SampleCount(8)
    }

    program {
        val gltf = loadGltfFromFile(File("demo-data/gltf-models/fox/Fox.glb"))
        val scene = Scene(SceneNode())

        scene.root.entities.add(HemisphereLight().apply {
            upColor = ColorRGBa.WHITE.shade(0.4)
            downColor = ColorRGBa.GRAY.shade(0.1)
        })
        val sceneData = gltf.buildSceneNodes()
        scene.root.children.addAll(sceneData.scenes.first())


        // -- create a renderer
        val renderer = dryRenderer()
        extend(Orbital()) {
            far = 500.0
            lookAt = Vector3(0.0, 40.0, 0.0)
            eye = Vector3(150.0, 40.0, 200.0)
            fov = 40.0
        }

        extend {
            sceneData.animations[2].applyToTargets(seconds.mod(sceneData.animations[2].duration))
            drawer.clear(ColorRGBa.PINK)
            renderer.draw(drawer, scene)
        }
    }
}