package org.openrndr.extra.kdtree
import org.openrndr.math.Vector2
import org.openrndr.math.IntVector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import kotlin.jvm.JvmName

/** built-in mapper for [Vector2] */
fun vector2Mapper(v: Vector2, dimension: Int): Double {
    return when (dimension) {
        0 -> v.x
        else -> v.y
    }
}

fun intVector2Mapper(v: IntVector2, dimension: Int): Double {
    return when (dimension) {
        0 -> v.x.toDouble()
        else -> v.y.toDouble()
    }
}


/** built-in mapper for [Vector3] */
fun vector3Mapper(v: Vector3, dimension: Int): Double {
    return when (dimension) {
        0 -> v.x
        1 -> v.y
        else -> v.z
    }
}

/** built-in mapper for [Vector4] */
fun vector4Mapper(v: Vector4, dimension: Int): Double {
    return when (dimension) {
        0 -> v.x
        1 -> v.y
        2 -> v.z
        else -> v.w
    }
}



@JvmName("kdTreeVector2")
fun Iterable<Vector2>.kdTree(): KDTreeNode<Vector2> {
    val items = this.toMutableList()
    return buildKDTree(items, 2, ::vector2Mapper)
}

@JvmName("kdTreeVector3")
fun Iterable<Vector3>.kdTree(): KDTreeNode<Vector3> {
    val items = this.toMutableList()
    return buildKDTree(items, 3, ::vector3Mapper)
}

@JvmName("kdTreeVector4")
fun Iterable<Vector4>.kdTree(): KDTreeNode<Vector4> {
    val items = this.toMutableList()
    return buildKDTree(items, 4, ::vector4Mapper)
}