package org.openrndr.panel

import org.openrndr.events.Event
import org.openrndr.panel.elements.Body
import org.openrndr.panel.elements.Element

/**
 * An edit in a [History]: [name] is what was done, [undo] and [redo] do it backward and forward again; edits of the
 * same [key] that follow each other closely are merged, like the values of a drag
 */
class Edit(val name: String, val key: Any?, internal var undo: () -> Unit, internal var redo: () -> Unit) {
    internal var time = 0L
    internal var gesture = 0
}

/**
 * The edits of a control manager that can be undone and redone: changes of the values of controls by the user, of
 * the keyframes of timelines, of the order of lists and of the layouts of docks. The commands Undo (mod+z) and Redo
 * (mod+shift+z) go through it. A history that is not [enabled] records nothing, like when the program has a history
 * of its own.
 */
class History {
    /** false to record nothing */
    var enabled = true

    /** the largest number of edits that are kept */
    var limit = 200

    private val done = ArrayDeque<Edit>()
    private val undone = ArrayDeque<Edit>()

    /** a press of the pointer starts a new gesture, edits of different gestures are not merged */
    internal var gesture = 0

    /** true while undoing or redoing, edits are not recorded then */
    private var replaying = false

    /** an edit was recorded, undone or redone */
    val changed = Event<History>("history-changed")

    val canUndo: Boolean get() = done.isNotEmpty()
    val canRedo: Boolean get() = undone.isNotEmpty()

    /** the name of the edit that [undo] undoes, null when there is none */
    val undoName: String? get() = done.lastOrNull()?.name

    /** the name of the edit that [redo] redoes, null when there is none */
    val redoName: String? get() = undone.lastOrNull()?.name

    /**
     * Records the change of a value from [old] to [new] that [apply] sets, merged with the previous edit of the same
     * [key] in the same gesture, or within half a second for keys
     */
    fun <T> record(name: String, key: Any?, old: T, new: T, apply: (T) -> Unit) {
        if (!enabled || replaying || old == new) return
        val now = System.currentTimeMillis()
        val last = done.lastOrNull()
        if (key != null && last != null && last.key == key && last.gesture == gesture && now - last.time < 500) {
            // the edit goes from the old value of the previous edit to the new value
            last.redo = { apply(new) }
            last.time = now
            undone.clear()
            changed.trigger(this)
            return
        }
        record(Edit(name, key, { apply(old) }, { apply(new) }))
    }

    /** records [edit] */
    fun record(edit: Edit) {
        if (!enabled || replaying) return
        edit.time = System.currentTimeMillis()
        edit.gesture = gesture
        done.addLast(edit)
        while (done.size > limit) done.removeFirst()
        undone.clear()
        changed.trigger(this)
    }

    /** undoes the last edit, false when there is none */
    fun undo(): Boolean {
        val edit = done.removeLastOrNull() ?: return false
        replay { edit.undo() }
        undone.addLast(edit)
        changed.trigger(this)
        return true
    }

    /** redoes the last edit that was undone, false when there is none */
    fun redo(): Boolean {
        val edit = undone.removeLastOrNull() ?: return false
        replay { edit.redo() }
        // a redone edit is not merged with the next one
        edit.gesture = -1
        done.addLast(edit)
        changed.trigger(this)
        return true
    }

    fun clear() {
        done.clear()
        undone.clear()
        changed.trigger(this)
    }

    private fun replay(action: () -> Unit) {
        replaying = true
        try {
            action()
        } finally {
            replaying = false
        }
    }
}

/** the history of the control manager of this element, null when it is not in one */
val Element.editHistory: History? get() = (root() as? Body)?.controlManager?.history

/** records the change of a value of this element by the user, see [History.record] */
internal fun <T> Element.recordEdit(name: String, old: T, new: T, apply: (T) -> Unit) {
    editHistory?.record(name, this, old, new, apply)
}
