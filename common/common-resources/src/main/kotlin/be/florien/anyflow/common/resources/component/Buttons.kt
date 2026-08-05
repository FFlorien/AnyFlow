package be.florien.anyflow.common.resources.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import be.florien.anyflow.common.resources.theming.onSecondaryContainerLight
import be.florien.anyflow.common.resources.theming.secondaryContainerLight

@Composable
fun ActionIcon(
    painter: @Composable () -> Painter,
    contentDescription: String?,
    onClick: () -> Unit,
) {
    Icon(
        modifier = Modifier
            .size(48.dp)
            .clickable(onClick = onClick)
            .padding(8.dp),
        painter = painter(),
        contentDescription = contentDescription,
        tint = MaterialTheme.colorScheme.tertiary
    )
}

@Composable
fun LoadingButton(
    isLoading: Boolean,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Button(
        modifier = modifier,
        onClick = {
            if (!isLoading) {
                onAction()
            }
        }
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = secondaryContainerLight,
                trackColor = onSecondaryContainerLight
            )
        } else {
            content()
        }
    }
}