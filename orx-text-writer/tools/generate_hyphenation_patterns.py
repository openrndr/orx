#!/usr/bin/env python3
"""
Generates the Kotlin hyphenation pattern data used by orx-text-writer's Hyphenator from the hyph-utf8 patterns
(https://github.com/hyphenation/tex-hyphen).

Usage:
    git clone --depth 1 https://github.com/hyphenation/tex-hyphen.git
    python3 tools/generate_hyphenation_patterns.py <path to tex-hyphen>

Only pattern sets that are available under a permissive licence or the LPPL are included, see ALLOWED below. The
original header of every pattern file (copyright, licence, authors) is kept in the generated file and in its `notice`.
"""
import os
import re
import sys

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "commonMain", "kotlin", "hyphenation")
PACKAGE = "org.openrndr.extra.textwriter.hyphenation"
CHUNK = 16000  # characters per string literal, keeps every JVM string constant below 64KB

# Licence names (from the YAML headers) that allow redistribution in a BSD-2-Clause project, GPL, LGPL and MPL
# licensed pattern sets are not included
ALLOWED = {"mit", "mit/x11", "bsd 3-clause licence", "unlicence", "public domain", "lppl"}
# Pattern files whose licence is given as text only, all of these are permissive notices
PERMISSIVE_TEXT = {"en-us", "bg", "nb", "nn", "fi", "eu", "sa"}

# The scripts of every pattern set, as names of org.openrndr.draw.font.internal.Script; None means multiple scripts
SCRIPTS = {
    "af": ["LATIN"], "be": ["CYRILLIC"], "bg": ["CYRILLIC"], "cop": ["COPTIC"], "cu": ["CYRILLIC"],
    "cy": ["LATIN"], "da": ["LATIN"], "de-1901": ["LATIN"], "de-1996": ["LATIN"], "de-ch-1901": ["LATIN"],
    "el-monoton": ["GREEK"], "el-polyton": ["GREEK"], "en-gb": ["LATIN"], "en-us": ["LATIN"], "es": ["LATIN"],
    "et": ["LATIN"], "eu": ["LATIN"], "fi": ["LATIN"], "fi-x-school": ["LATIN"], "fr": ["LATIN"], "fur": ["LATIN"],
    "ga": ["LATIN"], "gl": ["LATIN"], "grc": ["GREEK"], "it": ["LATIN"], "ka": ["GEORGIAN"], "kk": ["CYRILLIC"],
    "la": ["LATIN"], "la-x-classic": ["LATIN"], "la-x-liturgic": ["LATIN"], "lt": ["LATIN"],
    "mn-cyrl": ["CYRILLIC"], "mul-ethi": ["ETHIOPIC"], "nb": ["LATIN"], "nl": ["LATIN"], "nn": ["LATIN"],
    "oc": ["LATIN"], "pi": ["LATIN"], "pl": ["LATIN"], "pms": ["LATIN"], "pt": ["LATIN"], "rm": ["LATIN"],
    "sa": None, "sk": ["LATIN"], "sl": ["LATIN"], "sq": ["LATIN"], "tk": ["LATIN"], "uk": ["CYRILLIC"],
    "zh-latn-pinyin": ["LATIN"], "as": ["BENGALI"], "bn": ["BENGALI"], "gu": ["GUJARATI"], "hi": ["DEVANAGARI"],
    "kn": ["KANNADA"], "ml": ["MALAYALAM"], "mr": ["DEVANAGARI"], "or": ["ORIYA"], "pa": ["GURMUKHI"],
    "ta": ["TAMIL"], "te": ["TELUGU"],
    "ca": ["LATIN"], "eo": ["LATIN"], "hr": ["LATIN"], "hsb": ["LATIN"], "ia": ["LATIN"], "is": ["LATIN"],
    "kmr": ["LATIN"], "ru": ["CYRILLIC"], "sh-cyrl": ["CYRILLIC"], "sh-latn": ["LATIN"], "sv": ["LATIN"],
    "th": ["THAI"], "tr": ["LATIN"],
}

# hyphenmins for pattern files that do not declare them
DEFAULT_MINS = {"lt": (2, 2), "fi-x-school": (2, 2), "mul-ethi": (1, 1)}
INDIC = {"as", "bn", "gu", "hi", "kn", "ml", "mr", "or", "pa", "ta", "te"}


def header(path):
    lines = []
    with open(path, encoding="utf-8") as f:
        for line in f:
            if not line.startswith("%"):
                break
            lines.append(line[1:].rstrip("\n").rstrip())
    return lines


def licence_names(head):
    # the licence block runs from the "licence:" key to the next top level key
    block = []
    inside = False
    for line in head:
        if re.match(r"^ \S", line):
            inside = line.strip().startswith("licence:")
            continue
        if inside:
            block.append(line)
    return [n.strip() for n in re.findall(r"^\s*-?\s*name:\s*(.+)$", "\n".join(block), re.M)]


def is_allowed(tag, head):
    names = [n.lower() for n in licence_names(head)]
    if not names:
        return tag in PERMISSIVE_TEXT
    return any(n in ALLOWED for n in names)


def hyphenmins(tag, head):
    text = "\n".join(head)
    m = re.search(r"typesetting:\s*\n\s*left:\s*(\d+)\s*\n\s*right:\s*(\d+)", text)
    if m:
        return int(m.group(1)), int(m.group(2))
    if tag in INDIC:
        return 1, 1
    return DEFAULT_MINS.get(tag, (2, 2))


def title(head):
    for line in head:
        m = re.match(r"\s*title:\s*(.*)", line)
        if m:
            return m.group(1)
    return ""


def kotlin_string(s):
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$").replace("\n", "\\n") + '"'


def chunked(s):
    parts = [s[i:i + CHUNK] for i in range(0, len(s), CHUNK)] or [""]
    if len(parts) == 1:
        return kotlin_string(parts[0])
    return "listOf(\n" + ",\n".join("            " + kotlin_string(p) for p in parts) + "\n        ).joinToString(\"\")"


def object_name(tag):
    return "Patterns" + "".join(p.capitalize() for p in re.split(r"[-_]", tag))


def main(root):
    base = os.path.join(root, "hyph-utf8", "tex", "generic", "hyph-utf8", "patterns")
    os.makedirs(OUT, exist_ok=True)
    for f in os.listdir(OUT):
        if f.startswith("Patterns") and f.endswith(".kt"):
            os.remove(os.path.join(OUT, f))

    entries = []
    excluded = []
    for tex in sorted(os.listdir(os.path.join(base, "tex"))):
        tag = tex[len("hyph-"):-len(".tex")]
        head = header(os.path.join(base, "tex", tex))
        pat_path = os.path.join(base, "txt", "hyph-%s.pat.txt" % tag)
        if not os.path.exists(pat_path) or os.path.getsize(pat_path) == 0:
            continue
        if not is_allowed(tag, head):
            excluded.append((tag, "/".join(licence_names(head)) or "no licence"))
            continue
        if tag not in SCRIPTS:
            raise SystemExit("no script known for " + tag)

        with open(pat_path, encoding="utf-8") as f:
            patterns = " ".join(f.read().split())
        hyp_path = os.path.join(base, "txt", "hyph-%s.hyp.txt" % tag)
        exceptions = ""
        if os.path.exists(hyp_path):
            with open(hyp_path, encoding="utf-8") as f:
                exceptions = " ".join(f.read().split())
        notice = "\n".join(head)
        left, right = hyphenmins(tag, head)
        name = object_name(tag)

        with open(os.path.join(OUT, name + ".kt"), "w", encoding="utf-8") as out:
            out.write("// Generated by tools/generate_hyphenation_patterns.py from hyph-utf8 hyph-%s.tex, do not edit.\n" % tag)
            for line in head:
                out.write("//" + (" " + line if line else "") + "\n")
            out.write("\npackage %s\n\n" % PACKAGE)
            out.write("internal object %s : PatternData {\n" % name)
            out.write("    override val patterns: String\n        get() = %s\n\n" % chunked(patterns))
            out.write("    override val exceptions: String\n        get() = %s\n\n" % chunked(exceptions))
            out.write("    override val notice: String\n        get() = %s\n" % chunked(notice))
            out.write("}\n")
        entries.append((tag, title(head), left, right, SCRIPTS[tag], name))

    with open(os.path.join(OUT, "PatternSets.kt"), "w", encoding="utf-8") as out:
        out.write("// Generated by tools/generate_hyphenation_patterns.py, do not edit.\n\n")
        out.write("package %s\n\n" % PACKAGE)
        out.write("import org.openrndr.draw.font.internal.Script\n\n")
        out.write("/**\n * The hyphenation pattern sets that are available, derived from hyph-utf8.\n")
        out.write(" * Not included for licence reasons: %s.\n */\n" % ", ".join("%s (%s)" % e for e in excluded))
        out.write("val hyphenationPatternSets: List<HyphenationPatternSet> = listOf(\n")
        rows = []
        for tag, t, left, right, scripts, name in entries:
            s = "null" if scripts is None else "setOf(" + ", ".join("Script." + x for x in scripts) + ")"
            rows.append("    HyphenationPatternSet(%s, %s, %d, %d, %s) { %s }" % (kotlin_string(tag), kotlin_string(t), left, right, s, name))
        out.write(",\n".join(rows) + "\n)\n")
    print("generated %d pattern sets, excluded %d: %s" % (len(entries), len(excluded), excluded))


if __name__ == "__main__":
    main(sys.argv[1])
