package jsyphon

import java.io.File
import java.util.ArrayList
import java.util.HashMap

object JSyphonServerList {
    val list: ArrayList<HashMap<String?, String?>?>?
        external get

    init {
        System.load(File("orx-syphon/src/main/kotlin/jsyphon/libJSyphon.jnilib").absolutePath)
    }
}