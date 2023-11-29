package org.openrndr.extra.kotlinparser

import KotlinLexer
import KotlinParser
import KotlinParserBaseListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.RuleContext
import org.antlr.v4.runtime.misc.Interval
import org.antlr.v4.runtime.tree.ParseTreeWalker


fun ParserRuleContext.verbatimText(marginLeft: Int = 0, marginRight: Int = 0): String {
    if (start == null || stop == null) {
        return ""
    }

    val startIndex = start.startIndex + marginLeft
    val stopIndex = stop.stopIndex - marginRight
    val interval = Interval(startIndex, stopIndex)
    return start.inputStream.getText(interval)
}

class PackageExtractor() : KotlinParserBaseListener() {
    var result: String? = null
    override fun enterPackageHeader(ctx: KotlinParser.PackageHeaderContext) {
        result = ctx.verbatimText()
    }
}

class ImportsExtractor(val ruleNames: List<String>) : KotlinParserBaseListener() {
    var result: String? = null

    override fun enterImportList(ctx: KotlinParser.ImportListContext) {
        result = ctx.verbatimText()
    }
}

class LambdaExtractor(val ruleNames: List<String>, val lambdaName: String) : KotlinParserBaseListener() {
    fun RuleContext.named(): String {
        return ruleNames[this.ruleIndex]
    }

    var result: String? = null
    override fun enterAnnotatedLambda(ctx: KotlinParser.AnnotatedLambdaContext?) {
        val puec = ctx?.parent?.parent?.parent as? KotlinParser.PostfixUnaryExpressionContext
        if (puec != null) {
            val identifier = puec.primaryExpression()?.simpleIdentifier()?.Identifier()?.text
            if (identifier == lambdaName) {
                if (result == null) {
                    result = ctx.verbatimText(1, 1)
                }
            }
        }
    }
}

class ProgramSource(val packageName: String?, val imports: String, val programLambda: String)

fun extractProgram(source: String, programIdentifier: String = "program"): ProgramSource {
    val parser = KotlinParser(CommonTokenStream(KotlinLexer(CharStreams.fromString(source))))
    val root = parser.kotlinFile()
//    val rules = parser.ruleNames.toList()
//    val pt = TreeUtils.toPrettyTree(root, rules)
    val ruleNames = parser.ruleNames.toList()

    val packageExtractor = PackageExtractor()
    ParseTreeWalker.DEFAULT.walk(packageExtractor, root)

    val importsExtractor = ImportsExtractor(ruleNames)
    ParseTreeWalker.DEFAULT.walk(importsExtractor, root)

    val lambdaExtractor = LambdaExtractor(ruleNames, programIdentifier)
    ParseTreeWalker.DEFAULT.walk(lambdaExtractor, root)
    return ProgramSource(packageExtractor.result, importsExtractor.result ?: "", lambdaExtractor.result ?: "")
}
