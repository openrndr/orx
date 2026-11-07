package org.openrndr.panel

import org.openrndr.KEY_ARROW_DOWN
import org.openrndr.KEY_ARROW_UP
import org.openrndr.KEY_ENTER
import org.openrndr.KeyEvent
import org.openrndr.KeyModifier
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.panel.elements.*
import org.openrndr.panel.style.effectiveColor
import org.openrndr.shape.Rectangle

private val mac = System.getProperty("os.name").lowercase().contains("mac")

/**
 * A keyboard shortcut: [key] is the name of a key, like `k`, `f5` or `escape`, held with [modifiers]. Written as
 * `mod+shift+k`, where `mod` is command on macOS and control elsewhere, `cmd`, `ctrl`, `alt` and `shift` the others.
 */
data class Shortcut(val key: String, val modifiers: Set<KeyModifier>) {
    /** true when [event] is the shortcut */
    fun matches(event: KeyEvent): Boolean = event.name.lowercase() == key && event.modifiers == modifiers

    /**
     * the shortcut as it is shown, like Cmd+Shift+K on macOS and Ctrl+Shift+K elsewhere; in words, since the fonts of
     * panels need not have the symbols of the modifier keys
     */
    override fun toString(): String {
        val k = if (key.length == 1) key.uppercase() else key.replaceFirstChar { it.uppercase() }
        return (listOfNotNull(
            "Ctrl".takeIf { KeyModifier.CTRL in modifiers },
            (if (mac) "Option" else "Alt").takeIf { KeyModifier.ALT in modifiers },
            "Shift".takeIf { KeyModifier.SHIFT in modifiers },
            (if (mac) "Cmd" else "Super").takeIf { KeyModifier.SUPER in modifiers }
        ) + k).joinToString("+")
    }

    companion object {
        /** the shortcut [text], like `mod+s`; null when it is not one */
        fun parse(text: String): Shortcut? {
            val parts = text.lowercase().split('+').map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.isEmpty()) return null
            val modifiers = parts.dropLast(1).map {
                when (it) {
                    "mod" -> if (mac) KeyModifier.SUPER else KeyModifier.CTRL
                    "cmd", "command", "super", "meta" -> KeyModifier.SUPER
                    "ctrl", "control" -> KeyModifier.CTRL
                    "alt", "option", "opt" -> KeyModifier.ALT
                    "shift" -> KeyModifier.SHIFT
                    else -> return null
                }
            }.toSet()
            return Shortcut(parts.last(), modifiers)
        }
    }
}

/** a command: what it is called, its [shortcut], the [group] it is in, like a menu, and what it does */
class Command(val name: String, val shortcut: Shortcut?, val group: String?, val action: () -> Unit) {
    /** the name with its group, as the palette shows it */
    val title: String get() = if (group == null) name else "$group: $name"
}

/**
 * The commands of a control manager: actions with names and keyboard shortcuts, which the command palette finds by
 * their names, see [ControlManager.commandPalette]. Menus of a menu bar add their items as commands.
 */
class Commands {
    private val list = mutableListOf<Command>()

    /** the commands, in the order they were added */
    val all: List<Command> get() = list

    /**
     * Adds the command [name] that calls [action], when the keys of [shortcut] are pressed too, like `mod+s`; a
     * command with the same name and group replaces it
     */
    fun add(name: String, shortcut: String? = null, group: String? = null, action: () -> Unit): Command {
        list.removeAll { it.name == name && it.group == group }
        return Command(name, shortcut?.let { Shortcut.parse(it) ?: error("not a shortcut: '$it'") }, group, action).also { list.add(it) }
    }

    fun remove(command: Command) {
        list.remove(command)
    }

    /** runs the command of the shortcut of [event], true when there is one */
    fun handle(event: KeyEvent): Boolean {
        val command = list.lastOrNull { it.shortcut?.matches(event) == true } ?: return false
        command.action()
        return true
    }

    /**
     * The commands that match [query], best first: those whose titles have the characters of the query in order, the
     * more of them at the starts of words and next to each other, the better
     */
    fun search(query: String): List<Command> {
        if (query.isBlank()) return list
        return list.mapNotNull { command -> fuzzyScore(query.lowercase(), command.title.lowercase())?.let { command to it } }
            .sortedByDescending { it.second }.map { it.first }
    }
}

/** how well [query] matches [text], null when its characters are not in it in order */
internal fun fuzzyScore(query: String, text: String): Int? {
    var score = 0
    var at = 0
    var previous = -2
    for (c in query) {
        if (c == ' ') continue
        val i = text.indexOf(c, at)
        if (i < 0) return null
        if (i == previous + 1) score += 5
        if (i == 0 || !text[i - 1].isLetterOrDigit()) score += 8
        score -= (i - at).coerceAtMost(10)
        previous = i
        at = i + 1
    }
    return score
}

/**
 * Opens the command palette: a dialog with a field that searches the commands by their names, see [Commands.search];
 * up and down choose a command, enter runs it, escape closes the palette. Opened by mod+k.
 */
fun ControlManager.commandPalette(): Dialog {
    lateinit var dialog: Dialog
    lateinit var input: TextInput
    lateinit var list: VirtualList<Command>
    var chosen = 0
    fun results() = commands.search(input.value)
    // the row of the chosen command is :selected
    fun mark() = list.children.flatMap { it.children }.filterIsInstance<CommandRow>().forEach { row ->
        if (row.index == chosen) row.pseudoClasses.add(ElementPseudoClass("selected"))
        else row.pseudoClasses.remove(ElementPseudoClass("selected"))
    }
    fun show() {
        list.items = results()
        chosen = 0
    }
    dialog = dialog("command-palette") {
        input = TextInput().also { append(it) }
        list = VirtualList(commands.all, 28.0) { command, index ->
            append(CommandRow(command, index) {
                dialog.close()
                command.action()
            }.also { row -> if (index == chosen) row.pseudoClasses.add(ElementPseudoClass("selected")) })
        }.also { append(it) }
    }
    input.events.valueChanged.listen { show() }
    input.keyboard.pressed.listen {
        val results = list.items
        when (it.key) {
            KEY_ARROW_DOWN -> chosen = (chosen + 1).coerceAtMost(results.size - 1)
            KEY_ARROW_UP -> chosen = (chosen - 1).coerceAtLeast(0)
            KEY_ENTER -> {
                val command = results.getOrNull(chosen) ?: return@listen
                dialog.close()
                command.action()
                it.cancelPropagation()
                return@listen
            }
            else -> return@listen
        }
        // the chosen command is in view
        val top = chosen * list.rowHeight
        if (top < list.scrollTop) list.scrollTo(top = top, smooth = false)
        else if (top + list.rowHeight > list.scrollTop + list.layout.screenHeight) list.scrollTo(top = top + list.rowHeight - list.layout.screenHeight, smooth = false)
        mark()
        it.cancelPropagation()
    }
    keyboardInput.target = input
    return dialog
}

/** a command in the palette, its title and its shortcut */
private class CommandRow(val command: Command, val index: Int, val run: () -> Unit) : Div(ElementType("command-row")) {
    init {
        mouse.pressed.listen { it.cancelPropagation() }
        mouse.clicked.listen { run(); it.cancelPropagation() }
    }

    override fun draw(drawer: Drawer) {
        super.draw(drawer)
        val text = panelText ?: return
        val color = computedStyle.effectiveColor ?: ColorRGBa.WHITE
        val h = layout.screenHeight
        val w = layout.screenWidth
        text.drawInBox(drawer, this, "title", computedStyle, command.title, Rectangle(10.0, 0.0, w - 110.0, h), 0.0, 0.5, color)
        command.shortcut?.let {
            text.drawInBox(drawer, this, "shortcut", computedStyle, it.toString(), Rectangle(w - 100.0, 0.0, 90.0, h), 1.0, 0.5, color.opacify(0.6))
        }
    }
}
