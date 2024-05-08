import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
expect fun Scanner(
    modifier: Modifier = Modifier,
    onScanned: (String) -> Boolean,
)

@Composable
fun ScannerView(
    modifier: Modifier = Modifier.clipToBounds(),
    onScanned: (String) -> Boolean,
    permissionText: String,
    openSettingsLabel: String,
) {
    val permissionState = rememberCameraPermissionState()

    LaunchedEffect(permissionState) {
        if (permissionState.status == CameraPermissionStatus.Denied) {
            permissionState.requestCameraPermission()
        }
    }

    when (permissionState.status) {
        CameraPermissionStatus.Denied -> {
            Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    modifier = Modifier.padding(6.dp),
                    text = permissionText,
                    textAlign = TextAlign.Center,
                )

                Button(onClick = { permissionState.goToSettings() }) {
                    Text(openSettingsLabel)
                }
            }
        }

        CameraPermissionStatus.Granted -> {
            Scanner(modifier, onScanned = onScanned)
        }
    }
}
