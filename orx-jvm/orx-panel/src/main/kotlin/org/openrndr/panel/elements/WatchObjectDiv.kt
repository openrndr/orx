package org.openrndr.panel.elements

import kotlinx.coroutines.Job
import kotlinx.coroutines.yield
import org.openrndr.draw.Drawer
import org.openrndr.launch
import org.openrndr.panel.hash.watchHash

class WatchObjectDiv<T:Any>(
    val watchObject: T,
    private val builder: WatchObjectDiv<T>.(T) -> Unit
) : Div(),
    DisposableElement {
    override var disposed: Boolean = false
    private var objectStateHash = watchHash(watchObject)
    private var watchJob: Job? = null


    override fun dispose() {
        super.dispose()
        for (child in children) {
            child.parent = null
            (child as? DisposableElement)?.dispose()
        }
        children.clear()
    }

    fun regenerate(force: Boolean = false) {
        var regenerate = force
        if (watchHash(watchObject) != objectStateHash) {
            regenerate = true
        }

        if (regenerate) {
            for (child in children) {
                child.parent = null
                (child as? DisposableElement)?.dispose()
            }
            objectStateHash = watchHash(watchObject)
            children.clear()
            builder(watchObject)

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

fun <T : Any> Element.watchObjectDiv(
    vararg classes: String,
    watchObject: T,
    builder: WatchObjectDiv<T>.(T) -> Unit
) : WatchObjectDiv<T> {
    val wd = WatchObjectDiv(watchObject, builder)
    wd.classes.addAll(classes.map { ElementClass(it) })
    this.append(wd)
    wd.regenerate(true)
    wd.checkJob()
    return wd
}
