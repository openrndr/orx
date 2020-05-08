package org.openrndr.panel.elements

import kotlinx.coroutines.Job
import kotlinx.coroutines.yield
import org.openrndr.draw.Drawer
import org.openrndr.launch

class WatchDiv<T : Any>(private val watchList: List<T>, private val builder: WatchDiv<T>.(T) -> Unit) : Div(), DisposableElement {
    override var disposed: Boolean = false
    private var listState = emptyList<T>()
    private var watchJob: Job? = null

    override fun dispose() {
        super.dispose()
        for (child in children) {
            child.parent = null
            (child as? DisposableElement)?.dispose()
        }
        children.clear()
    }

    fun regenerate() {
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

    fun checkJob() {
        if (watchJob == null) {
            watchJob = (root() as Body).controlManager.program.launch {
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

fun <T : Any> Element.watchDiv(vararg classes: String, watchList: List<T>, builder: WatchDiv<T>.(T) -> Unit) {
    val wd = WatchDiv(watchList, builder)
    wd.classes.addAll(classes.map { ElementClass(it) })
    this.append(wd)
    wd.regenerate()
    wd.checkJob()
}
