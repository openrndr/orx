package org.openrndr.extra.dnk3

import org.openrndr.draw.Drawer

interface Feature {
    fun <T : Feature> update(
            drawer: Drawer,
            sceneRenderer: SceneRenderer,
            scene: Scene,
            feature: T,
            context: RenderContext
    )
}