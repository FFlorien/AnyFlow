package be.florien.anyflow.common.resources.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.LazyPagingItems
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

val handlerHeight = 80.dp
val handlerWidth = 16.dp

@Composable
fun <T : Any> BoxScope.ScrollBar(
    items: LazyPagingItems<T>,
    lazyListState: LazyListState,
    getSection: T?.() -> String
) {
    val scope = rememberCoroutineScope()
    var isUserFastScroll by remember { mutableStateOf(false) }
    var verticalOffset by remember { mutableFloatStateOf(0f) }
    var scrollableHeight by remember { mutableFloatStateOf(0f) }
    var sectionName by remember { mutableStateOf<String?>(null) }
    val density = LocalDensity.current
    val scrollHandlerWidth = remember { with(density) { handlerWidth.toPx() } }
    val scrollHandlerHeight = remember { with(density) { handlerHeight.toPx() } }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect {
                if (!isUserFastScroll) {
                    val itemCount = (items.itemCount.takeUnless { it == 0 } ?: 1)
                    val ratio = lazyListState.firstVisibleItemIndex.toFloat() / itemCount
                    verticalOffset = scrollableHeight * ratio
                } else {
                    sectionName = items[lazyListState.firstVisibleItemIndex].getSection()
                }
            }
    }

    val scrollbarState = rememberDraggableState {
        verticalOffset = (verticalOffset + it).coerceIn(0f, scrollableHeight)
        val ratio = verticalOffset / scrollableHeight
        val itemPosition = items.itemCount * ratio
        val index = itemPosition.toInt()
        scope.launch {
            lazyListState.scrollToItem(
                index,
                (scrollHandlerHeight * (itemPosition - index)).roundToInt()
            )
        }
    }
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .width(handlerWidth)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.inversePrimary)
            .onGloballyPositioned {
                scrollableHeight = it.size.height - scrollHandlerHeight
            }
    )
    Box(
        modifier = Modifier
            .align { size, containerSize, layoutDirection ->
                IntOffset(
                    containerSize.width - scrollHandlerWidth.roundToInt(),
                    verticalOffset.roundToInt()
                )
            }
            .size(handlerWidth, handlerHeight)
            .background(MaterialTheme.colorScheme.secondary, MaterialTheme.shapes.large)
            .draggable(
                scrollbarState,
                orientation = Orientation.Vertical,
                onDragStarted = {
                    isUserFastScroll = true

                    sectionName = if (lazyListState.firstVisibleItemIndex < items.itemCount) {
                        items[lazyListState.firstVisibleItemIndex].getSection()
                    } else {
                        null
                    }
                },
                onDragStopped = {
                    isUserFastScroll = false
                    sectionName = null
                })
    )
    sectionName?.let {
        SectionLabel(scrollHandlerWidth, verticalOffset, it)
    }
}

@Composable
private fun BoxScope.SectionLabel(
    scrollHandlerWidth: Float,
    offset: Float,
    string: String
) {
    Text(
        modifier = Modifier
            .align { size, containerSize, layoutDirection ->
                IntOffset(
                    (containerSize.width - (scrollHandlerWidth * 3) - size.width).roundToInt(),
                    offset.roundToInt()
                )
            }
            .background(
                color = MaterialTheme.colorScheme.secondary,
                shape = MaterialTheme.shapes.extraLarge
            )
            .padding(12.dp),
        text = string,
        color = MaterialTheme.colorScheme.onSecondary,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Black,
        fontSize = 38.sp
    )
}