package be.florien.anyflow.feature.library.ui.image

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage

@Composable
fun ImageDialog(source: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ZoomableAsyncImage(
            modifier = Modifier
                .fillMaxSize(),
            model = source,
            contentDescription = null,
            contentScale = ContentScale.Fit
        )
    }
}