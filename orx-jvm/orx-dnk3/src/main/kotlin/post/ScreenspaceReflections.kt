package org.openrndr.extra.dnk3.post

import org.openrndr.draw.Filter
import org.openrndr.math.Matrix44
import org.openrndr.resourceUrl

class ScreenspaceReflections : Filter(preprocessedFilterShaderFromUrl(resourceUrl("/shaders/screenspace-reflections.frag"))) {
    var projection: Matrix44 by parameters
    var projectionMatrixInverse: Matrix44 by parameters

    var colors: Int by parameters
    var projDepth: Int by parameters
    var normals: Int by parameters

    var jitterOriginGain: Double by parameters
    var iterationLimit: Int by parameters
    var distanceLimit: Double by parameters
    var gain: Double by parameters
    var borderWidth: Double by parameters

    init {
        colors = 0
        projDepth = 1
        normals = 2

        projection = Matrix44.IDENTITY
        projectionMatrixInverse = Matrix44.IDENTITY

        distanceLimit = 100.0
        iterationLimit = 128
        jitterOriginGain = 0.0

        gain = 1.0
        borderWidth = 130.0
    }
}