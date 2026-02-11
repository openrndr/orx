package org.openrndr.panel.elements

import kotlinx.coroutines.Job
import kotlinx.coroutines.yield
import org.openrndr.draw.Drawer
import org.openrndr.launch

class WatchListDiv<T : Any>(private val watchList: List<T>, private val builder: WatchListDiv<T>.(T) -> Unit) : Div() {
    private var listState = emptyList<T>()
    private var watchJob: Job? = null


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
                child.close()
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

fun <T : Any> Element.watchListDiv(vararg classes: String, watchList: List<T>, builder: WatchListDiv<T>.(T) -> Unit) {
    val wd = WatchListDiv(watchList, builder)
    wd.classes.addAll(classes.map { ElementClass(it) })
    this.append(wd)
    wd.regenerate()
    wd.checkJob()
}
