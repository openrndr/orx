package org.openrndr.extra.dnk3.dsl

import kotlinx.coroutines.yield
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.VertexBuffer
import org.openrndr.extra.dnk3.*
import org.openrndr.launch

fun scene(builder: Scene.() -> Unit): Scene {
    val scene = Scene()
    scene.builder()
    return scene
}

fun SceneNode.node(builder: SceneNode.() -> Unit): SceneNode {
    val node = SceneNode()
    node.builder()
    children.add(node)
    return node
}

fun SceneNode.hemisphereLight(builder: HemisphereLight.() -> Unit): HemisphereLight {
    val hemisphereLight = HemisphereLight()
    hemisphereLight.builder()
    entities.add(hemisphereLight)
    return hemisphereLight
}

fun SceneNode.directionalLight(buider: DirectionalLight.() -> Unit): DirectionalLight {
    val directionalLight = DirectionalLight()
    directionalLight.buider()
    this.entities.add(directionalLight)
    return directionalLight
}

fun SceneNode.pointLight(builder: PointLight.() -> Unit): PointLight {
    val pointLight = PointLight()
    pointLight.builder()
    this.entities.add(pointLight)
    return pointLight
}

fun SceneNode.spotLight(builder: SpotLight.() -> Unit): SpotLight {
    val spotLight = SpotLight()
    spotLight.builder()
    this.entities.add(spotLight)
    return spotLight
}

class SimpleMeshBuilder {
    var vertexBuffer: VertexBuffer? = null
    var primitive = DrawPrimitive.TRIANGLES
    var material: Material? = null
    fun build(): Mesh {
        val geometry = Geometry(
                listOf(vertexBuffer ?: error("no vertex buffer")),
                null,
                primitive,
                0,
                vertexBuffer?.vertexCount ?: error("no vertex buffer")
        )
        val primitive = MeshPrimitive(geometry, material ?: error("no material"))
        return Mesh(listOf(primitive))
    }
}

fun SceneNode.simpleMesh(builder: SimpleMeshBuilder.() -> Unit): Mesh {
    val mesh = SimpleMeshBuilder().apply { builder() }.build()
    entities.add(mesh)
    return mesh
}


fun SceneNode.pathMesh(builder: PathMesh.() -> Unit): PathMesh {
    val pathMesh = PathMesh(mutableListOf(), DummyMaterial(), 1.0)
    pathMesh.builder()
    entities.add(pathMesh)
    return pathMesh
}

fun Scene.update(function: () -> Unit) {
    dispatcher.launch {
        while (true) {
            function()
            yield()
        }
    }
}
