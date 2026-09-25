import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession.SessionOptions
import java.io.File


fun main() {

    println(File(".").absolutePath)
    val env = OrtEnvironment.getEnvironment()

    val options = SessionOptions()

    options.addCoreML()

    println(env.epDevices.joinToString("\n "))
    println(env.version)
    val session = env.createSession("orx-jvm/orx-onnx-runtime/test-data/add_one.onnx", options)


    val inputTensor = OnnxTensor.createTensor(env, floatArrayOf(0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f))

    println(session.outputNames)
    val results = session.run(mapOf("input" to inputTensor))
    println(results["output"].get() as OnnxTensor)
    val  outputTensor = results["output"].get() as OnnxTensor
    val fb = outputTensor.floatBuffer
    for (i in 0 until 10) {
        println(fb[i])
    }

    session.close()
    env.close()
}