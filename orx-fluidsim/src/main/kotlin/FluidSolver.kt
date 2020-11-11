
package org.openrndr.extra.fluidsim

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.Drawer
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.renderTarget
import org.openrndr.draw.shadeStyle
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.math.pow

interface UpdateFluidCallback {
    operator fun invoke(solver: FluidSolver)
}

class FluidSolver(
        val width: Int,
        val height: Int
) {
    val params = FluidParams()
    val fluidBounds = Rectangle(0.0, 0.0, width.toDouble(), height.toDouble())
    val pixelScale = Vector2(1.0 / width, 1.0 / height)

    var updateCallback: UpdateFluidCallback? = null

    val renderTarget = renderTarget(width, height) { depthBuffer() }

    val velocity    = PingPongBuffer(width, height, format = ColorFormat.RG,   type = ColorType.FLOAT32)
    val density     = PingPongBuffer(width, height, format = ColorFormat.RGBa, type = ColorType.FLOAT32)
    val pressure    = PingPongBuffer(width, height, format = ColorFormat.R,    type = ColorType.FLOAT32)
    val obstacleC   = colorBuffer(width,    height, format = ColorFormat.R,    type = ColorType.UINT8)
    val obstacleN   = PingPongBuffer(width, height, format = ColorFormat.RGBa, type = ColorType.UINT8)
    val curl        =    colorBuffer(width, height, format = ColorFormat.R,    type = ColorType.FLOAT32)
    val divergence  =    colorBuffer(width, height, format = ColorFormat.R,    type = ColorType.FLOAT32)

    init {
        val clearColor = ColorRGBa.TRANSPARENT
        velocity.fill(clearColor)
        density.fill(clearColor)
        pressure.fill(clearColor)
        obstacleN.fill(clearColor)
        obstacleC.fill(clearColor)
        curl.fill(clearColor)
        divergence.fill(clearColor)
    }

    fun update(drawer: Drawer) {
        createObstacleN(drawer)

        advect(drawer, velocity, params.velocityDissipation.pow(0.05))
        advect(drawer, density, params.densityDissipation.pow(0.05))

        updateCallback?.invoke(this)

        vorticity(drawer)
        divergence(drawer)
        jacobi(drawer)
        gradient(drawer)
    }

    private fun createObstacleN(drawer: Drawer) {
        drawer.isolated(obstacleN) {
            shadeStyle = shadeStyle {
                fragmentTransform = """
                    vec2 pos = vec2(c_boundsPosition.x, 1 - c_boundsPosition.y);
                    x_fill.r = textureOffset(p_obstacleC, pos, + ivec2(0,1)).r;
                    x_fill.g = textureOffset(p_obstacleC, pos, - ivec2(0,1)).r;
                    x_fill.b = textureOffset(p_obstacleC, pos, + ivec2(1,0)).r;
                    x_fill.a = textureOffset(p_obstacleC, pos, - ivec2(1,0)).r;
                """.trimIndent()
                parameter("obstacleC", obstacleC)
            }
            rectangle(bounds)
        }
        obstacleN.swap()
    }

    private fun advect(drawer: Drawer, buffer: PingPongBuffer, dissipation: Double) {
        drawer.isolated(buffer) {
            shadeStyle = shadeStyle {
                fragmentTransform = """
                    vec2 pos = vec2(c_boundsPosition.x, 1 - c_boundsPosition.y);
                    float oC = texture(p_obstacleC, pos).r;
                    if (oC >= 1.0) {
                        x_fill = vec4(0);
                    } else {
                        vec2 vel = texture(p_velocity, pos).xy;
                        vec2 pos_back = pos - p_timestep * p_rdx * vel * p_pixelScale;
                        x_fill = p_dissipation * texture(p_sourceTexture, pos_back);
                    }
                """.trimIndent()
                parameter("pixelScale", pixelScale)
                parameter("obstacleC", obstacleC)
                parameter("velocity", velocity.src())
                parameter("sourceTexture", buffer.src())
                parameter("timestep", params.timestep)
                parameter("rdx", 1.0 / params.gridScale)
                parameter("dissipation", dissipation)
            }
            rectangle(fluidBounds)
        }
        buffer.swap()
    }

    private fun vorticity(drawer: Drawer) {
        if (params.vorticity >= 0.0) {

            drawer.isolated(curl) {
                shadeStyle = shadeStyle {
                    fragmentTransform = """
                        vec2 pos = vec2(c_boundsPosition.x, 1 - c_boundsPosition.y);
                        float oC = texture(p_obstacleC, pos).r;
                        if (oC >= 1.0) {
                            x_fill.r = 0.0;
                            return;
                        }
                        
                        vec2 vT = textureOffset(p_velocity, pos, + ivec2(0,1)).rg;
                        vec2 vB = textureOffset(p_velocity, pos, - ivec2(0,1)).rg;
                        vec2 vR = textureOffset(p_velocity, pos, + ivec2(1,0)).rg;
                        vec2 vL = textureOffset(p_velocity, pos, - ivec2(1,0)).rg;
                        vec2 vC = texture      (p_velocity, pos              ).rg;
                        
                        x_fill.r = p_halfrdx * ((vT.x - vB.x) - (vR.y - vL.y));
                    """
                    parameter("velocity", velocity.src())
                    parameter("obstacleC", obstacleC)
                    parameter("halfrdx", 0.5 / params.gridScale)
                }
                rectangle(fluidBounds)
            }

            drawer.isolated(velocity) {
                shadeStyle = shadeStyle {
                    fragmentTransform = """
                        vec2 pos = vec2(c_boundsPosition.x, 1 - c_boundsPosition.y);
                        vec2 vOld = texture(p_velocity, pos).xy;
                        
                        // curl
                        float cT = abs(textureOffset(p_curl, pos, + ivec2(0,1)).r);
                        float cB = abs(textureOffset(p_curl, pos, - ivec2(0,1)).r);
                        float cR = abs(textureOffset(p_curl, pos, + ivec2(1,0)).r);
                        float cL = abs(textureOffset(p_curl, pos, - ivec2(1,0)).r);
                        float cC =     texture      (p_curl, pos              ).r;
                        
                        // normalize
                        vec2 dw = normalize(p_halfrdx * vec2(cT - cB, cR - cL) + 0.000001) * vec2(-1, 1);
                                            
                        // vorticity confinement
                        vec2 fvc = dw * cC * p_timestep * p_vorticity;
                     
                        // add to velocity
                        vec2 vNew = vOld + fvc;
  
                        x_fill.rg = vNew;
                    """
                    parameter("velocity", velocity.src())
                    parameter("curl", curl)
                    parameter("vorticity", params.vorticity)
                    parameter("timestep", params.timestep)
                    parameter("halfrdx", 0.5 / params.gridScale)
                }
                rectangle(fluidBounds)
            }
            velocity.swap()
        }
    }

    private fun divergence(drawer: Drawer) {
        drawer.isolated(divergence) {
            shadeStyle = shadeStyle {
                fragmentTransform = """
                    vec2 pos = vec2(c_boundsPosition.x, 1 - c_boundsPosition.y);
                    float oC = texture(p_obstacleC, pos).r;
                    if (oC >= 1.0) { 
                        x_fill.r = 0.0; 
                        return;
                    }
                    
                    vec2 vT = textureOffset(p_velocity, pos, + ivec2(0,1)).xy;
                    vec2 vB = textureOffset(p_velocity, pos, - ivec2(0,1)).xy;
                    vec2 vR = textureOffset(p_velocity, pos, + ivec2(1,0)).xy;
                    vec2 vL = textureOffset(p_velocity, pos, - ivec2(1,0)).xy;
                    vec2 vC = texture      (p_velocity, pos              ).xy;
                    
                    // no-slip (zero) velocity boundary conditions
                    // use negative center velocity if neighbor is an obstacle
                    vec4 oN = texture(p_obstacleN, pos);
                    vT = mix(vT, -vC, oN.x);  // if(oT > 0.0) vT = -vC;
                    vB = mix(vB, -vC, oN.y);  // if(oB > 0.0) vB = -vC;
                    vR = mix(vR, -vC, oN.z);  // if(oR > 0.0) vR = -vC;
                    vL = mix(vL, -vC, oN.w);  // if(oL > 0.0) vL = -vC;
                      
                    x_fill.r = p_halfrdx  * ((vR.x - vL.x) + (vT.y - vB.y));
  
                """.trimIndent()
                parameter("velocity", velocity.src())
                parameter("halfrdx", 0.5 / params.gridScale)
                parameter("obstacleC", obstacleC)
                parameter("obstacleN", obstacleN.src())
            }
            rectangle(fluidBounds)
        }
    }

    private fun jacobi(drawer: Drawer) {

        pressure.fill(ColorRGBa.TRANSPARENT)

        repeat(params.jacobiIterations) {
            drawer.isolated(pressure) {
                shadeStyle = shadeStyle {
                    fragmentTransform = """
                        vec2 pos = vec2(c_boundsPosition.x, 1 - c_boundsPosition.y);

                        float oC = texture(p_obstacleC, pos).r;
                        if (oC >= 1.0) {
                            x_fill = vec4(0.0);
                            return;
                        }

                        // tex b
                        float bC = texture(p_divergence, pos).r;

                        // tex x
                        float xT = textureOffset(p_pressure, pos, + ivec2(0,1)).r;
                        float xB = textureOffset(p_pressure, pos, - ivec2(0,1)).r;
                        float xR = textureOffset(p_pressure, pos, + ivec2(1,0)).r;
                        float xL = textureOffset(p_pressure, pos, - ivec2(1,0)).r;
                        float xC = texture      (p_pressure, pos              ).r;

                        // pure Neumann pressure boundary use center pressure if neighbor is an obstacle
                        vec4 oN = texture(p_obstacleN, pos);
                        xT = mix(xT, xC, oN.x);
                        xB = mix(xB, xC, oN.y);
                        xR = mix(xR, xC, oN.z);
                        xL = mix(xL, xC, oN.w);

                        x_fill.r = (xL + xR + xB + xT + p_alpha * bC) * p_rBeta;

                    """.trimIndent()
                    parameter("alpha", -(params.gridScale * params.gridScale))
                    parameter("rBeta", 0.25)
                    parameter("pressure", pressure.src())
                    parameter("divergence", divergence)
                    parameter("obstacleC", obstacleC)
                    parameter("obstacleN", obstacleN.src())
                }
                rectangle(fluidBounds)
            }
            pressure.swap()
        }
    }

    private fun gradient(drawer: Drawer) {
        drawer.isolated(velocity) {
            shadeStyle = shadeStyle {
                fragmentTransform = """
                    vec2 pos = vec2(c_boundsPosition.x, 1 - c_boundsPosition.y);
                    float oC = texture(p_obstacleC, pos).r;
                    if (oC >= 1.0) {
                        x_fill.rg = vec2(0);
                        return;
                    }

                    // pressure
                    float pT = textureOffset(p_pressure, pos, + ivec2(0,1)).r;
                    float pB = textureOffset(p_pressure, pos, - ivec2(0,1)).r;
                    float pR = textureOffset(p_pressure, pos, + ivec2(1,0)).r;
                    float pL = textureOffset(p_pressure, pos, - ivec2(1,0)).r;
                    float pC = texture      (p_pressure, pos              ).r;

                    // pure Neumann pressure boundary use center pressure if neighbor is an obstacle
                    vec4 oN = texture(p_obstacleN, pos);
                    pT = mix(pT, pC, oN.x);
                    pB = mix(pB, pC, oN.y);
                    pR = mix(pR, pC, oN.z);
                    pL = mix(pL, pC, oN.w);

                    // gradient subtract
                    vec2 grad = p_halfrdx * vec2(pR - pL, pT - pB) * p_gradientMag;
                    vec2 vOld = texture(p_velocity, pos).rg;
                    x_fill.rg = vOld - grad;

                """.trimIndent()
                parameter("halfrdx", 0.5 / params.gridScale)
                parameter("gradientMag", params.gradientScale)
                parameter("pressure", pressure.src())
                parameter("velocity", velocity.src())
                parameter("obstacleC", obstacleC)
                parameter("obstacleN", obstacleN.src())
            }
            rectangle(fluidBounds)
        }
        velocity.swap()
    }

    fun addVelocity(drawer: Drawer, pos: Vector2, vel: Vector2, radius: Double, blendMode: Int, mix: Double = 0.5) {
        drawer.isolated(velocity) {
            shadeStyle = shadeStyle {
                fragmentTransform = """
                    vec2 pos = vec2(c_boundsPosition.x, 1 - c_boundsPosition.y);
                    vec2 mousePos = vec2(p_pos.x, 1 - p_pos.y);
                    
                    vec2 vOld = texture(p_velocityTexture, pos).rg;
                    float dist = distance(mousePos, pos);
                    
                    if (dist < p_radius) {
                        float dist_norm = 1.0 - clamp( dist / p_radius, 0.0, 1.0);

                        if (p_blendmode == 0) {
                            x_fill.rg = p_vel;
                        }
                        if (p_blendmode == 1) {
                            float falloff = clamp(sqrt(dist_norm * 0.1), 0, 1);
                            x_fill.rg = vOld + p_vel * falloff;
                        }
                        if (p_blendmode == 2) {
                            vec2 vNew = p_vel * dist_norm;
                            if (length(vOld) > length(vNew)) {
                                x_fill.rg = vOld;
                            } else {
                                x_fill.rg = mix(vOld, vNew, p_mix);
                            }
                        }
                    } else {
                        x_fill.rg = vOld;
                    }
                """.trimIndent()
                parameter("obstacleC", obstacleC)
                parameter("velocityTexture", velocity.src())
                parameter("blendmode", blendMode)
                parameter("radius", radius / fluidBounds.width)
                parameter("pos", pos)
                parameter("vel", vel)
                parameter("mix", mix)
            }
            rectangle(fluidBounds)
        }
        velocity.swap()
    }

    fun addFluidColor(drawer: Drawer, pos: Vector2, color: ColorRGBa, radius: Double) {
        drawer.isolated(density) {
            shadeStyle = shadeStyle {
                fragmentTransform = """
                    vec2 pos = vec2(c_boundsPosition.x, 1 - c_boundsPosition.y);
                    vec2 mousePos = vec2(p_pos.x, 1 - p_pos.y);
                    
                    vec3 oldColor = texture(p_density, pos).rgb;
                    float dist = distance(mousePos, pos);
                    
                    if (dist < p_radius) {
                        float dist_norm = 1.0 - clamp( dist / p_radius, 0.0, 1.0);
                        x_fill.rgb = mix(oldColor, p_color.rgb, dist_norm);
                    } else {
                        x_fill.rgb = oldColor;
                    }
                    
                """.trimIndent()
                parameter("obstacleC", obstacleC)
                parameter("density", density.src())
                parameter("radius", radius / fluidBounds.width)
                parameter("pos", pos)
                parameter("color", color)
            }
            rectangle(fluidBounds)
        }
        density.swap()
    }

    private inline fun Drawer.isolated(buffer: PingPongBuffer, block: Drawer.() -> Unit) {
        isolated(buffer.dst(), block)
    }

    private inline fun Drawer.isolated(buffer: ColorBuffer, block: Drawer.() -> Unit) {
        renderTarget.attach(buffer)
        renderTarget.bind()
        pushTransforms()
        pushStyle()
        block()
        popStyle()
        popTransforms()
        renderTarget.unbind()
        renderTarget.detachColorAttachments()
    }


}