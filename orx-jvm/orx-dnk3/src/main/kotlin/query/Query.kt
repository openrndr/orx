package org.openrndr.extra.dnk3.query

import org.openrndr.extra.dnk3.Material
import org.openrndr.extra.dnk3.Mesh
import org.openrndr.extra.dnk3.Scene
import org.openrndr.extra.dnk3.SceneNode

fun Scene.findNodeByName(name: String): SceneNode? {
    return root.findNodeByName(name)
}

fun SceneNode.findNodeByName(name: String): SceneNode? {

    if (this.name == name) {
        return this
    } else {
        for (child in children) {
            val candidate = child.findNodeByName(name)
            if (candidate != null) {
                return candidate
            }
        }
    }
    return null
}

fun SceneNode.findMaterialByName(name: String): Material? {
    return allMaterials().find { it.name == name }
}

fun Scene.allMaterials(): Set<Material> {
    return root.allMaterials()
}

fun SceneNode.allMaterials(): Set<Material> {
    val materials = mutableSetOf<Material>()
    fun processNode(node: SceneNode) {
        for (entity in node.entities) {
            when (entity) {
                is Mesh -> {
                    materials.addAll(entity.primitives.map { it.material })
                }
                else -> {
                }
            }
        }

        for (child in node.children) {
            processNode(child)
        }
    }
    processNode(this)
    return materials
}


