package org.openrndr.extra.color.mixing

import org.openrndr.color.ColorRGBa

import org.openrndr.extra.color.tools.matchLinearity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestSpectral {
    @Test
    fun testIdentity() {
        val colors = listOf(ColorRGBa.RED, ColorRGBa.GREEN, ColorRGBa.BLUE)

        for (c in colors) {
            val r = linearToReflectance(c)
            assertEquals(38, r.size)
            val xyz = reflectanceToXYZ(r)
            val cp = xyz.toRGBa().matchLinearity(c)
            assertTrue(cp.toVector4().distanceTo(c.toVector4()) < 5E-3)
        }
    }

    @Test
    fun testSaunderson() {
        val c = saundersonCorrection(10.15, 0.0, 0.0)
        println(c)
    }

}