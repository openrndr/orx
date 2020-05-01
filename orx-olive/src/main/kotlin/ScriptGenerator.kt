package org.openrndr.extra.olive

import org.openrndr.extra.kotlinparser.ProgramSource

inline fun <reified T> generateScript(programSource: ProgramSource): String {
    val script = """
@file:Suppress("UNUSED_LAMBDA_EXPRESSION")

import org.openrndr.extra.olive.OliveProgram
${programSource.imports}        

{ program: ${T::class.simpleName} ->
        program.apply {
            ${programSource.programLambda}                    
        }
}
"""
    println(script)
    return script
}