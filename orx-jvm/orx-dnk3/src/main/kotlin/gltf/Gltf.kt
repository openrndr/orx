@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.openrndr.extra.dnk3.gltf

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import org.openrndr.draw.*
import java.io.File
import java.io.RandomAccessFile
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import kotlin.collections.LinkedHashMap
import kotlin.math.max

const val GLTF_FLOAT = 5126
const val GLTF_UNSIGNED_INT = 5125
const val GLTF_INT = 5124
const val GLTF_UNSIGNED_SHORT = 5123
const val GLTF_SHORT = 5122
const val GLTF_UNSIGNED_BYTE = 5121
const val GLTF_BYTE = 5120

const val GLTF_ARRAY_BUFFER = 34962
const val GLTF_ELEMENT_ARRAY_BUFFER = 34963

@Serializable
data class GltfAsset(val generator: String? = null, val version: String? = null)

@JvmRecord
@Serializable
data class GltfScene(val nodes: IntArray, val name: String? = null)

@JvmRecord
@Serializable
data class GltfNode(
    val name: String? = null,
    val children: IntArray? = null,
    val matrix: DoubleArray? = null,
    val scale: DoubleArray? = null,
    val rotation: DoubleArray? = null,
    val translation: DoubleArray? = null,
    val mesh: Int? = null,
    val skin: Int? = null,
    val camera: Int? = null,
    val extensions: GltfNodeExtensions? = null
)

@JvmRecord
@Serializable
data class KHRLightsPunctualIndex(val light: Int)

@JvmRecord
@Serializable
data class GltfNodeExtensions(val KHR_lights_punctual: KHRLightsPunctualIndex?) {

}

@Serializable
data class GltfPrimitive(
    val attributes: LinkedHashMap<String, Int>,
    val indices: Int? = null,
    val mode: Int? = null,
    val material: Int? = null
) {
    fun createDrawCommand(gltfFile: GltfFile): GltfDrawCommand {

        val indexBuffer = indices?.let { indices ->
            val accessor = gltfFile.accessors[indices]
            val indexType = when (accessor.componentType) {
                GLTF_UNSIGNED_SHORT -> IndexType.INT16
                GLTF_UNSIGNED_INT -> IndexType.INT32
                else -> error("unsupported index type: ${accessor.componentType}")
            }
            val bufferView = gltfFile.bufferViews[accessor.bufferView]
            val buffer = gltfFile.buffers[bufferView.buffer]
            val contents = buffer.contents(gltfFile)
            (contents as Buffer).limit(contents.capacity())
            (contents as Buffer).position((bufferView.byteOffset ?: 0) + (accessor.byteOffset))
            (contents as Buffer).limit(
                (bufferView.byteOffset ?: 0) + (accessor.byteOffset)
                        + accessor.count * indexType.sizeInBytes
            )
            val ib = indexBuffer(accessor.count, indexType)
            ib.write(contents)
            ib
        }

        var maxCount = 0

        abstract class Convertor {
            abstract fun convert(buffer: ByteBuffer, offset: Int, size: Int, writer: BufferWriter)
        }

        class CopyConvertor : Convertor() {
            override fun convert(buffer: ByteBuffer, offset: Int, size: Int, writer: BufferWriter) {
                writer.copyBuffer(buffer, offset, size)
            }
        }

        class Uint8ToUint32Convertor : Convertor() {
            override fun convert(buffer: ByteBuffer, offset: Int, size: Int, writer: BufferWriter) {
                for (i in 0 until 4) {
                    val ui = buffer.get(offset).toInt()
                    writer.write(ui)
                }
            }
        }

        class Uint16ToUint32Convertor : Convertor() {
            override fun convert(buffer: ByteBuffer, offset: Int, size: Int, writer: BufferWriter) {
                for (i in 0 until 4) {
                    val ui = buffer.getShort(offset).toInt()
                    writer.write(ui)
                }
            }
        }

        class CopyPadConvertor(val padFloats: Int) : Convertor() {
            override fun convert(buffer: ByteBuffer, offset: Int, size: Int, writer: BufferWriter) {
                writer.copyBuffer(buffer, offset, size)
                for (i in 0 until padFloats) {
                    writer.write(0.0f)
                }
            }
        }


        val accessors = mutableListOf<GltfAccessor>()
        val convertors = mutableListOf<Convertor>()
        val format = vertexFormat {
            for ((name, index) in attributes.toSortedMap()) {
                val accessor = gltfFile.accessors[index]
                maxCount = max(accessor.count, maxCount)
                when (name) {
                    "NORMAL" -> {
                        normal(3)
                        paddingFloat(1)
                        accessors.add(accessor)
                        convertors.add(CopyPadConvertor(1))
                    }

                    "POSITION" -> {
                        position(3)
                        paddingFloat(1)
                        accessors.add(accessor)
                        convertors.add(CopyPadConvertor(1))
                    }

                    "TANGENT" -> {
                        attribute("tangent", VertexElementType.VECTOR4_FLOAT32)
                        accessors.add(accessor)
                        convertors.add(CopyConvertor())
                    }

                    "TEXCOORD_0" -> {
                        val dimensions = when (accessor.type) {
                            "SCALAR" -> 1
                            "VEC2" -> 2
                            "VEC3" -> 3
                            else -> error("unsupported texture coordinate type ${accessor.type}")
                        }
                        textureCoordinate(4, 0)
                        //paddingFloat(4 - dimensions)
                        accessors.add(accessor)
                        convertors.add(CopyPadConvertor(4 - dimensions))
                    }

                    "JOINTS_0" -> {
                        attribute("joints", VertexElementType.VECTOR4_UINT32)
                        accessors.add(accessor)
                        convertors.add(
                            when (Pair(accessor.type, accessor.componentType)) {
                                Pair("VEC4", GLTF_UNSIGNED_BYTE) -> Uint8ToUint32Convertor()
                                Pair("VEC4", GLTF_UNSIGNED_SHORT) -> Uint16ToUint32Convertor()
                                else -> error("not supported ${accessor.type} / ${accessor.componentType}")
                            }
                        )
                    }

                    "WEIGHTS_0" -> {
                        val type = when (Pair(accessor.type, accessor.componentType)) {
                            Pair("VEC4", GLTF_FLOAT) -> VertexElementType.VECTOR4_FLOAT32
                            else -> error("not supported ${accessor.type} / ${accessor.componentType}")
                        }
                        attribute("weights", type)
                        accessors.add(accessor)
                        convertors.add(CopyConvertor())
                    }
                }
            }
        }

        val buffers =
            accessors.map { it.bufferView }
                .distinct()
                .associate {
                    Pair(
                        gltfFile.bufferViews[it].buffer,
                        gltfFile.buffers[gltfFile.bufferViews[it].buffer].contents(gltfFile)
                    )
                }

        val vb = vertexBuffer(format, maxCount)
        vb.put {
            for (i in 0 until maxCount) {
                for ((a, conv) in accessors zip convertors) {
                    val bufferView = gltfFile.bufferViews[a.bufferView]
                    val buffer = buffers[bufferView.buffer] ?: error("no buffer ${bufferView.buffer}")
                    val componentSize = when (a.componentType) {
                        GLTF_BYTE, GLTF_UNSIGNED_BYTE -> 1
                        GLTF_SHORT, GLTF_UNSIGNED_SHORT -> 2
                        GLTF_FLOAT, GLTF_UNSIGNED_INT, GLTF_INT -> 4
                        else -> error("unsupported type")
                    }
                    val componentCount = when (a.type) {
                        "SCALAR" -> 1
                        "VEC2" -> 2
                        "VEC3" -> 3
                        "VEC4" -> 4
                        "MAT2" -> 4
                        "MAT3" -> 9
                        "MAT4" -> 16
                        else -> error("unsupported type")
                    }
                    val size = componentCount * componentSize
                    val offset = (bufferView.byteOffset ?: 0) + a.byteOffset + i * (bufferView.byteStride ?: size)
                    conv.convert(buffer, offset, size, this)
                    //copyBuffer(buffer, offset, size)
                }
            }
        }
        val drawPrimitive = when (mode) {
            null, 4 -> DrawPrimitive.TRIANGLES
            5 -> DrawPrimitive.TRIANGLE_STRIP
            else -> error("unsupported mode $mode")
        }
        return GltfDrawCommand(vb, indexBuffer, drawPrimitive, indexBuffer?.indexCount ?: maxCount)
    }
}
@Serializable
data class GltfMesh(val primitives: List<GltfPrimitive>, val name: String) {
    fun createDrawCommands(gltfFile: GltfFile): List<GltfDrawCommand> {
        return primitives.map { it.createDrawCommand(gltfFile) }
    }
}

@Serializable
data class GltfPbrMetallicRoughness(
    val baseColorFactor: DoubleArray? = null,
    val baseColorTexture: GltfMaterialTexture? = null,
    var metallicRoughnessTexture: GltfMaterialTexture? = null,
    val roughnessFactor: Double? = null,
    val metallicFactor: Double? = null
)

@Serializable
data class GltfMaterialTexture(val index: Int, val scale: Double? = null, val texCoord: Int? = null)

@Serializable
data class GltfImage(val uri: String? = null, val bufferView: Int? = null)

@Serializable
data class GltfSampler(val magFilter: Int? = null, val minFilter: Int? = null, val wrapS: Int? = null, val wrapT: Int? = null)

@Serializable
data class GltfTexture(val sampler: Int, val source: Int)

@Serializable
data class GltfMaterial(
    val name: String,
    val alphaMode: String? = null,
    val doubleSided: Boolean? = null,
    val normalTexture: GltfMaterialTexture? = null,
    val occlusionTexture: GltfMaterialTexture? = null,
    val emissiveTexture: GltfMaterialTexture? = null,
    val emissiveFactor: DoubleArray? = null,
    val pbrMetallicRoughness: GltfPbrMetallicRoughness? = null,
    val extensions: GltfMaterialExtensions? = null
)

@Serializable
data class GltfMaterialExtensions(
    val KHR_materials_pbrSpecularGlossiness: KhrMaterialsPbrSpecularGlossiness?
)

@Serializable
class KhrMaterialsPbrSpecularGlossiness(val diffuseFactor: DoubleArray?, val diffuseTexture: GltfMaterialTexture?)

@Serializable
data class GltfBufferView(
    val buffer: Int,
    val byteOffset: Int? = null,
    val byteLength: Int,
    val byteStride: Int? = null,
    val target: Int? = null
)

@Serializable
data class GltfBuffer(val byteLength: Int, val uri: String? = null) {
    fun contents(gltfFile: GltfFile): ByteBuffer = if (uri != null) {
        if (uri.startsWith("data:")) {
            val base64 = uri.substring(uri.indexOf(",") + 1)
            val decoded = Base64.getDecoder().decode(base64)
            val buffer = ByteBuffer.allocateDirect(decoded.size)
            buffer.order(ByteOrder.nativeOrder())
            buffer.put(decoded)
            buffer.rewind()
            buffer
        } else {
            val raf = RandomAccessFile(File(gltfFile.file.parentFile, uri), "r")
            val buffer = ByteBuffer.allocateDirect(byteLength)
            buffer.order(ByteOrder.nativeOrder())
            buffer.rewind()
            raf.channel.read(buffer)
            buffer.rewind()
            buffer
        }
    } else {
        gltfFile.bufferBuffer ?: error("no embedded buffer from glb")
    }
}

data class GltfDrawCommand(
    val vertexBuffer: VertexBuffer,
    val indexBuffer: IndexBuffer?,
    val primitive: DrawPrimitive,
    var vertexCount: Int
)

@Serializable
data class GltfAccessor(
    val bufferView: Int,
    val byteOffset: Int = 0,
    val componentType: Int,
    val count: Int,
    val max: DoubleArray? = null,
    val min: DoubleArray? = null,
    val type: String
)

@Serializable
data class GltfAnimation(val name: String? = null, val channels: List<GltfChannel>, val samplers: List<GltfAnimationSampler>)

@Serializable
data class GltfAnimationSampler(val input: Int, val interpolation: String? = null, val output: Int)

@Serializable
data class GltfChannelTarget(val node: Int?, val path: String?)

@Serializable
data class GltfChannel(val sampler: Int, val target: GltfChannelTarget)

@Serializable
data class GltfSkin(val inverseBindMatrices: Int, val joints: IntArray, val skeleton: Int)

@Serializable
data class KHRLightsPunctualLight(
    val color: DoubleArray?,
    val type: String,
    val name: String,
    val intensity: Double?,
    val range: Double? = null,
    val spot: KHRLightsPunctualLightSpot? = null
)

@Serializable
data class KHRLightsPunctualLightSpot(val innerConeAngle: Double?, val outerConeAngle: Double?)

@Serializable
data class KHRLightsPunctual(val lights: List<KHRLightsPunctualLight>)

@Serializable
@JsonIgnoreUnknownKeys
data class GltfExtensions(val KHR_lights_punctual: KHRLightsPunctual? = null)

@Serializable
data class GltfCameraPerspective(val aspectRatio: Double? = null, val yfov: Double, val zfar: Double?, val znear: Double)

@Serializable
data class GltfCameraOrthographic(val xmag: Double, val ymag: Double, val zfar: Double, val znear: Double)

@Serializable
data class GltfCamera(
    val name: String? = null,
    val type: String,
    val perspective: GltfCameraPerspective? = null,
    val orthographic: GltfCameraOrthographic? = null
)

@Serializable
class GltfFile(
    val asset: GltfAsset?,
    val scene: Int? = null,
    val scenes: List<GltfScene>,
    val nodes: List<GltfNode>,
    val meshes: List<GltfMesh>,
    val accessors: List<GltfAccessor>,
    val materials: List<GltfMaterial>,
    val bufferViews: List<GltfBufferView>,
    val buffers: List<GltfBuffer>,
    val images: List<GltfImage>? = null,
    val textures: List<GltfTexture>? = null,
    val samplers: List<GltfSampler>? = null,
    val animations: List<GltfAnimation>? = null,
    val skins: List<GltfSkin>? = null,
    val extensions: GltfExtensions? = null,
    val extensionsUsed: List<String>? = null,
    val extensionsRequired: List<String>? = null,
    val cameras: List<GltfCamera>? = null
) {
    @Transient
    lateinit var file: File

    @Transient
    var bufferBuffer: ByteBuffer? = null
}

fun loadGltfFromFile(file: File): GltfFile = when (file.extension) {
    "gltf" -> {
        val gltfFile = Json{
            ignoreUnknownKeys = true
        }.decodeFromString<GltfFile>(file.readText())
        gltfFile.file = file
        gltfFile
    }

    "glb" -> {
        loadGltfFromGlbFile(file)
    }

    else -> error("extension ${file.extension} not supported in ${file}")
}

