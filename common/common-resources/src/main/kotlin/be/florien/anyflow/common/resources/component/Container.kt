package be.florien.anyflow.common.resources.component

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import be.florien.anyflow.common.resources.R
import kotlinx.coroutines.launch
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
    actions: @Composable RowScope.() -> Unit,
) {
    BlueTopAppBar(
        title = title,
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    painterResource(R.drawable.ic_up),
                    contentDescription = stringResource(R.string.content_description_up),
                    tint = Color.Unspecified
                )
            }
        },
        actions = actions
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlueTopAppBarMain(
    title: String,
    onHamburgerMenuClicked: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    BlueTopAppBar(
        title = title,
        navigationIcon = {
            IconButton(onClick = onHamburgerMenuClicked) {
                Icon(
                    Icons.Outlined.Menu,
                    tint = MaterialTheme.colorScheme.tertiary,
                    contentDescription = "Menu"
                )
            }
        },
        actions = actions
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlueTopAppBar(
    title: String,
    navigationIcon: @Composable (() -> Unit),
    actions: @Composable (RowScope.() -> Unit),
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = navigationIcon,
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
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

@Composable
fun SlideRightToActionLeftToShortCut(
    isSingleShortcut: Boolean,
    actionBackground: @Composable BoxScope.() -> Unit,
    shortcuts: @Composable RowScope.() -> Unit,
    onSlideComplete: () -> Unit,
    modifier: Modifier = Modifier,
    onSingleShortcut: () -> Unit = {},
    foreground: @Composable BoxScope.() -> Unit
) {
    val originalViewConfiguration = LocalViewConfiguration.current
    val viewConfiguration = object : ViewConfiguration {
        override val doubleTapMinTimeMillis: Long
            get() = originalViewConfiguration.doubleTapMinTimeMillis
        override val doubleTapTimeoutMillis: Long
            get() = originalViewConfiguration.doubleTapTimeoutMillis
        override val longPressTimeoutMillis: Long
            get() = originalViewConfiguration.longPressTimeoutMillis
        override val touchSlop: Float
            get() = 40f // set this to any value you want

    }
    CompositionLocalProvider(LocalViewConfiguration provides viewConfiguration) {
        val density: Density = LocalDensity.current
        val dpValue = with(density) { SLIDE_TOTAL_WIDTH.toDp() }
        var contextMenuWidth by remember {
            mutableFloatStateOf(0f)
        }
        val scope = rememberCoroutineScope()
        val offsetX = remember {
            Animatable(initialValue = 0f)
        }
        Box(
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                horizontalArrangement = Arrangement.End
            ) {
                Box(modifier = Modifier.width(dpValue)) {
                    actionBackground()
                }
                Spacer(modifier = Modifier.weight(1F))
                Row(Modifier.onSizeChanged {
                    contextMenuWidth = it.width.toFloat()
                }) { shortcuts() }
            }
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .pointerInput(contextMenuWidth) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { _, dragAmount ->
                                scope.launch {
                                    val newOffset = (offsetX.value + dragAmount)
                                        .coerceIn(-contextMenuWidth, SLIDE_TOTAL_WIDTH)
                                    offsetX.snapTo(newOffset)
                                }
                            },
                            onDragEnd = {
                                if (offsetX.value >= SLIDE_COMPLETE_WIDTH) {
                                    onSlideComplete()
                                }
                                if (isSingleShortcut && offsetX.value <= -contextMenuWidth) {
                                    onSingleShortcut()
                                }
                                if (offsetX.value > 0 || isSingleShortcut) {
                                    scope.launch {
                                        offsetX.animateTo(0f)
                                    }
                                }
                            }
                        )
                    }
            ) {
                foreground()
            }
        }
    }
}
