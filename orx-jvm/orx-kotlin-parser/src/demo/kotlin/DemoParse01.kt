package demo

import KotlinLexer
import KotlinParser
import KotlinParserBaseListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTreeWalker
import org.openrndr.application
import org.openrndr.extra.kotlinparser.ImportsExtractor
import org.openrndr.extra.kotlinparser.PackageExtractor
import java.io.File

class AnnotationsExtractor : KotlinParserBaseListener() {
    var result = mutableListOf<String>()

    override fun enterAnnotation(ctx: KotlinParser.AnnotationContext?) {
        ctx?.let { result.add(ctx.text.trim()) }
    }
}

fun ownSourceFile(): File? {
    val fileName = Thread.currentThread().stackTrace[1].fileName ?: return null
    val projectRoot = File(System.getProperty("user.dir"))
    val match = projectRoot.walkTopDown()
        .firstOrNull { it.isFile && it.name == fileName }

    return match?.let { File(it.absolutePath) }
}

/**
 * A demo for getting started with orx-kotlin-parser
 *
 * This demo reads the source code of this file and
 * prints the package, imports, and annotations found in it.
 */
fun main() = application {
    program {
        val f = ownSourceFile() ?: error("Can't figure out own source file")

        val parser = KotlinParser(
            CommonTokenStream(
                KotlinLexer(CharStreams.fromString(f.readText()))
            )
        )

        @Deprecated("Testing")
        fun dep1() {}

        @Deprecated("Testing again")
        fun dep2() {}

        val root = parser.kotlinFile()
        val ruleNames = parser.ruleNames.toList()

        // package
        val packageExtractor = PackageExtractor()
        ParseTreeWalker.DEFAULT.walk(packageExtractor, root)
        println("[1. package]")
        println(packageExtractor.result ?: "")

        // imports
        val importsExtractor = ImportsExtractor(ruleNames)
        ParseTreeWalker.DEFAULT.walk(importsExtractor, root)
        println("[2. imports]")
        println(importsExtractor.result ?: "")

        // annotations
        val annotationsExtractor = AnnotationsExtractor()
        ParseTreeWalker.DEFAULT.walk(annotationsExtractor, root)
        println("[3. annotations]")
        println(annotationsExtractor.result)

        extend {}
    }
}