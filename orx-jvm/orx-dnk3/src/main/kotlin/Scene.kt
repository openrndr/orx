package org.openrndr.extra.dnk3

import org.openrndr.Dispatcher
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import java.util.*

class Scene(val root: SceneNode = SceneNode(), val dispatcher: Dispatcher = Dispatcher()) {
    val features = mutableListOf<Feature>()
    override fun hashCode(): Int {
        var result = root.hashCode()
        result = result * 31 + features.hashCode()
        return result
    }
    fun hash(): String = Base64.getEncoder().encodeToString(hashCode().toString().toByteArray())
}

open class SceneNode {
    var name: String = ""
    var entities: MutableList<Entity> = mutableListOf()
    var parent: SceneNode? = null
    open var transform = Matrix44.IDENTITY
    var worldTransform = Matrix44.IDENTITY
    val children = mutableListOf<SceneNode>()
    var disposed = false

    var userData: Any? = null
    var update: (() -> Unit)? = null

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + entities.hashCode()
//        result = 31 * result + (parent?.hashCode() ?: 0)
        result = 31 * result + transform.hashCode()
        result = 31 * result + worldTransform.hashCode()
        result = 31 * result + children.hashCode()
        result = 31 * result + disposed.hashCode()
        return result
    }
}

val SceneNode.worldPosition: Vector3
    get() {
        return (worldTransform * Vector4.UNIT_W).xyz
    }

class NodeContent<T>(val node: SceneNode, val content: T) {
    operator fun component1() = node
    operator fun component2() = content
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as NodeContent<*>
        if (node != other.node) return false
        if (content != other.content) return false
        return true
    }

    override fun hashCode(): Int {
        var result = node.hashCode()
        result = 31 * result + content.hashCode()
        return result
    }
}

fun SceneNode.visit(visitor: SceneNode.() -> Unit) {
    visitor()
    children.forEach { it.visit(visitor) }
}

fun <P> SceneNode.scan(initial: P, scanner: SceneNode.(P) -> P) {
    val p = scanner(initial)
    children.forEach { it.scan(p, scanner) }
}

fun SceneNode.findNodes(selector: SceneNode.() -> Boolean): List<SceneNode> {
    val result = mutableListOf<SceneNode>()
    visit {
        if (selector()) result.add(this)
    }
    return result
}

fun <P : Entity> SceneNode.findContent(selector: Entity.() -> P?): List<NodeContent<P>> {
    val result = mutableListOf<NodeContent<P>>()

    visit {
        entities.forEach {
            val s = it.selector()
            if (s != null) {
                result.add(NodeContent(this, s))
            }
        }
    }
    return result
}

