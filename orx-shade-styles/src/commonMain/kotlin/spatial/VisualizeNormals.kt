package org.openrndr.extra.shadestyles.spatial

import org.openrndr.draw.shadeStyle

val visualizeNormals by lazy {
    shadeStyle {
        fragmentTransform = """
        x_fill = vec4(normalize(v_viewNormal)*0.5+0.5, 1.0) ;
    """.trimIndent()
    }
}