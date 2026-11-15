package org.openrndr.extra.textwriter.math

/**
 * Reads a formula in presentation MathML, like `<math><mfrac><mi>a</mi><mn>2</mn></mfrac></math>`. Elements that are
 * not known are read as rows of their children. Character references and the common named entities are replaced.
 */
fun parseMathML(source: String): MathNode = MathMLReader(source).read()

/** an element of the XML, with its attributes and its children: elements and text */
private class XmlElement(val name: String, val attributes: Map<String, String>, val children: List<Any>) {
    val elements get() = children.filterIsInstance<XmlElement>()
    val text: String get() = children.joinToString("") { if (it is String) it else (it as XmlElement).text }
}

private class MathMLReader(private val source: String) {
    private var i = 0

    fun read(): MathNode {
        val roots = mutableListOf<Any>()
        while (i < source.length) {
            skipMisc()
            if (i >= source.length) break
            if (source[i] == '<') roots.add(element()) else roots.add(textUntilTag())
        }
        val element = roots.filterIsInstance<XmlElement>().singleOrNull() ?: error("expected one root element")
        return node(element)
    }

    // comments, processing instructions and doctypes
    private fun skipMisc() {
        while (true) {
            while (i < source.length && source[i].isWhitespace()) i++
            when {
                source.startsWith("<!--", i) -> i = source.indexOf("-->", i).let { if (it < 0) source.length else it + 3 }
                source.startsWith("<?", i) -> i = source.indexOf("?>", i).let { if (it < 0) source.length else it + 2 }
                source.startsWith("<!", i) -> i = source.indexOf('>', i).let { if (it < 0) source.length else it + 1 }
                else -> return
            }
        }
    }

    private fun element(): XmlElement {
        expect('<')
        val name = name()
        val attributes = mutableMapOf<String, String>()
        while (true) {
            while (source[i].isWhitespace()) i++
            if (source.startsWith("/>", i)) {
                i += 2
                return XmlElement(name, attributes, emptyList())
            }
            if (source[i] == '>') {
                i++
                break
            }
            val key = name()
            while (source[i].isWhitespace()) i++
            expect('=')
            while (source[i].isWhitespace()) i++
            val quote = source[i]
            require(quote == '"' || quote == '\'') { "expected a quoted attribute value at $i" }
            val end = source.indexOf(quote, i + 1)
            require(end >= 0) { "unterminated attribute value at $i" }
            attributes[key.substringAfter(':')] = decode(source.substring(i + 1, end))
            i = end + 1
        }
        val children = mutableListOf<Any>()
        while (true) {
            require(i < source.length) { "unterminated element <$name>" }
            when {
                source.startsWith("</", i) -> {
                    i += 2
                    val closing = name()
                    require(closing == name) { "expected </$name>, found </$closing>" }
                    while (source[i].isWhitespace()) i++
                    expect('>')
                    return XmlElement(name.substringAfter(':'), attributes, children)
                }
                source.startsWith("<!--", i) -> skipMisc()
                source[i] == '<' -> children.add(element())
                else -> children.add(textUntilTag())
            }
        }
    }

    private fun name(): String {
        val start = i
        while (i < source.length && (source[i].isLetterOrDigit() || source[i] in ":-_.")) i++
        require(i > start) { "expected a name at $start" }
        return source.substring(start, i)
    }

    private fun expect(c: Char) {
        require(i < source.length && source[i] == c) { "expected '$c' at $i" }
        i++
    }

    private fun textUntilTag(): String {
        val end = source.indexOf('<', i).let { if (it < 0) source.length else it }
        val text = source.substring(i, end)
        i = end
        return decode(text)
    }

    private fun decode(text: String): String {
        if ('&' !in text) return text
        val sb = StringBuilder()
        var k = 0
        while (k < text.length) {
            val c = text[k]
            val end = if (c == '&') text.indexOf(';', k) else -1
            if (end < 0) {
                sb.append(c)
                k++
                continue
            }
            val entity = text.substring(k + 1, end)
            val codePoint = when {
                entity.startsWith("#x") || entity.startsWith("#X") -> entity.substring(2).toIntOrNull(16)
                entity.startsWith("#") -> entity.substring(1).toIntOrNull()
                else -> entities[entity]
            }
            if (codePoint == null) {
                sb.append(text, k, end + 1)
            } else {
                sb.appendCodePoint(codePoint)
            }
            k = end + 1
        }
        return sb.toString()
    }

    private fun node(e: XmlElement): MathNode {
        val children = e.elements
        fun child(index: Int) = node(children.getOrElse(index) { error("<${e.name}> needs ${index + 1} children") })
        fun row() = children.map { node(it) }.let { it.singleOrNull() ?: MathNode.Row(it) }
        fun flag(name: String) = e.attributes[name]?.trim()?.lowercase()?.let { it == "true" }
        return when (e.name) {
            "math" -> MathNode.Formula(row(), e.attributes["display"] == "block" || e.attributes["mode"] == "display")
            "mrow", "mpadded", "mphantom", "merror", "semantics" -> if (e.name == "semantics") child(0) else row()
            "mstyle" -> MathNode.Style(
                row(),
                flag("displaystyle"),
                e.attributes["scriptlevel"]?.trim()?.toIntOrNull()
            )
            "mi" -> MathNode.Identifier(e.text.trim(), e.attributes["mathvariant"])
            "mn" -> MathNode.Number(e.text.trim())
            "mo" -> MathNode.Operator(
                e.text.trim(),
                form = e.attributes["form"],
                stretchy = flag("stretchy"),
                largeOperator = flag("largeop"),
                movableLimits = flag("movablelimits"),
                leftSpaceInEm = e.attributes["lspace"]?.let { lengthInEm(it) },
                rightSpaceInEm = e.attributes["rspace"]?.let { lengthInEm(it) }
            )
            "mtext", "ms" -> MathNode.Text(e.text.trim())
            "mspace" -> MathNode.Space(
                e.attributes["width"]?.let { lengthInEm(it) } ?: 0.0,
                e.attributes["height"]?.let { lengthInEm(it) } ?: 0.0,
                e.attributes["depth"]?.let { lengthInEm(it) } ?: 0.0
            )
            "mfrac" -> MathNode.Fraction(child(0), child(1), e.attributes["linethickness"]?.let { thickness(it) } ?: 1.0)
            "msqrt" -> MathNode.SquareRoot(row())
            "mroot" -> MathNode.Root(child(0), child(1))
            "msub" -> MathNode.Scripts(child(0), child(1), null)
            "msup" -> MathNode.Scripts(child(0), null, child(1))
            "msubsup" -> MathNode.Scripts(child(0), child(1), child(2))
            "munder" -> MathNode.UnderOver(child(0), child(1), null, accentUnder = flag("accentunder") == true)
            "mover" -> MathNode.UnderOver(child(0), null, child(1), accent = flag("accent") == true)
            "munderover" -> MathNode.UnderOver(child(0), child(1), child(2), flag("accent") == true, flag("accentunder") == true)
            else -> row()
        }
    }

    /** a length in em: em, ex (as half an em), and the named spaces of MathML; null for units it does not know */
    private fun lengthInEm(value: String): Double? {
        val v = value.trim()
        namedSpaces[v]?.let { return it }
        val number = Regex("""^-?[0-9]*\.?[0-9]+""").find(v)?.value ?: return null
        val n = number.toDouble()
        return when (v.removePrefix(number).trim()) {
            "em", "" -> n
            "ex" -> n * 0.5
            else -> null
        }
    }

    /** the factor of the default thickness of a fraction rule: 0, thin, medium, thick, or a multiple */
    private fun thickness(value: String): Double = when (val v = value.trim()) {
        "thin" -> 0.5
        "medium" -> 1.0
        "thick" -> 2.0
        else -> v.toDoubleOrNull() ?: if (v.startsWith("0")) 0.0 else 1.0
    }

    companion object {
        val namedSpaces = mapOf(
            "veryverythinmathspace" to 1 / 18.0, "verythinmathspace" to 2 / 18.0, "thinmathspace" to 3 / 18.0,
            "mediummathspace" to 4 / 18.0, "thickmathspace" to 5 / 18.0, "verythickmathspace" to 6 / 18.0,
            "veryverythickmathspace" to 7 / 18.0
        )

        val entities = mapOf(
            "lt" to '<'.code, "gt" to '>'.code, "amp" to '&'.code, "quot" to '"'.code, "apos" to '\''.code,
            "nbsp" to 0xA0, "minus" to 0x2212, "plusmn" to 0xB1, "times" to 0xD7, "divide" to 0xF7,
            "sdot" to 0x22C5, "middot" to 0xB7, "le" to 0x2264, "leq" to 0x2264, "ge" to 0x2265, "geq" to 0x2265,
            "ne" to 0x2260, "approx" to 0x2248, "equiv" to 0x2261, "rarr" to 0x2192, "larr" to 0x2190,
            "harr" to 0x2194, "rArr" to 0x21D2, "hArr" to 0x21D4, "isin" to 0x2208, "notin" to 0x2209,
            "sub" to 0x2282, "sup" to 0x2283, "sube" to 0x2286, "supe" to 0x2287, "cup" to 0x222A, "cap" to 0x2229,
            "infin" to 0x221E, "part" to 0x2202, "nabla" to 0x2207, "sum" to 0x2211, "prod" to 0x220F,
            "int" to 0x222B, "oint" to 0x222E, "radic" to 0x221A, "prime" to 0x2032, "Prime" to 0x2033,
            "InvisibleTimes" to 0x2062, "it" to 0x2062, "ApplyFunction" to 0x2061, "af" to 0x2061,
            "InvisibleComma" to 0x2063, "ic" to 0x2063, "lang" to 0x27E8, "rang" to 0x27E9,
            "lceil" to 0x2308, "rceil" to 0x2309, "lfloor" to 0x230A, "rfloor" to 0x230B,
            "alpha" to 0x3B1, "beta" to 0x3B2, "gamma" to 0x3B3, "delta" to 0x3B4, "epsilon" to 0x3B5,
            "zeta" to 0x3B6, "eta" to 0x3B7, "theta" to 0x3B8, "iota" to 0x3B9, "kappa" to 0x3BA,
            "lambda" to 0x3BB, "mu" to 0x3BC, "nu" to 0x3BD, "xi" to 0x3BE, "pi" to 0x3C0, "rho" to 0x3C1,
            "sigma" to 0x3C3, "tau" to 0x3C4, "upsilon" to 0x3C5, "phi" to 0x3C6, "chi" to 0x3C7, "psi" to 0x3C8,
            "omega" to 0x3C9, "Gamma" to 0x393, "Delta" to 0x394, "Theta" to 0x398, "Lambda" to 0x39B,
            "Xi" to 0x39E, "Pi" to 0x3A0, "Sigma" to 0x3A3, "Phi" to 0x3A6, "Psi" to 0x3A8, "Omega" to 0x3A9
        )
    }
}

private fun StringBuilder.appendCodePoint(codePoint: Int) {
    if (codePoint < 0x10000) {
        append(codePoint.toChar())
    } else {
        val c = codePoint - 0x10000
        append((0xD800 + (c shr 10)).toChar())
        append((0xDC00 + (c and 0x3FF)).toChar())
    }
}
