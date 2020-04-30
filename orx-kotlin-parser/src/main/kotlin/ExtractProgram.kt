package org.openrndr.extra.kotlinparser

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.misc.Interval
import org.antlr.v4.runtime.tree.ParseTreeWalker
import org.openrndr.extra.kotlin.antlr.KotlinLexer
import org.openrndr.extra.kotlin.antlr.KotlinParser
import org.openrndr.extra.kotlin.antlr.KotlinParserBaseListener
import java.io.File

fun ParserRuleContext.verbatimText(marginLeft:Int = 0, marginRight:Int = 0): String {
    val startIndex = start.startIndex + marginLeft
    val stopIndex = stop.stopIndex - marginRight
    val interval = Interval(startIndex, stopIndex)
    return start.inputStream.getText(interval)
}

class ImportsExtractor : KotlinParserBaseListener() {
    var result: String? = null

    override fun enterImportList(ctx: KotlinParser.ImportListContext) {
        result = ctx.verbatimText()
    }
}

class LambdaExtractor(val lambdaName: String) : KotlinParserBaseListener() {
    var result: String? = null
    override fun enterAnnotatedLambda(ctx: KotlinParser.AnnotatedLambdaContext?) {
        val puec = ctx!!.parent!!.parent!!.parent!! as KotlinParser.PostfixUnaryExpressionContext
        val identifier = puec.primaryExpression().simpleIdentifier().Identifier().text
        if (identifier == lambdaName) {
            result = ctx.verbatimText(1, 1)
        }
    }
}
class ProgramSource(val imports: String, val programLambda: String)



fun extractProgram(source: String) : ProgramSource {
    val parser = KotlinParser(CommonTokenStream(KotlinLexer(CharStreams.fromString(source))))
    val root = parser.kotlinFile()

    val importsExtractor = ImportsExtractor()
    ParseTreeWalker.DEFAULT.walk(importsExtractor, root)

    val lambdaExtractor = LambdaExtractor("program")
    ParseTreeWalker.DEFAULT.walk(lambdaExtractor, root)

    return ProgramSource(importsExtractor.result!!, lambdaExtractor.result!!)
}
