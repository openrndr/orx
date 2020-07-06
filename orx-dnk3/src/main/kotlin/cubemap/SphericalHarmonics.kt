@file:ShaderPhrases([])

package org.openrndr.extra.dnk3.cubemap

import org.openrndr.draw.*
import org.openrndr.extra.shaderphrases.annotations.ShaderPhrases
import org.openrndr.extra.shaderphrases.phraseResource
import org.openrndr.math.Vector3
import org.openrndr.math.max
import org.openrndr.resourceUrl
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

class SphericalHarmonics : Filter(filterShaderFromUrl(resourceUrl("/shaders/cubemap-filters/spherical-harmonics.frag"))) {
    var input: Cubemap by parameters
}

/** based on https://andrew-pham.blog/2019/08/26/spherical-harmonics/ */
fun Cubemap.irradianceCoefficients(): Array<Vector3> {
    val cubemap = this
    require(cubemap.format == ColorFormat.RGB)
    require(cubemap.type == ColorType.FLOAT32)

    val result = Array(9) { Vector3.ZERO }

    var buffer = ByteBuffer.allocateDirect(cubemap.width * cubemap.width * cubemap.format.componentCount * cubemap.type.componentSize)
    buffer.order(ByteOrder.nativeOrder())

    var weightSum = 0.0

    for (side in CubemapSide.values()) {
        //cubemap.side(side).read(buffer)
        buffer.rewind()
        cubemap.read(side, buffer)

        buffer.rewind()
        for (y in 0 until cubemap.width) {
            for (x in 0 until cubemap.width) {
                val rf = buffer.float.toDouble()
                val gf = buffer.float.toDouble()
                val bf = buffer.float.toDouble()

                val L = Vector3(rf, gf, bf)

                var u = (x + 0.5) / cubemap.width;
                var v = (y + 0.5) / cubemap.width;
                u = u * 2.0 - 1.0
                v = v * 2.0 - 1.0

                val temp = 1.0 + u * u + v * v
                val weight = 4.0 / (sqrt(temp) * temp)

                val N = cubemap.mapUVSToN(u, v, side)
                val coefficients = genLightingCoefficientsForNormal(N, L)

                for (i in 0 until 9) {
                    result[i] += coefficients[i] * weight
                }
                weightSum += weight
            }
        }
    }

    for (i in 0 until 9) {
        result[i] = result[i] * (4.0 * Math.PI) / weightSum
    }

    return result;
}

fun genSHCoefficients(N: Vector3): DoubleArray {
    val result = DoubleArray(9)

    // Band 0
    result[0] = 0.282095;

    // Band 1
    result[1] = 0.488603 * N.y
    result[2] = 0.488603 * N.z
    result[3] = 0.488603 * N.x

    // Band 2
    result[4] = 1.092548 * N.x * N.y
    result[5] = 1.092548 * N.y * N.z
    result[6] = 0.315392 * (3.0 * N.z * N.z - 1.0)
    result[7] = 1.092548 * N.x * N.z
    result[8] = 0.546274 * (N.x * N.x - N.y * N.y)

    return result;
}


fun genLightingCoefficientsForNormal(N: Vector3, L: Vector3): Array<Vector3> {
    val coefficients = genSHCoefficients(N)
    val result = Array(9) { Vector3.ZERO }
    for (i in 0 until 9) {
        result[i] = L * coefficients[i]
    }
    return result
}

fun Cubemap.mapUVSToN(u: Double, v: Double, side: CubemapSide): Vector3 {
    return (side.right * u + side.up * v + side.forward).normalized
}


// Evaluates the irradiance perceived in the provided direction
// Analytic method from http://www1.cs.columbia.edu/~ravir/papers/envmap/envmap.pdf eq. 13
//
fun evaluateSHIrradiance(direction: Vector3, _SH: Array<Vector3>): Vector3 {
    val c1 = 0.42904276540489171563379376569857;    // 4 * Â2.Y22 = 1/4 * sqrt(15.PI)
    val c2 = 0.51166335397324424423977581244463;    // 0.5 * Â1.Y10 = 1/2 * sqrt(PI/3)
    val c3 = 0.24770795610037568833406429782001;    // Â2.Y20 = 1/16 * sqrt(5.PI)
    val c4 = 0.88622692545275801364908374167057;    // Â0.Y00 = 1/2 * sqrt(PI)

    val x = direction.x;
    val y = direction.y;
    val z = direction.z;

    return max(Vector3.ZERO,
            _SH[8] * (c1 * (x * x - y * y))                       // c1.L22.(x²-y²)
                    + _SH[6] * (c3 * (3.0 * z * z - 1))                   // c3.L20.(3.z² - 1)
                    + _SH[0] * c4                                   // c4.L00
                    + (_SH[4] * x * y + _SH[7] * x * z + _SH[5] * y * z) * 2.0 * c1 // 2.c1.(L2-2.xy + L21.xz + L2-1.yz)
                    + (_SH[3] * x + _SH[1] * y + _SH[2] * z) * c2 * 2.0);    // 2.c2.(L11.x + L1-1.y + L10.z)
}

val glslEvaluateSH: String by phraseResource("/phrases/irradiance-sh/evaluate-sh.frag")

val glslFetchSH: String by phraseResource("/phrases/irradiance-sh/fetch-sh.frag")
val glslFetchSH0: String by phraseResource("/phrases/irradiance-sh/fetch-sh0.frag")

fun genGlslGatherSH(xProbes: Int, yProbes: Int, zProbes: Int, spacing: Double = 1.0, offset: Vector3) = """
ivec3 gridCoordinates(vec3 p, out vec3 f) {
    float x = (p.x - ${offset.x}) / $spacing;
    float y = (p.y - ${offset.y})/ $spacing;
    float z = (p.z - ${offset.z}) / $spacing;
                  
    int ix = int(floor(x)) + $xProbes / 2;
    int iy = int(floor(y)) + $yProbes / 2;
    int iz = int(floor(z)) + $zProbes / 2;

    f.x = fract((x));
    f.y = fract((y));
    f.z = fract((z));
                           
    return ivec3(ix, iy, iz);                                  
}

int gridIndex(ivec3 p) {
    ivec3 c = clamp(p, ivec3(0), ivec3(${xProbes - 1}, ${yProbes - 1}, ${zProbes - 1}));
    return c.x + c.y * $xProbes + c.z * ${xProbes * yProbes};
}
    
void gatherSH(samplerBuffer btex, vec3 p, out vec3[9] blend) {
    vec3[9] c000;
    vec3[9] c001;
    vec3[9] c010;
    vec3[9] c011;
    vec3[9] c100;
    vec3[9] c101;
    vec3[9] c110;
    vec3[9] c111;
    
    vec3 f;
    ivec3 io = gridCoordinates(p, f);
    
    fetchSH(btex, gridIndex(io + ivec3(0,0,0)), c000);        
    fetchSH(btex, gridIndex(io + ivec3(0,0,1)), c001);
    fetchSH(btex, gridIndex(io + ivec3(0,1,0)), c010);        
    fetchSH(btex, gridIndex(io + ivec3(0,1,1)), c011);
    fetchSH(btex, gridIndex(io + ivec3(1,0,0)), c100);        
    fetchSH(btex, gridIndex(io + ivec3(1,0,1)), c101);
    fetchSH(btex, gridIndex(io + ivec3(1,1,0)), c110);        
    fetchSH(btex, gridIndex(io + ivec3(1,1,1)), c111);
    
    for (int i = 0; i < 9; ++i) {
        blend[i] =  mix( mix( mix(c000[i], c001[i], f.z), mix(c010[i], c011[i], f.z), f.y), mix( mix(c100[i], c101[i], f.z), mix(c110[i], c111[i], f.z), f.y), f.x);       
    }
}
""".trimIndent()

val glslGridCoordinates: String by phraseResource("/phrases/irradiance-sh/grid-coordinates.frag")
val glslGridIndex: String by phraseResource("/phrases/irradiance-sh/grid-index.frag")
val glslGatherSH: String by phraseResource("/phrases/irradiance-sh/gather-sh.frag")
val glslGatherSH0: String by phraseResource("/phrases/irradiance-sh/gather-sh0.frag")