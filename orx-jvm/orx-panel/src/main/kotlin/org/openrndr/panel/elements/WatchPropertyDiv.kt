package org.openrndr.panel.elements

import kotlinx.coroutines.Job
import kotlinx.coroutines.yield
import org.openrndr.draw.Drawer
import org.openrndr.launch
import kotlin.reflect.KMutableProperty0

class WatchPropertyDiv<T : Any>(
    private val watchProperty: KMutableProperty0<T>,
    private val builder: WatchPropertyDiv<T>.(T) -> Unit
) : Div() {

    private var propertyState = watchProperty.get()
    private var watchJob: Job? = null

    fun regenerate(force: Boolean = false) {
        var regenerate = force
        if (watchProperty.get() != propertyState) {
            regenerate = true
        }

        if (regenerate) {
            for (child in children) {
                child.parent = null
                child.close()
            }
            propertyState = watchProperty.get()
            children.clear()
            builder(propertyState)

            requestRedraw()
        }
    }

    fun checkJob() {
        if (watchJob == null) {
            watchJob = (root() as? Body)?.controlManager?.program?.launch {
                while (!disposed) {
                    regenerate()
                    yield()
                }
            }
        }
    }

    override fun draw(drawer: Drawer) {
        checkJob()
        super.draw(drawer)
    }

}

fun <T : Any> Element.watchPropertyDiv(
    vararg classes: String,
    watchProperty: KMutableProperty0<T>,
    builder: WatchPropertyDiv<T>.(T) -> Unit
) {
    val wd = WatchPropertyDiv(watchProperty, builder)
    wd.classes.addAll(classes.map { ElementClass(it) })
    this.append(wd)
    wd.regenerate(true)
    wd.checkJob()
}
