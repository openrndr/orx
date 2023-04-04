import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.dnk3.*

import org.openrndr.extra.dnk3.renderers.dryRenderer
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.meshgenerators.sphereMesh
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.transform

fun main() = application {
    configure {
        width = 1280
        height = 720
        //multisample = WindowMultisample.SampleCount(8)
    }

    program {

        val root = SceneNode()
        val scene = Scene(root)

        val lightNode = SceneNode()
        lightNode.transform = transform {
            translate(0.0, 10.0, 0.0)
        }
        lightNode.entities.add(PointLight())
        lightNode.entities.add(HemisphereLight(upColor = ColorRGBa.PINK, downColor = ColorRGBa(0.1,0.1,0.1)))
        scene.root.children.add(lightNode)

        val meshNode = SceneNode()
        val box = sphereMesh(32, 32)
        val geometry = Geometry(listOf(box), null, DrawPrimitive.TRIANGLES, 0, box.vertexCount)
        val material = PBRMaterial()
        val primitive = MeshPrimitive(geometry, material)
        val mesh = Mesh(listOf(primitive))
        meshNode.entities.add(mesh)
        root.children.add(meshNode)

        // -- create a renderer
        val renderer = dryRenderer()
        extend(Orbital()) {
            far = 500.0
            lookAt = Vector3(0.0, 0.0, 0.0)
            eye = Vector3(3.0, 2.0, -3.0)
            fov = 30.0
        }
        extend {
            drawer.clear(ColorRGBa.PINK)
            renderer.draw(drawer, scene)
        }
    }
}