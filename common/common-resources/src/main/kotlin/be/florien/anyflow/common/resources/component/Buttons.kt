package be.florien.anyflow.common.resources.component

import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import be.florien.anyflow.common.resources.theming.onSecondaryContainerLight
import be.florien.anyflow.common.resources.theming.secondaryContainerLight


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