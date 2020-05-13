import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.compositor.*
import org.openrndr.extra.fx.blend.Darken
import org.openrndr.extra.parameters.Parameter
import org.openrndr.extra.parameters.ParameterType
import org.openrndr.extra.parameters.listParameters
import org.openrndr.extras.imageFit.FitMethod
import org.openrndr.extras.imageFit.imageFit
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import org.openrndr.math.mix
import org.rabbitcontrol.rcp.RCPServer
import org.rabbitcontrol.rcp.model.interfaces.IParameter
import org.rabbitcontrol.rcp.model.parameter.*
import org.rabbitcontrol.rcp.transport.websocket.server.WebsocketServerTransporterNetty
import java.awt.Color
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.reflect.KMutableProperty1


class RabbitControlServer(private val showQRUntilClientConnects: Boolean = true, rcpPort: Int = 10000, staticFilesPort: Int = 8080) : Extension {
    private val rabbitServer = RCPServer()
    private val transporter = WebsocketServerTransporterNetty()

    private var parameterMap = mutableMapOf<IParameter, Pair<Any, Parameter>>()

    private var qrCodeImage: ColorBuffer? = null
    private var qrOverlayComposition: Composite? = null


    /**
     * Animate the opacity to make it look smooooth
     */
    private var currentOpacity = 0.0

    private val targetOpacity: Double
        get() = if (shouldShowQR) 0.8 else 0.0

    private val shouldShowQR
        get() = (rabbitServer.connectionCount == 0 && showQRUntilClientConnects) || showQRCode


    /**
     * Used to manually show and hide the QR code and override the default
     * behaviour of (only) showing the code when no clients are connected
     */
    var showQRCode = false

    init {
        rabbitServer.addTransporter(transporter)
        transporter.bind(rcpPort)

        /**
         * Start KTOR to serve the static files of the RabbitControl client
         */
        val server = embeddedServer(Netty, port = staticFilesPort) {
            routing {
                static("/rabbit-client") {
                    resources("rabbit-client")
                }
            }
        }
        server.start()

        /**
         * Print the address
         */
        val socket = Socket()
        socket.connect(InetSocketAddress("google.com", 80))
        val ip = socket.localAddress.toString().replace("/", "")
        val clientUrlWithHash = "http://$ip:$staticFilesPort/rabbit-client/index.html/#$ip:$rcpPort"
        qrCodeImage = getQRCodeImage(barcodeText = clientUrlWithHash)
        println("RabbitControl Web Client: $clientUrlWithHash")

        /**
         * Update the object when it has been updated in RabbitControl
         */
        rabbitServer.setUpdateListener {
            val (obj, orxParameter) = parameterMap[it]!!
            when(it) {
                is Int32Parameter -> {
                    val v = it.value
                    orxParameter.property.qset(obj, v)
                }
                is Float64Parameter -> {
                    val v = it.value
                    orxParameter.property.qset(obj, v)
                }
                is BooleanParameter -> {
                    val v = it.value
                    orxParameter.property.qset(obj, v)
                }
                is StringParameter -> {
                    val v = it.value
                    orxParameter.property.qset(obj, v)
                }
                is RGBAParameter -> {
                    val c = it.value
                    val cc = ColorRGBa(c.red.toDouble() / 255.0, c.green.toDouble() / 255.0, c.blue.toDouble() / 255.0, c.alpha.toDouble() / 255.0)
                    orxParameter.property.qset(obj, cc)
                }
                is Vector2Float32Parameter -> {
                    val v = it.value
                    orxParameter.property.qset(obj, Vector2(v.x.toDouble(), v.y.toDouble()))
                }
                is Vector3Float32Parameter -> {
                    val v = it.value
                    orxParameter.property.qset(obj, Vector3(v.x.toDouble(), v.y.toDouble(), v.z.toDouble()))
                }
                is Vector4Float32Parameter -> {
                    val v = it.value
                    orxParameter.property.qset(obj, Vector4(v.x.toDouble(), v.y.toDouble(), v.z.toDouble(), v.t.toDouble()))
                }
            }
        }
    }


    override fun setup(program: Program) {
        /**
         * Creating the Composite for the overlay needs to happen in setup(),
         * as we need access to [Program.drawer]
         */
        qrOverlayComposition = compose {
            layer {
                draw {
                    program.drawer.isolated {
                        fill = ColorRGBa.WHITE.opacify(currentOpacity)
                        stroke = null
                        rectangle(0.0,0.0, width.toDouble(), height.toDouble())
                    }
                }

                layer {
                    blend(Darken()) {
                        clip = true
                    }

                    draw {
                        qrCodeImage?.let {
                            program.drawer.imageFit(it, program.width / 4.0,program.height / 4.0, program.width * .5, program.height * .5, 0.0,0.0, FitMethod.Contain)
                        }
                    }
                }
            }
        }
    }


    @Suppress("UNCHECKED_CAST")
    fun add(objectWithParameters: Any) {
        val parameters = objectWithParameters.listParameters()

        parameters.forEach {
            val rabbitParam = when (it.parameterType) {
                ParameterType.Int -> {
                    val param = rabbitServer.createInt32Parameter(it.label)
                    param.value = (it.property as KMutableProperty1<Any, Int>).get(objectWithParameters)
                    param
                }
                ParameterType.Double -> {
                    val param = rabbitServer.createFloat64Parameter(it.label)
                    param.value = (it.property as KMutableProperty1<Any, Double>).get(objectWithParameters)
                    param
                }
                ParameterType.Action -> {
                    val param = rabbitServer.createBangParameter(it.label)
                    param.setFunction {
                        it.function!!.call(objectWithParameters)
                    }
                    param
                }
                ParameterType.Boolean -> {
                    val param = rabbitServer.createBooleanParameter(it.label)
                    param.value = (it.property as KMutableProperty1<Any, Boolean>).get(objectWithParameters)
                    param
                }
                ParameterType.Text -> {
                    val param =rabbitServer.createStringParameter(it.label)
                    param.value = (it.property as KMutableProperty1<Any, String>).get(objectWithParameters)
                    param
                }
                ParameterType.Color -> {
                    val param = rabbitServer.createRGBAParameter(it.label)
                    val c = (it.property as KMutableProperty1<Any, ColorRGBa>).get(objectWithParameters)
                    param.value = Color(c.r.toFloat(), c.g.toFloat(), c.b.toFloat(), c.a.toFloat())
                    param
                }
                ParameterType.Vector2 -> {
                    val param = rabbitServer.createVector2Float32Parameter(it.label)
                    val v2 = (it.property as KMutableProperty1<Any, Vector2>).get(objectWithParameters)
                    param.value = org.rabbitcontrol.rcp.model.types.Vector2(v2.x.toFloat(), v2.y.toFloat())
                    param
                }
                ParameterType.Vector3 -> {
                    val param = rabbitServer.createVector3Float32Parameter(it.label)
                    val v3 = (it.property as KMutableProperty1<Any, Vector3>).get(objectWithParameters)
                    param.value = org.rabbitcontrol.rcp.model.types.Vector3(v3.x.toFloat(), v3.y.toFloat(), v3.z.toFloat())
                    param
                }
                ParameterType.Vector4 -> {
                    val param = rabbitServer.createVector4Float32Parameter(it.label)
                    val v4 = (it.property as KMutableProperty1<Any, Vector4>).get(objectWithParameters)
                    param.value = org.rabbitcontrol.rcp.model.types.Vector4(v4.x.toFloat(), v4.y.toFloat(), v4.z.toFloat(), v4.w.toFloat())
                    param
                }
                else -> rabbitServer.createBangParameter(it.label)
            }

            // We need to store a mapping from Rabbit parameter to target object + orx parameter
            // so we can update the object later
            parameterMap[rabbitParam] = Pair(objectWithParameters, it)
        }

        rabbitServer.update()
    }

    override var enabled = true

    override fun shutdown(program: Program) {
        transporter.dispose()
    }

    private fun getQRCodeImage(barcodeText: String): ColorBuffer {
        val qrCodeWriter = QRCodeWriter()
        val bitMatrix = qrCodeWriter.encode(barcodeText, BarcodeFormat.QR_CODE, 30, 30)
        val cb = colorBuffer(bitMatrix.width, bitMatrix.height)
        cb.filterMag = MagnifyingFilter.NEAREST
        val shad = cb.shadow


        for (y in 0 until bitMatrix.width) {
            for (x in 0 until bitMatrix.height) {
                shad[x, y] = if (bitMatrix[x, y]) ColorRGBa.BLACK else ColorRGBa.WHITE
            }
        }

        shad.upload()
        return cb
    }

    override fun afterDraw(drawer: Drawer, program: Program) {
        currentOpacity = mix(targetOpacity, currentOpacity, 0.8)

        // Don't draw if it isn't necessary
        if (currentOpacity > 0.0) {
            qrOverlayComposition?.draw(drawer)
        }
    }
}

fun <T> KMutableProperty1<out Any, Any?>?.qset(obj: Any, value: T) {
    @Suppress("UNCHECKED_CAST")
    return (this as KMutableProperty1<Any, T>).set(obj, value)
}
