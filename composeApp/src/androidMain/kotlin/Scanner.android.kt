import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
actual fun Scanner(
    modifier: Modifier,
    onScanned: (String) -> Boolean,
) {
    val analyzer = remember {
        BarCodeAnalyzer(onCodeScanned = onScanned)
    }

    AndroidScannerView(modifier, analyzer)
}

@Composable
actual fun rememberCameraPermissionState(): CameraPermissionState {

    val cameraPermission = Manifest.permission.CAMERA
    val context = LocalContext.current

    // Create a state to hold the permission status
    val permissionStatus = remember { mutableStateOf(CameraPermissionStatus.Denied) }

    LaunchedEffect(Unit) {
        // Check if the permission is already granted
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            cameraPermission,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            permissionStatus.value = CameraPermissionStatus.Granted
        }
    }

    // Create a launcher for the permission request
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionStatus.value = if (isGranted) {
            CameraPermissionStatus.Granted
        } else {
            CameraPermissionStatus.Denied
        }
    }

    return object : CameraPermissionState {
        override val status: CameraPermissionStatus
            get() = permissionStatus.value

        override fun requestCameraPermission() {
            launcher.launch(cameraPermission)
        }

        override fun goToSettings() {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:" + context.packageName)
            ContextCompat.startActivity(context, intent, null)
        }
    }
}