package org.openrndr.extra.olive

import org.openrndr.extra.kotlinparser.ProgramSource

fun generateScript(programSource: ProgramSource): String {
    return """
@file:Suppress("UNUSED_LAMBDA_EXPRESSION")

${programSource.imports}        

{ program: Program ->
        program.apply {
            ${programSource.programLambda}                    
        }
}
"""
}