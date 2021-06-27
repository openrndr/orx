import kotlinx.coroutines.yield
import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.BufferMultisample
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.DrawPrimitive
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.dnk3.*
import org.openrndr.extra.dnk3.features.IrradianceSH
import org.openrndr.extra.dnk3.features.addIrradianceSH
import org.openrndr.extra.dnk3.gltf.buildSceneNodes
import org.openrndr.extra.dnk3.gltf.loadGltfFromFile
import org.openrndr.extra.dnk3.post.ScreenspaceReflections
import org.openrndr.extra.dnk3.post.VolumetricIrradiance
import org.openrndr.extra.dnk3.renderers.postRenderer
import org.openrndr.extra.shaderphrases.annotations.ShaderPhrases
import org.openrndr.extras.camera.Orbital
import org.openrndr.extras.meshgenerators.sphereMesh
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.filter.color.Delinearize
import org.openrndr.math.Matrix44
import org.openrndr.math.Spherical
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.scale
import org.openrndr.math.transforms.transform
import org.openrndr.math.transforms.translate
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

suspend fun main() = application {
    configure {
        width = 1280
        height = 720
        multisample = WindowMultisample.SampleCount(8)
    }

    program {

        if (System.getProperty("takeScreenshot") == "true") {
            extend(SingleScreenshot()) {
                this.outputFile = System.getProperty("screenshotPath")
            }
        }

        val gltf = loadGltfFromFile(File("demo-data/gltf-models/irradiance-probes/model.glb"))
        val scene = Scene(SceneNode())

        val probeBox = sphereMesh(16, 16, 0.1)
        val probeGeometry = Geometry(listOf(probeBox), null, DrawPrimitive.TRIANGLES, 0, probeBox.vertexCount)

        val c = 5
        scene.addIrradianceSH(c, c, c, 3.0 / c, cubemapSize = 32, offset = Vector3(0.0, 0.0, 0.0))


        val sceneData = gltf.buildSceneNodes()
        scene.root.children.addAll(sceneData.scenes.first())

        // -- create a renderer
        val renderer = postRenderer()


//        renderer.postSteps.add(
//                FilterPostStep(1.0, ScreenspaceReflections(), listOf("color", "clipDepth", "viewNormal"), "reflections", ColorFormat.RGB, ColorType.FLOAT16) {
//                    val p = Matrix44.scale(drawer.width / 2.0, drawer.height / 2.0, 1.0) * Matrix44.translate(Vector3(1.0, 1.0, 0.0)) * drawer.projection
//                    this.projection = p
//                    this.projectionMatrixInverse = drawer.projection.inversed
//                }
//        )

//        renderer.postSteps.add(
//                FilterPostStep(1.0, VolumetricIrradiance(), listOf("color", "clipDepth"), "volumetric-irradiance", ColorFormat.RGB, ColorType.FLOAT16) {
//                    this.irradianceSH = scene.features[0] as IrradianceSH
//                    this.projectionMatrixInverse = drawer.projection.inversed
//                    this.viewMatrixInverse = drawer.view.inversed
//                }
//        )

        renderer.postSteps.add(
                FilterPostStep(1.0, Delinearize(), listOf("color"), "ldr", ColorFormat.RGB, ColorType.FLOAT16)
        )

        val orb = extend(Orbital()) {
            this.fov = 20.0
            camera.setView(Vector3(-0.49, -0.24, 0.20), Spherical(26.56, 90.0, 6.533), 40.0)
        }

        renderer.draw(drawer, scene)

        val dynNode = SceneNode()
        val dynMaterial = PBRMaterial()
        val dynPrimitive = MeshPrimitive(probeGeometry, dynMaterial)
        val dynMesh = Mesh(listOf(dynPrimitive))
        dynNode.entities.add(dynMesh)
        scene.root.children.add(dynNode)

        scene.dispatcher.launch {
            while (true) {
                dynNode.transform = transform {
                    translate(cos(seconds) * 0.5, 0.5, sin(seconds) * 0.5)
                    scale(2.0)
                }
                yield()
            }
        }

        extend {
            drawer.clear(ColorRGBa.BLACK)
            renderer.draw(drawer, scene)
            drawer.defaults()

        }
    }
}