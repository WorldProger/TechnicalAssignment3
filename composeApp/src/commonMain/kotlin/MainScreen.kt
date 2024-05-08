import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun MainScreen() {
    MaterialTheme {

        var scannerEnabled by remember { mutableStateOf(false) }
        var scannedData by remember { mutableStateOf("") }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.25f))

            AnimatedContent(
                targetState = scannerEnabled,
                transitionSpec = { fadeIn() togetherWith fadeOut() }
            ) { enabled ->
                if (enabled) {
                    ScannerView(
                        modifier = Modifier
                            .padding(16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .size(250.dp),
                        onScanned = {
                            scannedData = it
                            scannerEnabled = false

                            // Return true to stop scanning
                            true
                        },
                        permissionText = "Camera is required for QR Code scanning",
                        openSettingsLabel = "Open Settings"
                    )
                } else {
                    Text(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        text = scannedData,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.25f))

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 32.dp),
                onClick = { scannerEnabled = !scannerEnabled }
            ) {
                Text(if (scannerEnabled) "Stop scan" else "Scan", fontSize = 28.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                modifier = Modifier.padding(horizontal = 24.dp),
                text = "Please hold your phone horizontally to scan Bar codes",
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}