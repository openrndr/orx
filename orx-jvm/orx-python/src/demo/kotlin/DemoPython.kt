import org.openrndr.extra.python.createVirtualEnv
import org.openrndr.extra.python.systemPython

fun main() {

    val p = systemPython()
    val venv = p.createVirtualEnv("delete-me")

    println("version: '${venv.interpreter.version()}'")
    venv.remove()
}