package org.openrndr.extra.computeshaders

import io.kotest.matchers.shouldBe
import org.openrndr.math.IntVector2
import org.openrndr.math.IntVector3
import kotlin.test.Test

class TestComputeShaders {

    @Test
    fun testComputeShaderExecuteDimensionsFor2D() {

        computeShaderExecuteDimensionsFor2D(
            resolution = IntVector2(639, 480),
            localSizeX = 8,
            localSizeY = 8
        ) shouldBe IntVector3(80, 60, 1)

        computeShaderExecuteDimensionsFor2D(
            resolution = IntVector2(640, 480),
            localSizeX = 8,
            localSizeY = 8
        ) shouldBe IntVector3(80, 60, 1)

        computeShaderExecuteDimensionsFor2D(
            resolution = IntVector2(641, 480),
            localSizeX = 8,
            localSizeY = 8
        ) shouldBe IntVector3(81, 60, 1)

        computeShaderExecuteDimensionsFor2D(
            resolution = IntVector2(641, 481),
            localSizeX = 8,
            localSizeY = 8
        ) shouldBe IntVector3(81, 61, 1)

    }

}
