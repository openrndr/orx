import java.io.BufferedInputStream
import java.io.File

fun systemPython(): String {
    val executable = if (System.getProperty("os.name").lowercase().contains("windows")) {
        "python.exe"
    } else {
        "python3"
    }
    return executable
}

fun venvPython(venv: File): String {
    val executable = if (System.getProperty("os.name").lowercase().contains("windows")) {
        "${venv.absolutePath}/Scripts/python.exe"
    } else {
        "${venv.absolutePath}/bin/python"
    }
    return executable
}


fun invokePython(arguments: List<String>, executable: String = systemPython()): String {

    println("invoking with executable $executable and arguments $arguments")

    val result: String
    val pb = ProcessBuilder()
        .let {
            it.command(listOf(executable) + arguments)
            //it.redirectError(File("python.error.txt"))
            it.inheritIO()
        }
        .start()
        .let {
//            val os = it.outputStream
//            val bw = os.bufferedWriter()
//            bw.write(input)
//            bw.flush()
//            bw.close()

            val `is` = it.inputStream
            val bis = BufferedInputStream(`is`)
            val br = bis.bufferedReader()
            result = br.readText().trim()
            val error = it.waitFor()
            if (error != 0) {
                error("Python invoke failed with error $error")
            }
        }

    return result
}

fun main() {
    if (!File("axidraw-venv").exists()) {
        invokePython(listOf("-m", "venv", "axidraw-venv"))
    }
    val python = venvPython(File("axidraw-venv"))
    invokePython(listOf("-m", "pip", "install", "https://cdn.evilmadscientist.com/dl/ad/public/AxiDraw_API.zip"), python)
}

/*
* val result = invokePython("python/script.py", "hello")
* println(result)
*
* TODO
* Check if axicli is available and if not install it automatically.
*
* Deal with Linux, Mac, Windows differences.
*
* How to launch a terminal window and keep it open?
*
* In Linux I'm using
* "xterm", "-hold", "-fullscreen", "-fs", "24", "-e", "axicli" ...
*
* Planned:
*
* # Check if axidraw-venv/ exists, otherwise run
* python3 -m venv axidraw-venv
*
* # Activate it
* source axidraw-venv/bin/activate
*
* # Check if axidraw-venv/bin/axicli exists, otherwise run
* python -m pip install https://cdn.evilmadscientist.com/dl/ad/public/AxiDraw_API.zip
*
* # Print location
* which axicli
*
*
* NOTE
* I do pipx install https://cdn.evilmadscientist.com/dl/ad/public/AxiDraw_API.zip --force
* on my system to have it system wide.
*/