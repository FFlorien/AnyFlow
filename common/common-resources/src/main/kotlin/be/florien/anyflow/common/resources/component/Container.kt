package be.florien.anyflow.common.resources.component

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import be.florien.anyflow.common.resources.R
import kotlin.math.roundToInt


@Composable
fun TopSnackbarHost(snackbarHostState: SnackbarHostState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding()
    ) {
        SnackbarHost(
            hostState = snackbarHostState,
            snackbar = {
                Snackbar(
                    snackbarData = it,
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopCenter)
                )
            })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlueTopAppBar(
    title: String,
    onClose: () -> Unit,
    actions: @Composable (RowScope) -> Unit
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    painterResource(R.drawable.ic_up),
                    contentDescription = stringResource(R.string.content_description_up),
                    tint = Color.Unspecified
                )
            }
        },
        actions = actions,
        colors = TopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            scrolledContainerColor = MaterialTheme.colorScheme.primaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.secondary,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.secondary,
            subtitleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

private const val SLIDE_TOTAL_WIDTH = 300f
private const val SLIDE_COMPLETE_WIDTH = 225f

@Composable
fun SlideRightToAction(
    background: @Composable BoxScope.() -> Unit,
    onSlideComplete: () -> Unit,
    modifier: Modifier = Modifier,
    foreground: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        background()
        var offsetX by remember { mutableFloatStateOf(0f) }
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState {
                        offsetX = (offsetX + it).coerceIn(0f, SLIDE_TOTAL_WIDTH)
                    },
                    onDragStopped = {
                        if (offsetX >= SLIDE_COMPLETE_WIDTH) {
                            onSlideComplete()
                        }
                        offsetX = 0f
                    }
                )
        ) {
            foreground()
        }
    }
}