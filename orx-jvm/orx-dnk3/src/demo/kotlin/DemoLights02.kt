import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.dnk3.HemisphereLight
import org.openrndr.extra.dnk3.Scene
import org.openrndr.extra.dnk3.SceneNode
import org.openrndr.extra.dnk3.gltf.buildSceneNodes
import org.openrndr.extra.dnk3.gltf.loadGltfFromFile
import org.openrndr.extra.dnk3.renderers.dryRenderer
import org.openrndr.math.Spherical
import org.openrndr.math.Vector3
import java.io.File

fun main() = application {
    configure {
        width = 1280
        height = 720
        //multisample = WindowMultisample.SampleCount(8)
    }

    program {
        val gltf = loadGltfFromFile(File("demo-data/gltf-models/spot-light/Scene.glb"))
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
            far = 50.0
            camera.setView(Vector3(-0.514, -0.936, -1.122), Spherical(454.346, 25.0, 8.444), 40.0)
        }
        extend {
            sceneData.animations[0].applyToTargets(seconds.mod(sceneData.animations[0].duration))
            drawer.clear(ColorRGBa.PINK)
            renderer.draw(drawer, scene)
        }
    }
}