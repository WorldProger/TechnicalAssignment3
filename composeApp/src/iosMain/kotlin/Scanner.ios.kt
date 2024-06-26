import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.useContents
import kotlinx.cinterop.value
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureVideoOrientationLandscapeLeft
import platform.AVFoundation.AVCaptureVideoOrientationLandscapeRight
import platform.AVFoundation.AVCaptureVideoOrientationPortrait
import platform.AVFoundation.AVCaptureVideoOrientationPortraitUpsideDown
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectType
import platform.AVFoundation.AVMetadataObjectTypeAztecCode
import platform.AVFoundation.AVMetadataObjectTypeCodabarCode
import platform.AVFoundation.AVMetadataObjectTypeCode128Code
import platform.AVFoundation.AVMetadataObjectTypeCode39Code
import platform.AVFoundation.AVMetadataObjectTypeCode93Code
import platform.AVFoundation.AVMetadataObjectTypeITF14Code
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.AudioToolbox.AudioServicesPlaySystemSound
import platform.CoreGraphics.CGRect
import platform.Foundation.NSError
import platform.QuartzCore.CALayer
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCATransactionDisableActions
import platform.UIKit.UIColor
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceOrientation
import platform.UIKit.UIView
import platform.darwin.NSObject
import platform.darwin.dispatch_get_main_queue

@Composable
actual fun Scanner(
    modifier: Modifier,
    onScanned: (String) -> Boolean,
) {
    IosScannerView(
        modifier = modifier,
        onScanned = onScanned,
    )
}

@OptIn(ExperimentalForeignApi::class)
@Composable
fun IosScannerView(
    modifier: Modifier = Modifier,
    allowedMetadataTypes: List<AVMetadataObjectType> = listOf(
        AVMetadataObjectTypeCodabarCode,
        AVMetadataObjectTypeAztecCode,
        AVMetadataObjectTypeCode39Code,
        AVMetadataObjectTypeCode93Code,
        AVMetadataObjectTypeCode128Code,
        AVMetadataObjectTypeQRCode,
    ),
    onScanned: (String) -> Boolean
) {
    val coordinator = remember {
        ScannerCameraCoordinator(
            onScanned = onScanned
        )
    }

    DisposableEffect(Unit) {
        val listener = OrientationListener { orientation ->
            coordinator.setCurrentOrientation(orientation)
        }

        listener.register()

        onDispose {
            listener.unregister()
        }
    }

    UIKitView(
        modifier = modifier, background = Color.Black,
        factory = {
            val previewContainer = UIView()
            println("Calling prepare")
            coordinator.prepare(previewContainer.layer, allowedMetadataTypes)
            previewContainer.backgroundColor = UIColor.cyanColor
            previewContainer
        },
        onResize = { view, rect ->
            CATransaction.begin()
            CATransaction.setValue(true, kCATransactionDisableActions)
            view.layer.setFrame(rect)
            coordinator.setFrame(rect)
            CATransaction.commit()
        },
        update = {},
    )

}

@OptIn(ExperimentalForeignApi::class)
class ScannerCameraCoordinator(
    val onScanned: (String) -> Boolean
) : AVCaptureMetadataOutputObjectsDelegateProtocol, NSObject() {

    private var previewLayer: AVCaptureVideoPreviewLayer? = null
    private lateinit var captureSession: AVCaptureSession

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class, DelicateCoroutinesApi::class)
    fun prepare(layer: CALayer, allowedMetadataTypes: List<AVMetadataObjectType>) {
        captureSession = AVCaptureSession()
        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)

        if (device == null) {
            println("Device has no camera")
            return
        }

        println("Initializing video input")
        val videoInput = memScoped {
            val error: ObjCObjectVar<NSError?> = alloc<ObjCObjectVar<NSError?>>()
            val videoInput = AVCaptureDeviceInput(device = device, error = error.ptr)
            if (error.value != null) {
                println(error.value)
                null
            } else {
                videoInput
            }
        }

        println("Adding video input")
        if (videoInput != null && captureSession.canAddInput(videoInput)) {
            captureSession.addInput(videoInput)
        } else {
            println("Could not add input")
            return
        }

        val metadataOutput = AVCaptureMetadataOutput()

        println("Adding metadata output")
        if (captureSession.canAddOutput(metadataOutput)) {
            captureSession.addOutput(metadataOutput)

            metadataOutput.setMetadataObjectsDelegate(this, queue = dispatch_get_main_queue())
            metadataOutput.metadataObjectTypes = allowedMetadataTypes
        } else {
            println("Could not add output")
            return
        }

        println("Adding preview layer")
        previewLayer = AVCaptureVideoPreviewLayer(session = captureSession).also {
            it.frame = layer.bounds
            it.videoGravity = AVLayerVideoGravityResizeAspectFill
            println("Set orientation")
            setCurrentOrientation(newOrientation = UIDevice.currentDevice.orientation)
            println("Adding sublayer")

            layer.bounds.useContents {
                println("Bounds: ${this.size.width}x${this.size.height}")
            }

            layer.frame.useContents {
                println("Frame: ${this.size.width}x${this.size.height}")
            }

            layer.addSublayer(it)
        }

        println("Launching capture session")

        GlobalScope.launch(Dispatchers.Default) {
            captureSession.startRunning()
        }
    }

    // This function is called when the orientation of the device changes
    fun setCurrentOrientation(newOrientation: UIDeviceOrientation) {
        when (newOrientation) {
            UIDeviceOrientation.UIDeviceOrientationLandscapeLeft -> previewLayer?.connection?.videoOrientation =
                AVCaptureVideoOrientationLandscapeRight

            UIDeviceOrientation.UIDeviceOrientationLandscapeRight -> previewLayer?.connection?.videoOrientation =
                AVCaptureVideoOrientationLandscapeLeft

            UIDeviceOrientation.UIDeviceOrientationPortrait -> previewLayer?.connection?.videoOrientation =
                AVCaptureVideoOrientationPortrait

            UIDeviceOrientation.UIDeviceOrientationPortraitUpsideDown -> previewLayer?.connection?.videoOrientation =
                AVCaptureVideoOrientationPortraitUpsideDown

            else -> previewLayer?.connection?.videoOrientation = AVCaptureVideoOrientationPortrait
        }
    }

    // This function is called when a QR code is found
    override fun captureOutput(
        output: platform.AVFoundation.AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: platform.AVFoundation.AVCaptureConnection
    ) {
        val metadataObject =
            didOutputMetadataObjects.firstOrNull() as? AVMetadataMachineReadableCodeObject

        metadataObject?.stringValue?.let { onFound(it) }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun onFound(code: String) {
        println("Found code: $code")
        AudioServicesPlaySystemSound(0x1000u) // Sound when a code is found
        captureSession.stopRunning()

        // if onScanned returns true, scanning will stop
        if (!onScanned(code)) {
            GlobalScope.launch(Dispatchers.Default) {
                captureSession.startRunning()
            }
        }
    }

    // This function is called when the preview layer is resized
    fun setFrame(rect: CValue<CGRect>) {
        previewLayer?.setFrame(rect)
    }
}