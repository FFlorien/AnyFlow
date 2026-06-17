package be.florien.anyflow.feature.mediaList.ui

import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.text.Html
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import be.florien.anyflow.common.resources.component.ScrollBar
import be.florien.anyflow.common.resources.component.handlerWidth
import be.florien.anyflow.common.resources.theming.AppTheme
import be.florien.anyflow.management.queue.model.Chapter
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

sealed interface MediaItemData {
    val id: String

    sealed interface Full : MediaItemData {
        val position: Int
        val artUrl: String
        val title: String
        val author: String
        val duration: String

        data class Song(
            override val id: String,
            override val position: Int,
            override val artUrl: String,
            override val title: String,
            override val author: String,
            override val duration: String,
            val album: String,
        ) : Full

        data class PodcastEpisode(
            override val id: String,
            override val position: Int,
            override val artUrl: String,
            override val title: String,
            override val author: String,
            override val duration: String,
            val chapters: List<Chapter>
        ) : Full
    }

    data class PodcastChapter(
        override val id: String,
        val podcastPosition: Int,
        val time: Long,
        val title: String
    ) : MediaItemData
}


enum class MediaPosition {
    Top, Bottom, None
}

@Composable
fun MediaList(
    items: LazyPagingItems<MediaItemData>?,
    selectedPosition: Int,
    selectedChapterTime: Long,
    onMediaItemClick: (Int) -> Unit,
    onChapterItemClick: (Int, Long) -> Unit
) {
    if (items == null) {
        Text(stringResource(R.string.general_loading_label))
        return
    }
    val lazyListState = rememberLazyListState()
    var displayCurrentMedia by remember { mutableStateOf(MediaPosition.None) }

    LaunchedEffect(selectedPosition) {
        snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo.map { it.index } }
            .distinctUntilChanged()
            .collect { itemData ->
                if (itemData.isEmpty()) {
                    return@collect
                }
                val selectedItem =
                    items.itemSnapshotList.indexOfFirst { (it as? MediaItemData.Full)?.position == selectedPosition }
                displayCurrentMedia = if (itemData.min() >= selectedItem) {
                    MediaPosition.Top
                } else if (itemData.max() <= selectedItem) {
                    MediaPosition.Bottom
                } else {
                    MediaPosition.None
                }
            }
    }
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        ItemList(
            items,
            lazyListState,
            selectedPosition,
            selectedChapterTime,
            onChapterItemClick,
            onMediaItemClick
        )
        if (displayCurrentMedia != MediaPosition.None) {
            StickyItem(items, lazyListState, selectedPosition, displayCurrentMedia)
        }
        ScrollBar(
            items = items,
            lazyListState = lazyListState,
            getSection = {
                lazyListState.firstVisibleItemIndex.plus(1).toString()
            }
        )
    }
}

@Composable
private fun ItemList(
    items: LazyPagingItems<MediaItemData>,
    lazyListState: LazyListState,
    selectedPosition: Int,
    selectedChapterTime: Long,
    onChapterItemClick: (Int, Long) -> Unit,
    onMediaItemClick: (Int) -> Unit
) {
    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(end = handlerWidth),
    ) {
        items(
            count = items.itemCount,
            key = items.itemKey {
                val type = when (it) {
                    is MediaItemData.Full.Song -> "song"
                    is MediaItemData.Full.PodcastEpisode -> "podcast"
                    is MediaItemData.PodcastChapter -> "chapter_${it.time}"
                }
                "${type}_${it.id}"
            }
        ) { position ->
            items[position]?.let {
                when (it) {
                    is MediaItemData.PodcastChapter -> {
                        ChapterItem(
                            media = it,
                            isSelected = it.podcastPosition == selectedPosition && it.time == selectedChapterTime,
                            onItemClick = onChapterItemClick
                        )
                    }

                    is MediaItemData.Full -> MediaItem(
                        it,
                        it.position == selectedPosition
                    ) {
                        onMediaItemClick(it.position)
                    }
                }

            } ?: Text(
                modifier = Modifier.height(116.dp),
                text = stringResource(R.string.general_loading_label)
            )
        }
    }
}

@Composable
private fun BoxScope.StickyItem(
    items: LazyPagingItems<MediaItemData>,
    lazyListState: LazyListState,
    selectedPosition: Int,
    currentMediaPosition: MediaPosition
) {
    val coroutineScope = rememberCoroutineScope()
    val alignment = if (currentMediaPosition == MediaPosition.Top) {
        Alignment.TopCenter
    } else {
        Alignment.BottomCenter
    }
    val modifier = Modifier
        .align(alignment)
        .padding(end = handlerWidth)
    if (selectedPosition < items.itemCount) {
        val itemData = items.itemSnapshotList.items.first {
            when (it) {
                is MediaItemData.Full -> it.position == selectedPosition
                is MediaItemData.PodcastChapter -> it.podcastPosition == selectedPosition
            }
        }
        val itemFull = itemData as? MediaItemData.Full
            ?: (itemData as? MediaItemData.PodcastChapter)
                ?.podcastPosition
                ?.let { items[it] as? MediaItemData.Full }
        itemFull?.let {
            MediaItem(
                modifier = modifier,
                media = it,
                isSelected = true
            ) {
                coroutineScope.launch {
                    lazyListState.scrollToItem(selectedPosition)
                }
            }

        }
    }
}

@Composable
fun MediaItem(
    media: MediaItemData.Full,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onItemClick: () -> Unit
) {
    val background =
        if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    val textColor =
        if (isSelected) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 116.dp)
            .background(background)
            .clickable(onClick = onItemClick)
    ) {
        AsyncImage(
            modifier = Modifier
                .padding(8.dp)
                .size(100.dp)
                .align(Alignment.CenterVertically),
            model = media.artUrl,
            contentDescription = null
        )
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                modifier = Modifier.padding(top = 8.dp, end = 8.dp),
                text = Html.fromHtml(Html.fromHtml(media.title).toString()).toString()
                    .trim(),
                color = textColor
            )
            Text(
                modifier = Modifier.padding(top = 8.dp, end = 8.dp),
                text = media.author,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium
            )
            (media as? MediaItemData.Full.Song)?.album?.let {
                Text(
                    modifier = Modifier.padding(top = 8.dp, end = 8.dp),
                    text = it,
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(bottom = 8.dp, end = 8.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.large
                    )
                    .padding(horizontal = 4.dp),
                text = media.duration,
                textAlign = TextAlign.End,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun ChapterItem(
    media: MediaItemData.PodcastChapter,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onItemClick: (Int, Long) -> Unit
) {
    val background =
        if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    val textColor =
        if (isSelected) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Text(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onItemClick(media.podcastPosition, media.time)
            }
            .background(background)
            .padding(12.dp),
        text = Html.fromHtml(Html.fromHtml(media.title).toString()).toString()
            .trim(),
        color = textColor
    )
}

@Preview
@Composable
fun SongItemPreview() {
    AppTheme {
        PreviewAsyncImage(R.drawable.cover_placeholder) {
            MediaItem(
                media = MediaItemData.Full.Song(
                    id = "0",
                    position = 0,
                    artUrl = "http://tutu.com",
                    title = "Song's title",
                    author = "Best band ever",
                    album = "Eponymous",
                    duration = "3:13"
                ),
                isSelected = false
            ) {}
        }

    }
}

@Preview
@Composable
fun PodcastItemPreview() {
    AppTheme {
        PreviewAsyncImage(R.drawable.cover_placeholder) {
            MediaItem(
                media = MediaItemData.Full.PodcastEpisode(
                    id = "0",
                    position = 0,
                    artUrl = "http://tutu.com",
                    title = "Song's title",
                    author = "Best band ever",
                    duration = "3:13",
                    chapters = emptyList()
                ),
                isSelected = false
            ) {}
        }

    }
}

@OptIn(ExperimentalCoilApi::class)
@Composable
fun PreviewAsyncImage(
    drawableResId: Int,
    content: @Composable () -> Unit
) {
    if (!LocalInspectionMode.current) {
        error("PreviewAsyncImage should only be used in @Preview functions")
    }

    val context = LocalContext.current
    val previewHandler = remember(drawableResId) {
        AsyncImagePreviewHandler {
            val drawable = ContextCompat.getDrawable(context, drawableResId)
            val bitmap = if (drawable is BitmapDrawable) {
                drawable.bitmap
            } else {
                // Convert vector/other drawables to bitmap
                val width = drawable?.intrinsicWidth?.takeIf { it > 0 } ?: 1
                val height = drawable?.intrinsicHeight?.takeIf { it > 0 } ?: 1
                val bitmap = createBitmap(width, height)
                val canvas = Canvas(bitmap)
                drawable?.setBounds(0, 0, canvas.width, canvas.height)
                drawable?.draw(canvas)
                bitmap
            }
            bitmap.asImage()
        }
    }

    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides previewHandler) {
        content()
    }
}
