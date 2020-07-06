package org.openrndr.extra.dnk3.gltf

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.dnk3.*
import org.openrndr.extra.keyframer.KeyframerChannelQuaternion
import org.openrndr.extra.keyframer.KeyframerChannelVector3
import org.openrndr.math.Matrix44
import org.openrndr.math.Quaternion
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.transform
import java.io.File
import java.nio.Buffer
import java.nio.ByteOrder
import kotlin.reflect.KMutableProperty0

class SceneAnimation(var channels: List<AnimationChannel>) {
    val duration: Double
        get() {
            return channels.maxBy { it.duration }?.duration ?: 0.0
        }
    fun applyToTargets(input: Double) {
        for (channel in channels) {
            channel.applyToTarget(input)
        }
    }
}

sealed class AnimationChannel {
    abstract val duration: Double
    abstract fun applyToTarget(input: Double)
}


class QuaternionChannel(val target: KMutableProperty0<Quaternion>,
                        val keyframer: KeyframerChannelQuaternion) : AnimationChannel() {
    override fun applyToTarget(input: Double) {
        target.set(keyframer.value(input) ?: Quaternion.IDENTITY)
    }

    override val duration: Double
        get() = keyframer.duration()
}

class Vector3Channel(val target: KMutableProperty0<Vector3>,
                     val keyframer: KeyframerChannelVector3, val default: Vector3) : AnimationChannel() {
    override fun applyToTarget(input: Double) {
        target.set(keyframer.value(input) ?: default)
    }

    override val duration: Double
        get() = keyframer.duration()
}

class GltfSceneNode : SceneNode() {
    var translation = Vector3.ZERO
    var scale = Vector3.ONE
    var rotation = Quaternion.IDENTITY

    override fun toString(): String {
        return "translation: $translation, scale: $scale, rotation: $rotation, children: ${children.size}, entities: ${entities} "
    }

    override var transform: Matrix44 = Matrix44.IDENTITY
        get() = transform {
            translate(translation)
            multiply(rotation.matrix.matrix44)
            scale(scale)
        } * field
}

class GltfSceneData(val scenes: List<List<SceneNode>>, val animations: List<SceneAnimation>)


/** Tools to convert GltfFile into a DNK3 scene */
fun GltfFile.buildSceneNodes(): GltfSceneData {
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
                    val cb = ColorBuffer.fromBuffer(localBuffer)
                    cb.generateMipmaps()
                    cb.filter(MinifyingFilter.LINEAR_MIPMAP_LINEAR, MagnifyingFilter.LINEAR)
                    cb.anisotropy = 100.0
                    localBuffer.limit(localBuffer.capacity())
                    cb
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
        material.name = this.name

        material.doubleSided = this.doubleSided ?: false
        material.transparent = this.alphaMode != null

        pbrMetallicRoughness?.let { pbr ->
            material.roughness = pbr.roughnessFactor ?: 1.0
            material.metalness = pbr.metallicFactor ?: 1.0

            material.color = ColorRGBa.WHITE
            pbr.baseColorFactor?.let {
                material.color = ColorRGBa(it[0], it[1], it[2], it[3])
            }

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
                occlusionTexture?.let { texture ->
                    val cb = images!![textures!![texture.index].source].createSceneImage()
                    cb.filter(MinifyingFilter.LINEAR_MIPMAP_LINEAR, MagnifyingFilter.LINEAR)
                    cb.wrapU = WrapMode.REPEAT
                    cb.wrapV = WrapMode.REPEAT
                    val sceneTexture = Texture(ModelCoordinates(texture = cb, pre = "x_texCoord.y = 1.0-x_texCoord.y;"), TextureTarget.AMBIENT_OCCLUSION)
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
        val material = materials?.getOrNull(material)?.createSceneMaterial() ?: PBRMaterial()
        return MeshPrimitive(geometry, material)
    }


    val sceneNodes = mutableMapOf<GltfNode, SceneNode>()
    fun GltfNode.createSceneNode(): SceneNode = sceneNodes.getOrPut(this) {
        val node = GltfSceneNode()
        node.name = name ?: ""
        node.translation = translation?.let { Vector3(it[0], it[1], it[2]) } ?: Vector3.ZERO
        node.scale = scale?.let { Vector3(it[0], it[1], it[2]) } ?: Vector3.ONE
        node.rotation = rotation?.let { Quaternion(it[0], it[1], it[2], it[3]) } ?: Quaternion.IDENTITY

        matrix?.let {
            node.transform = Matrix44.fromDoubleArray(it).transposed
        }
        for (child in children.orEmpty) {
            node.children.add(nodes[child].createSceneNode())
        }
        node
    }

    val sceneMeshes = mutableMapOf<GltfMesh, MeshBase>()
    fun GltfMesh.createSceneMesh(skin: GltfSkin?): MeshBase = sceneMeshes.getOrPut(this) {
        if (skin == null) {
            Mesh(primitives.map {
                it.createScenePrimitive()
            })
        } else {
            val joints = skin.joints.map { nodes[it].createSceneNode() }
            val skeleton = nodes[skin.skeleton].createSceneNode()
            val ibmAccessor = accessors[skin.inverseBindMatrices]
            val ibmBufferView = bufferViews[ibmAccessor.bufferView]
            val ibmBuffer = buffers[ibmBufferView.buffer]

            val ibmData = ibmBuffer.contents(this@buildSceneNodes)
            ibmData.order(ByteOrder.nativeOrder())
            (ibmData as Buffer).position(ibmAccessor.byteOffset + (ibmBufferView.byteOffset ?: 0))

            require(ibmAccessor.type == "MAT4")
            require(ibmAccessor.componentType == GLTF_FLOAT)
            require(ibmAccessor.count == joints.size)
            val ibms = (0 until ibmAccessor.count).map {
                val array = DoubleArray(16)
                for (i in 0 until 16) {
                    array[i] = ibmData.float.toDouble()
                }
                Matrix44.fromDoubleArray(array).transposed
            }

            SkinnedMesh(primitives.map {
                it.createScenePrimitive()
            }, joints, skeleton, ibms)
        }
    }

    fun GltfCamera.createSceneCamera(sceneNode: SceneNode): Camera {
        return when (type) {
            "perspective" -> {
                PerspectiveCamera(sceneNode).apply {
                    aspectRatio = perspective?.aspectRatio ?: aspectRatio
                    far = perspective?.zfar ?: far
                    near = perspective?.znear ?: near
                    fov = perspective?.yfov?.let { Math.toDegrees(it) } ?: fov
                }
            }
            "orthographic" -> {
                OrthographicCamera(sceneNode).apply {
                    xMag = orthographic?.xmag ?: xMag
                    yMag = orthographic?.ymag ?: yMag
                    near = orthographic?.znear ?: near
                    far = orthographic?.zfar ?: far
                }
            }
            else -> error("unsupported camera type: $type")
        }
    }

    val scenes = scenes.map { scene ->
        scene.nodes.map { node ->
            val gltfNode = nodes[node]
            val sceneNode = gltfNode.createSceneNode()
            sceneNode
        }
    }
    for ((gltfNode, sceneNode) in sceneNodes) {
        gltfNode.mesh?.let {
            val skin = gltfNode.skin?.let { (skins!!)[it] }
            sceneNode.entities.add(meshes[it].createSceneMesh(skin))
        }

        gltfNode.camera?.let {
            sceneNode.entities.add(cameras!![it].createSceneCamera(sceneNode))
        }

        gltfNode.extensions?.let { exts ->
            exts.KHR_lights_punctual?.let { lightIndex ->
                extensions?.KHR_lights_punctual?.lights?.get(lightIndex.light)?.let { light ->
                    val sceneLight = when (light.type) {
                        "point" -> {
                            PointLight()
                        }
                        "directional" -> {
                            DirectionalLight().apply {
                                shadows = Shadows.PCF()
                            }
                        }
                        "spot" -> {
                            SpotLight().apply {
                                innerAngle = Math.toDegrees(light.spot!!.innerConeAngle ?: 0.0)
                                outerAngle = Math.toDegrees(light.spot.outerConeAngle ?: Math.PI / 4.0)
                                shadows = Shadows.PCF()
                            }

                        }
                        else -> error("unsupported light type ${light.type}")
                    }
                    sceneLight.apply {
                        val lightColor = (light.color ?: doubleArrayOf(1.0, 1.0, 1.0))
                        color = ColorRGBa(lightColor[0], lightColor[1], lightColor[2])
                    }
                    sceneNode.entities.add(sceneLight)
                }
            }
        }
    }

    val sceneAnimations = animations?.map { animation ->
        val animationChannels = animation.channels.mapNotNull { channel ->
            val candidate = channel.target.node?.let { nodes[it] }?.createSceneNode() as? GltfSceneNode
            candidate?.let { sceneNode ->
                val sampler = animation.samplers[channel.sampler]

                val inputAccessor = accessors[sampler.input]
                val inputBufferView = bufferViews[inputAccessor.bufferView]
                val inputData = buffers[inputBufferView.buffer].contents(this)

                val outputAccessor = accessors[sampler.output]
                val outputBufferView = bufferViews[outputAccessor.bufferView]
                val outputData = buffers[outputBufferView.buffer].contents(this)

                inputData.order(ByteOrder.nativeOrder())
                outputData.order(ByteOrder.nativeOrder())

                require(inputAccessor.count == outputAccessor.count)
                when (channel.target.path) {
                    "scale", "translation" -> {
                        require(inputAccessor.type == "SCALAR")
                        require(outputAccessor.type == "VEC3")
                        val keyframer = KeyframerChannelVector3()
                        val inputOffset = (inputBufferView.byteOffset ?: 0) + (inputAccessor.byteOffset ?: 0)
                        val outputOffset = (outputBufferView.byteOffset ?: 0) + (outputAccessor.byteOffset ?: 0)
                        val inputStride = (inputBufferView.byteStride ?: 4)
                        val outputStride = (outputBufferView.byteStride ?: 12)

                        inputData.limit(inputData.capacity())
                        for (i in 0 until outputAccessor.count) {
                            val input = inputData.getFloat(inputOffset + i * inputStride).toDouble()
                            val outputX = outputData.getFloat(outputOffset + i * outputStride).toDouble()
                            val outputY = outputData.getFloat(outputOffset + i * outputStride + 4).toDouble()
                            val outputZ = outputData.getFloat(outputOffset + i * outputStride + 8).toDouble()
                            keyframer.add(input, Vector3(outputX, outputY, outputZ))
                        }
                        val target = if (channel.target.path == "translation") sceneNode::translation else sceneNode::scale
                        val default = if (channel.target.path == "translation") Vector3.ZERO else Vector3.ONE
                        Vector3Channel(target, keyframer, default)
                    }
                    "rotation" -> {
                        require(inputAccessor.type == "SCALAR")
                        require(outputAccessor.type == "VEC4") {
                            "${outputAccessor.type}"
                        }
                        val keyframer = KeyframerChannelQuaternion()
                        val inputOffset = (inputBufferView.byteOffset ?: 0) + (inputAccessor.byteOffset ?: 0)
                        val outputOffset = (outputBufferView.byteOffset ?: 0) + (outputAccessor.byteOffset ?: 0)
                        val inputStride = (inputBufferView.byteStride ?: 4)
                        val outputStride = (outputBufferView.byteStride ?: 16)
                        for (i in 0 until outputAccessor.count) {
                            val input = inputData.getFloat(inputOffset + i * inputStride).toDouble()
                            val outputX = outputData.getFloat(outputOffset + i * outputStride).toDouble()
                            val outputY = outputData.getFloat(outputOffset + i * outputStride + 4).toDouble()
                            val outputZ = outputData.getFloat(outputOffset + i * outputStride + 8).toDouble()
                            val outputW = outputData.getFloat(outputOffset + i * outputStride + 12).toDouble()
                            keyframer.add(input, Quaternion(outputX, outputY, outputZ, outputW))
                        }
                        QuaternionChannel(sceneNode::rotation, keyframer)
                    }
                    else -> error("unsupported path ${channel.target.path}")
                }
            }
        }
        SceneAnimation(animationChannels)
    }
    return GltfSceneData(scenes, sceneAnimations.orEmpty())
}

private val IntArray?.orEmpty: IntArray get() = this ?: IntArray(0)