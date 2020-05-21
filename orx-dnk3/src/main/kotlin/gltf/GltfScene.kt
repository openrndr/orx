package org.openrndr.extra.dnk3.gltf

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.dnk3.*
import org.openrndr.math.Matrix44
import org.openrndr.math.Quaternion
import org.openrndr.math.transforms.transform
import java.io.File

/** Tools to convert GltfFile into a DNK3 scene */

fun GltfFile.buildSceneNodes(): List<List<SceneNode>> {

    val sceneImages = mutableMapOf<GltfImage, ColorBuffer>()
    fun GltfImage.createSceneImage(): ColorBuffer {
        return sceneImages.getOrPut(this) {
            if (uri == null) {

                bufferView?.let { bv ->
                    val localBufferView = bufferViews[bv]

                    val localBuffer = buffers[localBufferView.buffer].contents(this@buildSceneNodes)
                    require(localBufferView.byteOffset != null)
                    require(localBufferView.byteLength != null)
                    localBuffer.position(localBufferView.byteOffset)
                    localBuffer.limit(localBufferView.byteOffset + localBufferView.byteLength)
                    ColorBuffer.fromBuffer(localBuffer)
                } ?: error("no uri and no bufferview")

            } else {
                if (uri.startsWith("data:")) {
                    loadImage(uri)
                } else {
                    loadImage(File(file.parent, uri))
                }
            }
        }
    }

    val sceneMaterials = mutableMapOf<GltfMaterial, Material>()
    fun GltfMaterial.createSceneMaterial(): Material = sceneMaterials.getOrPut(this) {
        val material = PBRMaterial()

        material.doubleSided = this.doubleSided ?: false

        pbrMetallicRoughness?.let { pbr ->
            material.roughness = pbr.roughnessFactor ?: 1.0
            material.metalness = pbr.metallicFactor ?: 1.0
            pbr.baseColorTexture?.let { texture ->
                val cb = images!![textures!![texture.index].source].createSceneImage()
                cb.filter(MinifyingFilter.LINEAR_MIPMAP_LINEAR, MagnifyingFilter.LINEAR)
                cb.wrapU = WrapMode.REPEAT
                cb.wrapV = WrapMode.REPEAT
                val sceneTexture = Texture(ModelCoordinates(texture = cb, pre = "x_texCoord.y = 1.0-x_texCoord.y;"), TextureTarget.COLOR)
                material.textures.add(sceneTexture)
            }
            pbr.metallicRoughnessTexture?.let { texture ->
                val cb = images!![textures!![texture.index].source].createSceneImage()
                cb.filter(MinifyingFilter.LINEAR_MIPMAP_LINEAR, MagnifyingFilter.LINEAR)
                cb.wrapU = WrapMode.REPEAT
                cb.wrapV = WrapMode.REPEAT
                val sceneTexture = Texture(ModelCoordinates(texture = cb, pre = "x_texCoord.y = 1.0-x_texCoord.y;"), TextureTarget.METALNESS_ROUGHNESS)
                material.textures.add(sceneTexture)
            }

        }
        occlusionTexture?.let { texture ->
            val cb = images!![textures!![texture.index].source].createSceneImage()
            cb.filter(MinifyingFilter.LINEAR_MIPMAP_LINEAR, MagnifyingFilter.LINEAR)
            cb.wrapU = WrapMode.REPEAT
            cb.wrapV = WrapMode.REPEAT
            val sceneTexture = Texture(ModelCoordinates(texture = cb, pre = "x_texCoord.y = 1.0-x_texCoord.y;"), TextureTarget.AMBIENT_OCCLUSION)
            material.textures.add(sceneTexture)
        }

        normalTexture?.let { texture ->
            val cb = images!![textures!![texture.index].source].createSceneImage()
            cb.filter(MinifyingFilter.LINEAR_MIPMAP_LINEAR, MagnifyingFilter.LINEAR)
            cb.wrapU = WrapMode.REPEAT
            cb.wrapV = WrapMode.REPEAT

            val sceneTexture = Texture(ModelCoordinates(texture = cb, tangentInput = "va_tangent", pre = "x_texCoord.y = 1.0-x_texCoord.y;"), TextureTarget.NORMAL)
            material.textures.add(sceneTexture)
        }

        emissiveFactor?.let {
            material.emission = ColorRGBa(it[0], it[1], it[2])
        }

        emissiveTexture?.let {
            val cb = images!![textures!![it.index].source].createSceneImage()
            val sceneTexture = Texture(ModelCoordinates(texture = cb, pre = "x_texCoord.y = 1.0-x_texCoord.y;"), TextureTarget.EMISSION)
            material.textures.add(sceneTexture)
        }


        extensions?.let { ext ->

            ext.KHR_materials_pbrSpecularGlossiness?.let { sg ->
                sg.diffuseFactor?.let {
                    material.color = ColorRGBa(it[0], it[1], it[2], it[3])
                }
                sg.diffuseTexture?.let {
                    val cb = images!![textures!![it.index].source].createSceneImage()
                    cb.filter(MinifyingFilter.LINEAR_MIPMAP_LINEAR, MagnifyingFilter.LINEAR)
                    cb.wrapU = WrapMode.REPEAT
                    cb.wrapV = WrapMode.REPEAT
                    val sceneTexture = Texture(ModelCoordinates(texture = cb, pre = "x_texCoord.y = 1.0-x_texCoord.y;"), TextureTarget.COLOR)
                    material.textures.add(sceneTexture)
                }

            }


        }


        emissiveFactor?.let {
            material.emission = ColorRGBa(it[0], it[1], it[2], 1.0)
        }
        material
    }

    fun GltfPrimitive.createScenePrimitive(): MeshPrimitive {
        val drawCommand = createDrawCommand(this@buildSceneNodes)
        val geometry = Geometry(listOf(drawCommand.vertexBuffer),
                drawCommand.indexBuffer,
                drawCommand.primitive,
                0,
                drawCommand.vertexCount)
        val material = materials[material].createSceneMaterial()
        return MeshPrimitive(geometry, material)
    }

    val sceneMeshes = mutableMapOf<GltfMesh, Mesh>()
    fun GltfMesh.createSceneMesh(): Mesh = sceneMeshes.getOrPut(this) {
        Mesh(primitives.map {
            it.createScenePrimitive()
        })
    }

    fun GltfNode.createSceneNode(): SceneNode {
        val node = SceneNode()
        mesh?.let {
            node.entities.add(meshes[it].createSceneMesh())
        }

        val localTransform = transform {

            translation?.let {
                translate(it[0], it[1], it[2])
            }
            rotation?.let {
                val q = Quaternion(it[0], it[1], it[2], it[3])
                multiply(q.matrix.matrix44)
            }

            scale?.let {
                scale(it[0], it[1], it[2])
            }
        }

        node.transform = this.matrix?.let {
            Matrix44.fromDoubleArray(it).transposed
        } ?: localTransform
        for (child in children.orEmpty) {
            node.children.add(nodes[child].createSceneNode())
        }
        return node
    }

    return scenes.map { scene ->
        scene.nodes.map { node ->
            nodes[node].createSceneNode()
        }
    }
}

private val IntArray?.orEmpty: IntArray get() = this ?: IntArray(0)