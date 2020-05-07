package org.openrndr.panel.elements

import kotlinx.coroutines.Job
import kotlinx.coroutines.yield
import org.openrndr.draw.Drawer
import org.openrndr.launch

class WatchDiv<T : Any>(val watchList: List<T>, val builder: WatchDiv<T>.(T) -> Unit) : Div(), DisposableElement {
    override var disposed: Boolean = false
    var listState = emptyList<T>()
    var watchJob : Job? = null

    override fun dispose() {
        for (child in children) {
            child.parent = null
            (child as? DisposableElement)?.dispose()
        }
        children.clear()
    }

    private fun regenerate() {
        var regenerate = false
        if (listState.size != watchList.size) {
            regenerate = true
        }
        if (!regenerate) {
            for (i in watchList.indices) {
                if (watchList[i] !== listState[i]) {
                    regenerate = true
                    break
                }
            }
        }
        if (regenerate) {
            for (child in children) {
                child.parent = null
                (child as? DisposableElement)?.dispose()
            }
            children.clear()
            listState = watchList.map { it }
            for (i in watchList) {
                builder(i)
            }
            requestRedraw()
        }
    }

    override fun draw(drawer: Drawer) {
        if (watchJob == null) {
            watchJob = (root() as Body).controlManager.program.launch {
                while (!disposed) {
                    regenerate()
                    yield()
                }
            }
        }
        super.draw(drawer)
    }
}

fun <T : Any> Element.watchDiv(vararg classes: String, watchList: List<T>, builder: WatchDiv<T>.(T) -> Unit) {
    val wd = WatchDiv(watchList, builder)
    wd.classes.addAll(classes.map { ElementClass(it) })
    this.append(wd)
}
