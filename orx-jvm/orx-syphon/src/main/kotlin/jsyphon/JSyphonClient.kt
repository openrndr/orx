        package jsyphon

class JSyphonClient
{
    private var ptr: Long = 0

    val native = JSyphonNative.check()

    fun init() {
        ptr = init(null)
    }

    fun setApplicationName(appName: String?) {
        setApplicationName(ptr, appName)
    }

    fun setServerName(serverName: String?) {
        setServerName(ptr, serverName)
    }

    val isValid: Boolean
        get() = isValid(ptr)

    fun newFrameImageForContext(): JSyphonImage {
        val dict = newFrameDataForContext()
        val name = dict["name"] as Long?
        val width = dict["width"] as Double?
        val height = dict["height"] as Double?
        return JSyphonImage(name!!.toInt(), width!!.toInt(), height!!.toInt())
    }

    // Native method declarations
    external fun init(options: HashMap<String?, Any?>?): Long
    external fun setApplicationName(ptr: Long, appName: String?)
    external fun setServerName(ptr: Long, serverName: String?)
    external fun isValid(ptr: Long): Boolean

    @JvmOverloads
    external fun serverDescription(ptr: Long = this.ptr): HashMap<String?, String?>?

    @JvmOverloads
    external fun hasNewFrame(ptr: Long = this.ptr): Boolean

    @JvmOverloads
    external fun newFrameDataForContext(ptr: Long = this.ptr): HashMap<String, Any>

    @JvmOverloads
    external fun stop(ptr: Long = this.ptr)
}