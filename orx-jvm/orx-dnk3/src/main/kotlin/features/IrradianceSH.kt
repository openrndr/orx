package org.openrndr.extra.dnk3.features

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.dnk3.*
import org.openrndr.extra.dnk3.cubemap.irradianceCoefficients
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.transform
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class IrradianceSH(val xCount: Int, val yCount: Int, val zCount: Int, val spacing: Double, val offset: Vector3, val cubemapSize: Int) : Feature {
    override fun <T : Feature> update(drawer: Drawer, sceneRenderer: SceneRenderer, scene: Scene, feature: T, context: RenderContext) {
        sceneRenderer.processIrradiance(drawer, scene, feature as IrradianceSH, context)
    }

    var shMap: BufferTexture? = null
    val probeCount
        get() = xCount * yCount * zCount

}

fun Scene.addIrradianceSH(xCount: Int,
                          yCount: Int,
                          zCount: Int,
                          spacing: Double,
                          offset: Vector3 = Vector3.ZERO,
                          cubemapSize: Int = 256
) {
    features.add(IrradianceSH(xCount * 2 + 1, yCount * 2 + 1, zCount * 2 + 1, spacing, offset, cubemapSize))
    var probeID = 0
    for (k in -zCount..zCount) {
        for (j in -yCount..yCount) {
            for (i in -xCount..xCount) {
                val probeNode = SceneNode()
                probeNode.transform = transform {
                    translate(offset)
                    translate(i * spacing, j * spacing, k * spacing)
                }
                probeNode.entities.add(IrradianceProbe())
                probeID++
                root.children.add(probeNode)
            }
        }
    }
}

private fun SceneRenderer.processIrradiance(drawer: Drawer, scene: Scene, feature: IrradianceSH, context: RenderContext) {
    val irradianceProbes = scene.root.findContent { this as? IrradianceProbe }
    val irradianceProbePositions = irradianceProbes.map { it.node.worldPosition }

    if (feature.shMap == null && irradianceProbes.isNotEmpty()) {
        val hash = scene.hash()
        val cached = File("data/scene-cache/sh-$hash.orb")
        if (cached.exists()) {
            feature.shMap = loadBufferTexture(cached)
        } else {
            var probeID = 0
            val tempCubemap = cubemap(feature.cubemapSize, format = ColorFormat.RGB, type = ColorType.FLOAT32)
            var cubemapDepthBuffer = depthBuffer(feature.cubemapSize, feature.cubemapSize, DepthFormat.DEPTH16, BufferMultisample.Disabled)

            feature.shMap = bufferTexture(irradianceProbes.size * 9, format = ColorFormat.RGB, type = ColorType.FLOAT32)
            val buffer = ByteBuffer.allocateDirect(irradianceProbePositions.size * 9 * 3 * 4)
            buffer.order(ByteOrder.nativeOrder())

            for ((node, probe) in irradianceProbes) {
                if (probe.dirty) {
                    val pass = IrradianceProbePass
                    val materialContext = MaterialContext(pass, context.lights, emptyList(), shadowLightTargets, emptyMap(), 0)
                    val position = node.worldPosition

                    for (side in CubemapSide.values()) {
                        val target = renderTarget(feature.cubemapSize, feature.cubemapSize) {
                            //this.colorBuffer(tempCubemap.side(side))
                            this.cubemap(tempCubemap, side)
                            this.depthBuffer(cubemapDepthBuffer)
                        }
                        drawer.isolatedWithTarget(target) {
                            drawer.clear(ColorRGBa.BLACK)
                            drawer.projection = probe.projectionMatrix
                            drawer.view = Matrix44.IDENTITY
                            drawer.model = Matrix44.IDENTITY
                            drawer.lookAt(position, position + side.forward, side.up)
                            drawPass(drawer, pass, materialContext, context)
                        }

                        target.detachDepthBuffer()
                        target.detachColorAttachments()
                        target.destroy()
                    }
                    val coefficients = tempCubemap.irradianceCoefficients()
                    for (coef in coefficients) {
                        buffer.putVector3((coef))
                    }
                    probeID++
                    //println("$probeID / ${irradianceProbePositions.size}")
                    probe.dirty = false
                }
            }
            feature.shMap?.let {
                buffer.rewind()
                it.write(buffer)
                val f = File("data/scene-cache/sh-$hash.orb")
                if (f.canWrite()) {
                    it.saveToFile(f)
                }
            }
        }
    }
}
