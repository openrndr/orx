@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.openrndr.extra.dnk3.gltf

import com.google.gson.Gson
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

class GltfAsset(val generator: String?, val version: String?)

class GltfScene(val nodes: IntArray)

class GltfNode(val children: IntArray?,
               val matrix: DoubleArray?,
               val scale: DoubleArray?,
               val rotation: DoubleArray?,
               val translation: DoubleArray?,
               val mesh: Int?,
               val skin: Int?)

class GltfPrimitive(val attributes: LinkedHashMap<String, Int>, val indices: Int?, val mode: Int?, val material: Int) {
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
            (contents as Buffer).limit((bufferView.byteOffset ?: 0) + (accessor.byteOffset)
                    + accessor.count * indexType.sizeInBytes)
            val ib = indexBuffer(accessor.count, indexType)
            ib.write(contents)
            ib
        }

        var maxCount = 0

        val accessors = mutableListOf<GltfAccessor>()
        val format = vertexFormat {
            for ((name, index) in attributes.toSortedMap()) {
                val accessor = gltfFile.accessors[index]
                maxCount = max(accessor.count, maxCount)
                when (name) {
                    "NORMAL" -> {
                        normal(3)
                        accessors.add(accessor)
                    }
                    "POSITION" -> {
                        position(3)
                        accessors.add(accessor)
                    }
                    "TANGENT" -> {
                        attribute("tangent", VertexElementType.VECTOR4_FLOAT32)
                        accessors.add(accessor)
                    }
                    "TEXCOORD_0" -> {
                        val dimensions = when (accessor.type) {
                            "SCALAR" -> 1
                            "VEC2" -> 2
                            "VEC3" -> 3
                            else -> error("unsupported texture coordinate type ${accessor.type}")
                        }
                        textureCoordinate(dimensions, 0)
                        accessors.add(accessor)
                    }
                    "JOINTS_0" -> {
                        val type = when (Pair(accessor.type, accessor.componentType)) {
                            Pair("VEC4", GLTF_UNSIGNED_BYTE) -> VertexElementType.VECTOR4_UINT8
                            Pair("VEC4", GLTF_UNSIGNED_SHORT) -> VertexElementType.VECTOR4_UINT16
                            else -> error("not supported ${accessor.type} / ${accessor.componentType}")
                        }
                        attribute("joints", type)
                        accessors.add(accessor)
                    }
                    "WEIGHTS_0" -> {
                        val type = when (Pair(accessor.type, accessor.componentType)) {
                            Pair("VEC4", GLTF_FLOAT) -> VertexElementType.VECTOR4_FLOAT32
                            else -> error("not supported ${accessor.type} / ${accessor.componentType}")
                        }
                        attribute("weights", type)
                        accessors.add(accessor)
                    }
                }
            }
        }

        val buffers =
                accessors.map { it.bufferView }
                        .distinct()
                        .associate {
                            Pair(gltfFile.bufferViews[it].buffer,
                                    gltfFile.buffers[gltfFile.bufferViews[it].buffer].contents(gltfFile))
                        }

        val vb = vertexBuffer(format, maxCount)
        vb.put {
            for (i in 0 until maxCount) {
                for (a in accessors) {
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
                    copyBuffer(buffer, offset, size)
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

class GltfMesh(val primitives: List<GltfPrimitive>, val name: String) {
    fun createDrawCommands(gltfFile: GltfFile): List<GltfDrawCommand> {
        return primitives.map { it.createDrawCommand(gltfFile) }
    }
}

class GltfPbrMetallicRoughness(val baseColorFactor: DoubleArray?,
                               val baseColorTexture: GltfMaterialTexture?,
                               var metallicRoughnessTexture: GltfMaterialTexture?,
                               val roughnessFactor: Double?,
                               val metallicFactor: Double?)

class GltfMaterialTexture(val index: Int, val scale: Double?, val texCoord: Int?)

class GltfImage(val uri: String?, val bufferView: Int?)

class GltfSampler(val magFilter: Int, val minFilter: Int, val wrapS: Int, val wrapT: Int)

class GltfTexture(val sampler: Int, val source: Int)

class GltfMaterial(val name: String,
                   val alphaMode: String?,
                   val doubleSided: Boolean?,
                   val normalTexture: GltfMaterialTexture?,
                   val occlusionTexture: GltfMaterialTexture?,
                   val emissiveTexture: GltfMaterialTexture?,
                   val emissiveFactor: DoubleArray?,
                   val pbrMetallicRoughness: GltfPbrMetallicRoughness?,
                   val extensions: GltfMaterialExtensions?
)

class GltfMaterialExtensions(
        val KHR_materials_pbrSpecularGlossiness: KhrMaterialsPbrSpecularGlossiness?
)

class KhrMaterialsPbrSpecularGlossiness(val diffuseFactor: DoubleArray?, val diffuseTexture: GltfMaterialTexture?)

class GltfBufferView(val buffer: Int,
                     val byteOffset: Int?,
                     val byteLength: Int,
                     val byteStride: Int?,
                     val target: Int)

class GltfBuffer(val byteLength: Int, val uri: String?) {
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

class GltfDrawCommand(val vertexBuffer: VertexBuffer, val indexBuffer: IndexBuffer?, val primitive: DrawPrimitive, var vertexCount: Int)

class GltfAccessor(
        val bufferView: Int,
        val byteOffset: Int,
        val componentType: Int,
        val count: Int,
        val max: DoubleArray,
        val min: DoubleArray,
        val type: String
)

class GltfAnimation(val name: String?, val channels: List<GltfChannel>, val samplers: List<GltfAnimationSampler>)
class GltfAnimationSampler(val input: Int, val interpolation: String, val output: Int)

class GltfChannelTarget(val node: Int?, val path: String?)

class GltfChannel(val sampler: Int, val target: GltfChannelTarget)


class GltfSkin(val inverseBindMatrices: Int, val joints: IntArray, val skeleton: Int)

class GltfFile(
        val asset: GltfAsset?,
        val scene: Int?,
        val scenes: List<GltfScene>,
        val nodes: List<GltfNode>,
        val meshes: List<GltfMesh>,
        val accessors: List<GltfAccessor>,
        val materials: List<GltfMaterial>,
        val bufferViews: List<GltfBufferView>,
        val buffers: List<GltfBuffer>,
        val images: List<GltfImage>?,
        val textures: List<GltfTexture>?,
        val samplers: List<GltfSampler>?,
        val animations: List<GltfAnimation>?,
        val skins: List<GltfSkin>?
) {
    @Transient
    lateinit var file: File

    @Transient
    var bufferBuffer: ByteBuffer? = null
}

fun loadGltfFromFile(file: File): GltfFile = when (file.extension) {
    "gltf" -> {
        val gson = Gson()
        val json = file.readText()
        gson.fromJson(json, GltfFile::class.java).apply {
            this.file = file
        }
    }
    "glb" -> {
        loadGltfFromGlbFile(file)
    }
    else -> error("extension ${file.extension} not supported in ${file}")
}

