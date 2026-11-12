package org.openrndr.panel.elements

import org.openrndr.KEY_ENTER
import org.openrndr.events.Event
import org.openrndr.events.await
import org.openrndr.panel.ControlManager
import org.openrndr.panel.style.flexBasis
import org.openrndr.panel.style.px

/**
 * Two panes next to each other, or above each other when [vertical] is set, with a splitter between them that the
 * pointer drags; [position] is the size of the [first] pane in pixels, the [second] pane takes the rest. The panes are
 * `split-first` and `split-second`, the splitter is a `splitter`.
 */
class SplitPane(val vertical: Boolean = false) : Div(ElementType("split-pane")) {
    val first = Div().apply { classes.add(ElementClass("split-first")) }
    val splitter: Div = object : Div() { override val handlesDoubleClick = true }.apply { classes.add(ElementClass("splitter")) }
    val second = Div().apply { classes.add(ElementClass("split-second")) }

    /** the smallest size of a pane, in pixels */
    var minimum = 40.0

    /** the size of the first pane in pixels */
    var position: Double = 200.0
        set(value) {
            val size = if (vertical) layout.screenHeight else layout.screenWidth
            field = if (size > 2.0 * minimum) value.coerceIn(minimum, size - minimum) else value
            first.style { flexBasis = field.px }
            requestLayout()
        }

    private var dragging = false

    init {
        if (vertical) setAttribute("data-vertical", "")
        append(first)
        append(splitter)
        append(second)
        position = position
        splitter.mouse.pressed.listen {
            dragging = true
            it.cancelPropagation()
        }
        splitter.mouse.dragged.listen {
            if (dragging) {
                position = if (vertical) it.position.y - layout.screenY else it.position.x - layout.screenX
                it.cancelPropagation()
            }
        }
        splitter.mouse.released.listen { dragging = false }
        // a double click on the splitter puts it in the middle
        splitter.mouse.doubleClicked.listen {
            animatePosition((if (vertical) layout.screenHeight else layout.screenWidth) / 2.0)
            it.cancelPropagation()
        }
    }

    /** moves the splitter to [target] over [duration] seconds */
    fun animatePosition(target: Double, duration: Double = 0.25) {
        val cm = (root() as? Body)?.controlManager
        if (cm == null) { position = target; return }
        val from = position
        cm.animate(this, duration, key = "split") { t -> position = from + (target - from) * t }
    }

    /** builds the first pane */
    fun first(init: Div.() -> Unit) = first.init()

    /** builds the second pane */
    fun second(init: Div.() -> Unit) = second.init()
}

/** a [SplitPane], see there */
fun Element.splitPane(vararg classes: String, vertical: Boolean = false, init: SplitPane.() -> Unit): SplitPane =
    initElement(classes, SplitPane(vertical), init)

/**
 * A modal dialog, shown over a backdrop that covers the window, see [ControlManager.dialog]: the keyboard focus stays
 * in it, escape closes it, and so does a click on the backdrop when it is dismissable.
 */
class Dialog internal constructor(private val controlManager: ControlManager, val backdrop: Div, val box: Div) {
    /** happens when the dialog is closed */
    val closed = Event<Dialog>("dialog-closed")

    /** true while the dialog is shown */
    var open = true
        private set

    /** closes the dialog */
    fun close() {
        if (!open) return
        open = false
        controlManager.closeDialog(this)
        backdrop.parent?.remove(backdrop)
        backdrop.close()
        closed.trigger(this)
    }
}

/**
 * Shows a modal dialog that [init] builds, over a backdrop that covers the window: the keyboard focus moves into the
 * dialog and stays in it, escape closes it, and a click on the backdrop closes it when [dismissable] is set. The
 * backdrop is a `dialog-backdrop`, the dialog a `dialog` with [classes].
 */
fun ControlManager.dialog(vararg classes: String, dismissable: Boolean = true, init: Div.() -> Unit): Dialog {
    val body = body ?: error("no body")
    val backdrop = Div().apply { this.classes.add(ElementClass("dialog-backdrop")) }
    val box = Div().apply {
        this.classes.add(ElementClass("dialog"))
        classes.forEach { this.classes.add(ElementClass(it)) }
    }
    backdrop.append(box)
    box.init()
    body.append(backdrop)
    val dialog = Dialog(this, backdrop, box)
    // presses on the dialog stay in it, presses on the backdrop around it dismiss it
    box.mouse.pressed.listen { it.cancelPropagation() }
    box.mouse.clicked.listen { it.cancelPropagation() }
    backdrop.mouse.clicked.listen {
        if (dismissable && !box.containsScreenPoint(it.position)) dialog.close()
        it.cancelPropagation()
    }
    openDialog(dialog)
    return dialog
}

/**
 * Asks to confirm [message] in a modal dialog, under [title] when it is set, with the buttons [cancelLabel] and
 * [confirmLabel]. [onResult] is called once, when the dialog closes: with true when it is confirmed, false when it is
 * cancelled, by its button, escape, a click on the backdrop or [Dialog.close]. Enter presses the button with the
 * keyboard focus, which starts on the confirming button, or on the cancelling one when the action is [destructive].
 *
 * The dialog is a `dialog.confirm`, its buttons are in a `dialog-buttons`: a `.cancel-button` and a `.confirm-button`,
 * which is `.destructive` when [destructive] is set.
 */
fun ControlManager.confirm(
    message: String,
    title: String? = null,
    confirmLabel: String = "OK",
    cancelLabel: String = "Cancel",
    destructive: Boolean = false,
    onResult: (Boolean) -> Unit
): Dialog {
    var result = false
    lateinit var shown: Dialog
    lateinit var cancelButton: Button
    lateinit var confirmButton: Button
    fun answer(confirmed: Boolean) {
        result = confirmed
        shown.close()
    }
    shown = dialog("confirm") {
        title?.let { h3 { it } }
        p { message }
        div("dialog-buttons") {
            cancelButton = button("cancel-button", label = cancelLabel) { clicked { answer(false) } }
            val classes = listOfNotNull("confirm-button", "destructive".takeIf { destructive })
            confirmButton = button(*classes.toTypedArray(), label = confirmLabel) { clicked { answer(true) } }
        }
    }
    shown.closed.listen { onResult(result) }
    keyboardInput.target = if (destructive) cancelButton else confirmButton
    shown.box.keyboard.pressed.listen {
        if (it.key == KEY_ENTER) {
            answer(keyboardInput.target !== cancelButton)
            it.cancelPropagation()
        }
    }
    return shown
}

/**
 * Asks to confirm [message] like the [confirm] that takes a callback, and suspends until the dialog closes: true when it
 * is confirmed, false when it is cancelled. Cancelling the coroutine closes the dialog. For use in a coroutine on the
 * thread of the program, like one that `launch` starts.
 */
suspend fun ControlManager.confirm(
    message: String,
    title: String? = null,
    confirmLabel: String = "OK",
    cancelLabel: String = "Cancel",
    destructive: Boolean = false
): Boolean {
    var confirmed = false
    val dialog = confirm(message, title, confirmLabel, cancelLabel, destructive) { confirmed = it }
    try {
        dialog.closed.await()
    } finally {
        dialog.close()
    }
    return confirmed
}

/**
 * Shows a notification with [text] at the bottom right of the window, for [duration] seconds, see the styles of
 * `toast`; [classes] are added to it, like `error`
 */
fun ControlManager.toast(text: String, duration: Double = 3.0, vararg classes: String): Div {
    val body = body ?: error("no body")
    val container = body.children.firstOrNull { ElementClass("toasts") in it.classes }
        ?: Div().apply { this.classes.add(ElementClass("toasts")) }.also { body.append(it) }
    val toast = Div().apply {
        this.classes.add(ElementClass("toast"))
        classes.forEach { this.classes.add(ElementClass(it)) }
        p { text }
    }
    container.append(toast)
    scheduleToast(toast, duration)
    return toast
}
