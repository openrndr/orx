package org.openrndr.extra.dnk3.tools

import org.openrndr.draw.*
import org.openrndr.extra.dnk3.*
import org.openrndr.extras.meshgenerators.boxMesh


data class SkyboxMaterial(val cubemap: Cubemap, val intensity: Double = 0.0) : Material {
    override val name: String = "skybox"
    override var doubleSided: Boolean = false
    override var transparent: Boolean = false
    override val fragmentID: Int = 0

    override fun generateShadeStyle(materialContext: MaterialContext, primitiveContext: PrimitiveContext): ShadeStyle {
        return shadeStyle {
            vertexTransform = """ 
                vec2 i = vec2(1.0, 0.0); 
                x_viewMatrix = x_viewNormalMatrix;
            """.trimIndent()

            val combinerFS = materialContext.pass.combiners.map {
                it.generateShader()
            }.joinToString("\n")

            fragmentPreamble = """
                vec4 f_diffuse = vec4(0.0, 0.0, 0.0, 1.0);
                vec3 f_specular = vec3(0.0);
                vec3 f_ambient = vec3(0.0);
                vec3 f_emission = vec3(0.0);
                int f_fragmentID = 0;
                vec4 m_color = vec4(1.0);
                vec4 f_fog = vec4(0.0);
                
            """.trimIndent()
            fragmentTransform = """
                f_diffuse = texture(p_skybox, va_position);
                f_diffuse.rgb *= p_intensity;
            """ + combinerFS

            suppressDefaultOutput = true
            val rt = RenderTarget.active
            materialContext.pass.combiners.map {
                if (rt is ProgramRenderTarget || materialContext.pass === DefaultPass || materialContext.pass === DefaultOpaquePass || materialContext.pass == DefaultTransparentPass || materialContext.pass == IrradianceProbePass) {
                    this.output(it.targetOutput, ShadeStyleOutput(0))
                } else {
                    val index = rt.colorAttachmentIndexByName(it.targetOutput)
                            ?: error("attachment ${it.targetOutput} not found")
                    val type = rt.colorBuffer(index).type
                    val format = rt.colorBuffer(index).format
                    this.output(it.targetOutput, ShadeStyleOutput(index, format, type))
                }
            }
        }
    }

    override fun applyToShadeStyle(context: MaterialContext, shadeStyle: ShadeStyle) {
        shadeStyle.parameter("skybox", cubemap)
        shadeStyle.parameter("intensity", intensity)
    }


    override fun hashCode(): Int {
        var result = intensity.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + doubleSided.hashCode()
        result = 31 * result + transparent.hashCode()
        result = 31 * result + fragmentID
        return result
    }


}

fun Scene.addSkybox(cubemapUrl: String, size: Double = 100.0, intensity: Double = 1.0) {
    val cubemap = Cubemap.fromUrl(cubemapUrl, Session.active).apply { generateMipmaps() }
    val box = boxMesh(size, size, size, 1, 1, 1, true)
    val node = SceneNode()
    val material = SkyboxMaterial(cubemap, intensity)
    val geometry = Geometry(listOf(box), null, DrawPrimitive.TRIANGLES, 0, box.vertexCount)
    val primitive = MeshPrimitive(geometry, material)
    val mesh = Mesh(listOf(primitive))
    node.entities.add(mesh)
    root.children.add(node)
}