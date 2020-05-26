package org.openrndr.extra.dnk3

import org.openrndr.math.Matrix44
import org.openrndr.math.transforms.ortho
import org.openrndr.math.transforms.perspective

class PerspectiveCamera(var node: SceneNode) : Camera() {
    override val projectionMatrix: Matrix44
        get() = perspective(fov, aspectRatio, near, far)

    override val viewMatrix: Matrix44
        get() = node.worldTransform.inversed

    var aspectRatio: Double = 16.0 / 9.0
    var fov = 45.0
    var far = 100.0
    var near = 0.1

}

class OrthographicCamera(var node: SceneNode) : Camera() {
    override val projectionMatrix: Matrix44
        get() = ortho(xMag, yMag, near, far)

    override val viewMatrix: Matrix44
        get() = node.worldTransform.inversed

    var xMag = 1.0
    var yMag = 1.0
    var near = 0.1
    var far = 100.0
}