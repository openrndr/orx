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

/**
 * Demonstrates how to load a .glb file containing an animated scene.
 * The scene contains a floor, a cube, and an animated light.
 * When rendered, the light casts the shadow of the cube onto the floor.
 *
 * The scene contains a list of animations, which need to be updated using the `.applyToTargets()` method,
 * otherwise the time in the animation is still. The method expects a time in seconds. In this demo,
 * we pass a time that loops based on the duration of the animation. Note that it would be easy to
 * pass a different time, slower or faster than real time, play it backwards or even travel back and forth
 * in time.
 *
 * An interactive orbital camera is enabled, letting you use the mouse to control the camera position,
 * direction, and zoom.
 */
fun main() = application {
    configure {
        width = 1280
        height = 720
        //multisample = WindowMultisample.SampleCount(8)
    }

    program {
        val gltf = loadGltfFromFile(File("demo-data/gltf-models/spot-light/Scene.glb"))
        val scene = Scene(SceneNode())

        // Add a light to tint the objects in the scene
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