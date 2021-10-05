package org.openrndr.extra.dnk3

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.dnk3.features.IrradianceSH
import org.openrndr.extra.fx.blur.ApproximateGaussianBlur
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector3
import java.nio.ByteBuffer

class RenderContext(
        val lights: List<NodeContent<Light>>,
        val meshes: List<NodeContent<Mesh>>,
        val skinnedMeshes: List<NodeContent<SkinnedMesh>>,
        val instancedMeshes: List<NodeContent<InstancedMesh>>,
        val pathMeshes: List<NodeContent<PathMesh>>,
        val fogs: List<NodeContent<Fog>>
)

class SceneRenderer {
    class Configuration {
        var multisampleLines = false
    }

    val configuration = Configuration()

    val blur = ApproximateGaussianBlur()

    var shadowLightTargets = mutableMapOf<ShadowLight, RenderTarget>()
    var meshCubemaps = mutableMapOf<Mesh, Cubemap>()


    var outputPasses = mutableListOf(DefaultOpaquePass, DefaultTransparentPass)
    var outputPassTarget: RenderTarget? = null
    var outputPassTargetMS: RenderTarget? = null

    val postSteps = mutableListOf<PostStep>()
    val buffers = mutableMapOf<String, ColorBuffer>()

    var drawFinalBuffer = true

    var first = true
    fun draw(drawer: Drawer, scene: Scene) {
        drawer.pushStyle()
        drawer.depthWrite = true
        drawer.depthTestPass = DepthTestPass.LESS_OR_EQUAL

        drawer.cullTestPass = CullTestPass.FRONT

        scene.dispatcher.execute()

        // update all the transforms
        scene.root.scan(Matrix44.IDENTITY) { p ->
            if (p !== Matrix44.IDENTITY) {
                worldTransform = p * transform
            } else {
                worldTransform = transform
            }
            worldTransform
        }

        val context = RenderContext(
                lights = scene.root.findContent { this as? Light },
                meshes = scene.root.findContent { this as? Mesh },
                skinnedMeshes = scene.root.findContent { this as? SkinnedMesh },
                fogs = scene.root.findContent { this as? Fog },
                instancedMeshes = scene.root.findContent { this as? InstancedMesh },
                pathMeshes = scene.root.findContent { this as? PathMesh}
        )

        // shadow passes
        run {
            context.lights.filter { it.content is ShadowLight && (it.content as ShadowLight).shadows is Shadows.MappedShadows }.forEach {
                val shadowLight = it.content as ShadowLight
                val pass: RenderPass
                pass = when (shadowLight.shadows) {
                    is Shadows.PCF, is Shadows.Simple -> {
                        LightPass
                    }
                    is Shadows.VSM -> {
                        VSMLightPass
                    }
                    else -> TODO()
                }
                val target = shadowLightTargets.getOrPut(shadowLight) {
                    val mapSize = (shadowLight.shadows as Shadows.MappedShadows).mapSize
                    pass.createPassTarget(mapSize, mapSize, DepthFormat.DEPTH16)
                }
                target.clearDepth(depth = 1.0)

                val look = shadowLight.view(it.node)
                val materialContext = MaterialContext(pass, context.lights, context.fogs, shadowLightTargets, emptyMap(), 0)
                drawer.isolatedWithTarget(target) {
                    drawer.projection = shadowLight.projection(target)
                    drawer.view = look
                    drawer.model = Matrix44.IDENTITY

                    drawer.clear(ColorRGBa.BLACK)
                    drawer.cullTestPass = CullTestPass.FRONT
                    drawPass(drawer, pass, materialContext, context)
                }
                when (shadowLight.shadows) {
                    is Shadows.VSM -> {
                        blur.gain = 1.0
                        blur.sigma = 3.0
                        blur.window = 9
                        blur.spread = 1.0
                        blur.apply(target.colorBuffer(0), target.colorBuffer(0))
                    }
                }
            }
        }

        // -- feature passes
        for (feature in scene.features) {
            feature.update(drawer, this, scene, feature, context)
        }

        // -- output passes
        run {
            val irradianceSH = scene.features.find { it is IrradianceSH } as? IrradianceSH
            for (pass in outputPasses) {
                val materialContext = MaterialContext(pass, context.lights, context.fogs, shadowLightTargets, meshCubemaps, irradianceSH?.probeCount
                        ?: 0)
                materialContext.irradianceSH = irradianceSH

                val defaultPasses = setOf(DefaultTransparentPass, DefaultOpaquePass)

                if ((pass !in defaultPasses || postSteps.isNotEmpty()) && outputPassTarget == null) {
                    outputPassTarget = pass.createPassTarget(RenderTarget.active.width, RenderTarget.active.height)
                }

                if (pass == outputPasses[0]) {
                    outputPassTarget?.let {
                        drawer.withTarget(it) {
                            clear(ColorRGBa.TRANSPARENT)
                        }
                    }
                }
                outputPassTarget?.let { target ->
                    pass.combiners.forEach {
                        if (it is ColorBufferFacetCombiner) {
                            val index = target.colorAttachmentIndexByName(it.targetOutput)
                                    ?: error("attachment not found ${it.targetOutput}")
                            target.blendMode(index, it.blendMode)
                        }
                    }
                }
                outputPassTarget?.bind()
                drawPass(drawer, pass, materialContext, context)
                outputPassTarget?.unbind()

                outputPassTarget?.let { output ->
                    for (combiner in pass.combiners) {
                        buffers[combiner.targetOutput] = (output.colorAttachmentByName(combiner.targetOutput) as? ColorBufferAttachment)?.colorBuffer
                                ?: error("attachment not found ${combiner.targetOutput}")
                    }
                }
            }
            val lightContext = LightContext(context.lights, shadowLightTargets)
            val postContext = PostContext(lightContext, drawer.view.inversed)

            for (postStep in postSteps) {
                postStep.apply(buffers, postContext)
            }
        }

        drawer.popStyle()
        if (drawFinalBuffer) {
            outputPassTarget?.let { output ->
                drawer.isolated {
                    drawer.defaults()
                    drawer.ortho()
                    val outputName = (postSteps.lastOrNull() as? FilterPostStep<*>)?.output ?: "color"
                    val outputBuffer = buffers[outputName]
                            ?: throw IllegalArgumentException("can't find $outputName buffer")
                    drawer.image(outputBuffer)
                }
            }
        }
    }

    internal fun drawPass(drawer: Drawer, pass: RenderPass, materialContext: MaterialContext,
                          context: RenderContext, shadeStyleTransformer: ((ShadeStyle)->Unit)? = null
    ) {

        drawer.depthWrite = pass.depthWrite
        val primitives = context.meshes.flatMap { mesh ->
            mesh.content.primitives.map { primitive ->
                NodeContent(mesh.node, primitive)
            }
        }

        // -- draw all meshes
        primitives
                .filter { (it.content.material.transparent && pass.renderTransparent) || (!it.content.material.transparent && pass.renderOpaque) }
                .forEach {
                    val primitive = it.content
                    drawer.isolated {
                        if (primitive.material.doubleSided) {
                            drawer.drawStyle.cullTestPass = CullTestPass.ALWAYS
                        }
                        val hasNormalAttribute = primitive.geometry.vertexBuffers.any { it.vertexFormat.hasAttribute("normal") }
                        val primitiveContext = PrimitiveContext(hasNormalAttribute, false)
                        val shadeStyle = primitive.material.generateShadeStyle(materialContext, primitiveContext)
                        shadeStyle.parameter("viewMatrixInverse", drawer.view.inversed)
                        primitive.material.applyToShadeStyle(materialContext, shadeStyle)
                        shadeStyleTransformer?.invoke(shadeStyle)

                        drawer.shadeStyle = shadeStyle
                        drawer.model = it.node.worldTransform

                        if (primitive.geometry.indexBuffer == null) {
                            drawer.vertexBuffer(primitive.geometry.vertexBuffers,
                                    primitive.geometry.primitive,
                                    primitive.geometry.offset,
                                    primitive.geometry.vertexCount)
                        } else {
                            drawer.vertexBuffer(primitive.geometry.indexBuffer!!,
                                    primitive.geometry.vertexBuffers,
                                    primitive.geometry.primitive,
                                    primitive.geometry.offset,
                                    primitive.geometry.vertexCount)
                        }
                    }
                }


        val skinnedPrimitives = context.skinnedMeshes.flatMap { mesh ->
            mesh.content.primitives.map { primitive ->
                NodeContent(mesh.node, Pair(primitive, mesh))
            }
        }

        skinnedPrimitives
                .filter {
                    (it.content.first.material.transparent && pass.renderTransparent) ||
                            (!it.content.first.material.transparent && pass.renderOpaque)
                }
                .forEach {
                    val primitive = it.content.first
                    val skinnedMesh = it.content.second.content
                    drawer.isolated {
                        if (primitive.material.doubleSided) {
                            drawer.drawStyle.cullTestPass = CullTestPass.ALWAYS
                        }
                        val hasNormalAttribute = primitive.geometry.vertexBuffers.any { it.vertexFormat.hasAttribute("normal") }
                        val primitiveContext = PrimitiveContext(hasNormalAttribute, true)

                        val nodeInverse = it.node.worldTransform.inversed


                        val jointTransforms = (skinnedMesh.joints zip skinnedMesh.inverseBindMatrices)
                                .map { (nodeInverse * it.first.worldTransform * it.second) }
                        val shadeStyle = primitive.material.generateShadeStyle(materialContext, primitiveContext)

                        shadeStyle.parameter("jointTransforms", jointTransforms.toTypedArray())

                        shadeStyle.parameter("viewMatrixInverse", drawer.view.inversed)
                        primitive.material.applyToShadeStyle(materialContext, shadeStyle)
                        drawer.shadeStyle = shadeStyle
                        drawer.model = it.node.worldTransform

                        if (primitive.geometry.indexBuffer == null) {
                            drawer.vertexBuffer(primitive.geometry.vertexBuffers,
                                    primitive.geometry.primitive,
                                    primitive.geometry.offset,
                                    primitive.geometry.vertexCount)
                        } else {
                            drawer.vertexBuffer(primitive.geometry.indexBuffer!!,
                                    primitive.geometry.vertexBuffers,
                                    primitive.geometry.primitive,
                                    primitive.geometry.offset,
                                    primitive.geometry.vertexCount)
                        }
                    }
                }


        val instancedPrimitives = context.instancedMeshes.flatMap { mesh ->
            mesh.content.primitives.map { primitive ->
                NodeContent(mesh.node, MeshPrimitiveInstance(primitive, mesh.content.instances, mesh.content.attributes))
            }
        }

        // -- draw all instanced meshes
        instancedPrimitives
                .filter { (it.content.primitive.material.transparent && pass.renderTransparent) || (!it.content.primitive.material.transparent && pass.renderOpaque) }
                .forEach {
                    val primitive = it.content
                    drawer.isolated {
                        val primitiveContext = PrimitiveContext(true, false)
                        val shadeStyle = primitive.primitive.material.generateShadeStyle(materialContext, primitiveContext)
                        shadeStyle.parameter("viewMatrixInverse", drawer.view.inversed)
                        primitive.primitive.material.applyToShadeStyle(materialContext, shadeStyle)
                        if (primitive.primitive.material.doubleSided) {
                            drawer.drawStyle.cullTestPass = CullTestPass.ALWAYS
                        }
                        drawer.shadeStyle = shadeStyle
                        drawer.model = it.node.worldTransform
                        drawer.vertexBufferInstances(primitive.primitive.geometry.vertexBuffers,
                                primitive.attributes,
                                DrawPrimitive.TRIANGLES,
                                primitive.instances,
                                primitive.primitive.geometry.offset,
                                primitive.primitive.geometry.vertexCount)
                    }
                }

                context.pathMeshes.filter { (it.content.material.transparent && pass.renderTransparent) || (!it.content.material.transparent && pass.renderOpaque) }
                .forEach {
                    drawer.isolated {
                        val primitiveContext = PrimitiveContext(true, false)
                        val shadeStyle = it.content.material.generateShadeStyle(materialContext, primitiveContext)
                        shadeStyle.parameter("viewMatrixInverse", drawer.view.inversed)
                        it.content.material.applyToShadeStyle(materialContext, shadeStyle)
                        drawer.drawStyle.cullTestPass = CullTestPass.ALWAYS
                        drawer.shadeStyle = shadeStyle
                        drawer.model = it.node.worldTransform
                        drawer.strokeWeight = it.content.weight
                        for (path in it.content.paths) {
                            drawer.path(path.sampleLinear(0.0005))
                        }
                    }
                }


                drawer.depthWrite = true
    }
}

fun sceneRenderer(builder: SceneRenderer.() -> Unit): SceneRenderer {
    val sceneRenderer = SceneRenderer()
    sceneRenderer.builder()
    return sceneRenderer
}

internal fun ByteBuffer.putVector3(v: Vector3) {
    putFloat(v.x.toFloat())
    putFloat(v.y.toFloat())
    putFloat(v.z.toFloat())
}