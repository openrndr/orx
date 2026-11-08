    import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import org.openrndr.panel.controlManager
import org.openrndr.panel.elements.*

/**
 * Demonstrates a console for a small command language over circles: `add <color> at <x> <y> as <name>`,
 * `remove <name>`, `move <name> to <x> <y>` and `list`. Tab completes the commands, the colors and the names of the
 * circles from a list under the caret, the rest of the best candidate shows dimmed and right takes it. Words that
 * are not understood are underlined while typing, errors show under their span in the log, and up and down go
 * through the commands that were given.
 */
fun main() = application {
    configure {
        width = 900
        height = 600
        windowResizable = true
    }
    program {
        val colors = mapOf("red" to ColorRGBa.fromHex("#ff6f61"), "green" to ColorRGBa.fromHex("#7fd18b"), "blue" to ColorRGBa.fromHex("#5ab0ff"), "yellow" to ColorRGBa.fromHex("#f0c75e"))
        class Circle(var position: Vector2, val color: ColorRGBa)
        val circles = linkedMapOf<String, Circle>()
        val commands = listOf("add", "remove", "move", "list", "clear")
        val keyword = ColorRGBa.fromHex("#c49bff")
        val number = ColorRGBa.fromHex("#f0c75e")

        /** the words of [text] with where they start */
        fun words(text: String) = Regex("""\S+""").findAll(text).map { it.range.first to it.value }.toList()

        /** what the word [index] of a command that starts with [first] can be */
        fun expected(first: String?, index: Int, previous: String?): List<Completion> = when {
            index == 0 -> commands.map { Completion(it, detail = "command") }
            first == "add" && index == 1 -> colors.keys.map { Completion(it, detail = "color", color = colors[it]) }
            first == "add" && index == 2 -> listOf(Completion("at"))
            first == "add" && index == 5 -> listOf(Completion("as"))
            (first == "remove" || first == "move") && index == 1 -> circles.keys.map { Completion(it, detail = "circle") }
            first == "move" && index == 2 -> listOf(Completion("to"))
            else -> emptyList()
        }

        /** the first word of [text] that is not understood, with what was expected there */
        fun firstError(text: String): Triple<Int, Int, String>? {
            val ws = words(text)
            val first = ws.firstOrNull()?.second
            for ((i, w) in ws.withIndex()) {
                val (start, word) = w
                val numeric = (first == "add" && i in 3..4) || (first == "move" && i in 3..4)
                val options = expected(first, i, ws.getOrNull(i - 1)?.second)
                when {
                    numeric && word.toDoubleOrNull() == null -> return Triple(start, start + word.length, "expected a number")
                    !numeric && first == "add" && i == 6 -> {}
                    !numeric && options.isEmpty() -> return Triple(start, start + word.length, "nothing expected after '${ws[i - 1].second}'")
                    !numeric && options.none { it.text == word } -> return Triple(start, start + word.length,
                        "expected ${options.joinToString(", ") { "'${it.text}'" }.let { if (options.size > 4) options.take(4).joinToString(", ") { o -> "'${o.text}'" } + ", …" else it }}")
                }
            }
            return null
        }

        val cm = controlManager {
            css("""
                .page { display: flex; flex-direction: column; width: 100%; height: 100% }
                .stage { flex-grow: 1; flex-basis: 0; background-color: transparent }
                .page > console { height: 240px; flex-shrink: 0 }
            """)
            layout {
                div("page") {
                    div("stage") { }
                    console {
                        info("Type a command, tab completes. Try: add red at 200 120 as sun")
                        completer = Completer { text, caret ->
                            // the word at the caret, and the words before it
                            val start = text.lastIndexOfAny(charArrayOf(' '), caret - 1) + 1
                            val before = words(text.substring(0, start))
                            Completions(start, caret, expected(before.firstOrNull()?.second, before.size, before.lastOrNull()?.second))
                        }
                        diagnose = { text ->
                            val ws = words(text)
                            val marks = ws.mapIndexedNotNull { i, (start, word) ->
                                when {
                                    i == 0 && word in commands -> TextMark(start, start + word.length, keyword)
                                    word in setOf("at", "as", "to") -> TextMark(start, start + word.length, keyword)
                                    word.toDoubleOrNull() != null -> TextMark(start, start + word.length, number)
                                    word in colors -> TextMark(start, start + word.length, colors[word])
                                    else -> null
                                }
                            }
                            // an error only in the words that are finished, the last one is still being typed
                            val error = firstError(text)?.takeIf { (_, end, _) -> end < text.length }
                            marks + listOfNotNull(error?.let { (a, b, _) -> TextMark(a, b, underline = ColorRGBa.fromHex("#ff7a6e")) })
                        }
                        events.submitted.listen { command ->
                            val ws = words(command).map { it.second }
                            val error = firstError(command)
                            when {
                                ws.isEmpty() -> {}
                                error != null -> error(error.third, error.first, error.second)
                                ws[0] == "list" -> if (circles.isEmpty()) info("no circles") else circles.forEach { (n, c) -> info("$n at ${c.position.x.toInt()} ${c.position.y.toInt()}") }
                                ws[0] == "clear" -> { circles.clear(); ok("cleared") }
                                ws[0] == "add" && ws.size == 7 -> {
                                    if (ws[6] in circles) error("'${ws[6]}' exists already", command.lastIndexOf(ws[6]), command.length)
                                    else { circles[ws[6]] = Circle(Vector2(ws[3].toDouble(), ws[4].toDouble()), colors.getValue(ws[1])); ok("added ${ws[6]}") }
                                }
                                ws[0] == "remove" && ws.size == 2 -> { circles.remove(ws[1]); ok("removed ${ws[1]}") }
                                ws[0] == "move" && ws.size == 5 -> { circles.getValue(ws[1]).position = Vector2(ws[3].toDouble(), ws[4].toDouble()); ok("moved ${ws[1]}") }
                                else -> error("the command is not finished", command.length, command.length)
                            }
                        }
                    }
                }
            }
        }
        extend {
            drawer.clear(ColorRGBa.fromHex("#16181c"))
            drawer.stroke = null
            for (c in circles.values) {
                drawer.fill = c.color
                drawer.circle(c.position, 30.0)
            }
        }
        extend(cm)
    }
}
