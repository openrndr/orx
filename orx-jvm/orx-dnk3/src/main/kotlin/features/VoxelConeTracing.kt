package org.openrndr.extra.dnk3.features

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.dnk3.*
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector3

data class VoxelConeTracing(val xCount: Int, val yCount: Int, val zCount: Int, val spacing: Double, val offset: Vector3) : Feature {
    var voxelMap: VolumeTexture? = null
    var voxelRenderTarget = null as? RenderTarget?
    override fun <T : Feature> update(drawer: Drawer, sceneRenderer: SceneRenderer, scene: Scene, feature: T, context: RenderContext) {
        sceneRenderer.processVoxelConeTracing(drawer, scene, this, context)
    }

    var initialized = false
    val voxelPass = RenderPass(listOf(VoxelFacet(this)), renderOpaque = true, renderTransparent = false, depthWrite = false, skipTarget = true)
}

fun Scene.addVoxelConeTracing(xCount: Int, yCount: Int, zCount: Int, spacing: Double, offset: Vector3 = Vector3.ZERO) : VoxelConeTracing {
    val feature = VoxelConeTracing(xCount, yCount, zCount, spacing, offset)
    features.add(feature)
    return feature
}

class VoxelFacet(val voxelConeTracing: VoxelConeTracing) : ColorBufferFacetCombiner(setOf(FacetType.DIFFUSE, FacetType.SPECULAR, FacetType.EMISSIVE), "color", ColorFormat.RGBa, ColorType.FLOAT16) {
    override fun generateShader() = """
    vec3 finalColor =  (max(vec3(0.0), f_diffuse.rgb) + max(vec3(0.0), f_emission.rgb) + max(vec3(0.0), f_ambient.rgb));
    vec3 p = v_worldPosition;
    {                 
        float x = (p.x - ${voxelConeTracing.offset.x}) / ${voxelConeTracing.spacing};
        float y = (p.y - ${voxelConeTracing.offset.y}) / ${voxelConeTracing.spacing};
        float z = (p.z - ${voxelConeTracing.offset.z}) / ${voxelConeTracing.spacing};
    
        int ix = int(floor(x+0.5)) + ${voxelConeTracing.xCount} / 2;
        int iy = int(floor(y+0.5)) + ${voxelConeTracing.yCount} / 2;
        int iz = int(floor(z+0.5)) + ${voxelConeTracing.zCount} / 2;
        imageStore(p_voxelMap, ivec3(ix, iy, iz), vec4(finalColor, 1.0));
    }
    """
}

private fun SceneRenderer.processVoxelConeTracing(drawer: Drawer, scene: Scene, feature: VoxelConeTracing, context: RenderContext) {
    if (feature.voxelMap == null) {
        feature.voxelMap = volumeTexture(feature.xCount * 2 + 1, feature.yCount * 2 + 1, feature.zCount * 2 + 1, format = ColorFormat.RGBa, type = ColorType.FLOAT16)
    }
    if (feature.voxelRenderTarget == null) {
        feature.voxelRenderTarget = renderTarget(2048, 2048, 1.0, BufferMultisample.SampleCount(8)) {
            colorBuffer()
        }
    }
    if (!feature.initialized) {
        println("drawing voxelmap")
        for (side in CubemapSide.values()) {
            drawer.isolatedWithTarget(feature.voxelRenderTarget ?: error("no render target")) {
                val pass = feature.voxelPass
                val materialContext = MaterialContext(pass, context.lights, emptyList(), shadowLightTargets, emptyMap(), 0)
                drawer.clear(ColorRGBa.BLACK)
                drawer.ortho(-10.0, 10.0, -10.0, 10.0, -40.0, 40.0)
                drawer.view = Matrix44.IDENTITY
                drawer.model = Matrix44.IDENTITY
                val position = Vector3.ZERO
                drawer.lookAt(position + side.forward*40.0, position , side.up)
                drawPass(drawer, pass, materialContext, context) {
                    it.image("voxelMap", feature.voxelMap!!.imageBinding(0, ImageAccess.WRITE))
                }
            }
        }
        feature.initialized = true
    }
}