import org.openrndr.Fullscreen
import org.openrndr.application
import org.openrndr.draw.Filter
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.filterShaderFromCode
import org.openrndr.extra.depth.camera.DepthMeasurement
import org.openrndr.extra.depth.camera.calibrator.DepthCameraCalibrator
import org.openrndr.extra.depth.camera.calibrator.isolatedWithCalibration
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.kinect.v1.Kinect1

fun main() = application {
    configure {
        fullscreen = Fullscreen.CURRENT_DISPLAY_MODE
    }
    program {

        val kinect = extend(Kinect1())
        val device = kinect.openDevice()
        val camera = device.depthCamera
        // depth measurement in meters is required by the calibrator
        camera.depthMeasurement = DepthMeasurement.METERS
        val kinectResolution = camera.resolution

        val outputBuffer = colorBuffer(
            kinectResolution.x,
            kinectResolution.y
        )

        // simple visual effect applied to kinect data
        val spaceRangeExtractor = object : Filter(filterShaderFromCode("""
            uniform sampler2D   tex0;             // kinect raw
            uniform float       minDepth;
            uniform float       maxDepth;
            out     vec4        o_color;
            void main() {
                ivec2 uv = ivec2(gl_FragCoord.xy);
                float depth = texelFetch(tex0, uv, 0).r;
                float luma = ((depth >= minDepth) && (depth <= maxDepth)) ? 1.0 : 0.0;
                o_color = vec4(vec3(luma), 1.0);
            }
            """.trimIndent(),
            "space range extractor"
        )) {
            var minDepth: Double by parameters
            var maxDepth: Double by parameters
        }
        camera.onFrameReceived { frame ->
            spaceRangeExtractor.apply(frame, outputBuffer)
        }
        val calibrator = DepthCameraCalibrator(this, camera)

        val gui = GUI()
        calibrator.addControlsTo(gui)

        /*
         Note: remember that extend(gui) has to be called after all the parameter
         controls are added.

         Also extensions are rendered in reverse order, if we start with gui,
         it will not be covered by calibrator view when calibrator is enabled
         */
        extend(gui)

        /*
         if it's an interactive installation, probably we don't want to
         show GUI on startup. It can be shown by pressing F11.
         */
        gui.visible = false

        /*
         Registering this callback here after gui will prevent it from
         being triggered multiple times when GUI parameters are restored
         on startup.
         */
        calibrator.onCalibrationChange { calibration ->
            spaceRangeExtractor.minDepth = calibration.minDepth
            spaceRangeExtractor.maxDepth = calibration.maxDepth
        }
        extend(calibrator)
        camera.enabled = true

        extend {
            val calibration = calibrator.getCalibration(camera)
            drawer.isolatedWithCalibration(calibration) {
                image(
                    colorBuffer = outputBuffer,
                    position = calibration.position,
                    width = calibration.width,
                    height = calibration.height
                )
            }
        }

        // switching calibrator view on and off with keyboard
        program.keyboard.keyDown.listen {
            if (it.name == "k") {
                calibrator.enabled = !calibrator.enabled
            }
        }

    }

}
