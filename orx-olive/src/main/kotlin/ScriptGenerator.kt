package org.openrndr.extra.olive

import org.openrndr.extra.kotlinparser.ProgramSource

inline fun <reified T> generateScript(programSource: ProgramSource): String {
    val script = """
        
//${programSource.packageName?:""}

import org.openrndr.extra.olive.OliveProgram
${programSource.imports}        

{ program: ${T::class.qualifiedName} ->
        program.apply {
            ${programSource.programLambda}                    
        }
}
"""
    return script
}