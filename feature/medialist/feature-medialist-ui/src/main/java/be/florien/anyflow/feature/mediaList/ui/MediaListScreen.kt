package be.florien.anyflow.feature.mediaList.ui

import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.text.Html
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.currentStateAsState
import androidx.lifecycle.compose.rememberLifecycleOwner
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import be.florien.anyflow.common.resources.component.ScrollBar
import be.florien.anyflow.common.resources.component.SlideRightToActionLeftToShortCut
import be.florien.anyflow.common.resources.component.handlerWidth
import be.florien.anyflow.common.resources.theming.AppTheme
import be.florien.anyflow.management.queue.model.Chapter
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

sealed interface MediaItemData {
    val id: Long

    sealed interface Full : MediaItemData {
        val position: Int
        val artUrl: String
        val title: String
        val author: String
        val duration: String

        data class Song(
            override val id: Long,
            override val position: Int,
            override val artUrl: String,
            override val title: String,
            override val author: String,
            override val duration: String,
            val album: String,
        ) : Full

        data class PodcastEpisode(
            override val id: Long,
            override val position: Int,
            override val artUrl: String,
            override val title: String,
            override val author: String,
            override val duration: String,
            val chapters: List<Chapter>
        ) : Full
    }

    data class PodcastChapter(
        override val id: Long,
        val podcastPosition: Int,
        val time: Long,
        val title: String
    ) : MediaItemData
}


enum class MediaPosition {
    Top, Bottom, None
}

@Composable
fun MediaListScreen(
    items: LazyPagingItems<MediaItemData>?,
    selectedPosition: Int,
    selectedChapterTime: Long,
    shortcuts: PersistentList<MediaListViewModel.ShortcutData>,
    onMediaItemClick: (Int) -> Unit,
    onChapterItemClick: (Int, Long) -> Unit,
    onItemNavigation: (MediaItemData.Full) -> Unit,
    onShortcut: (MediaListViewModel.ShortcutData, MediaItemData.Full) -> Unit,
    refreshShortcuts: () -> Unit
) {
    if (items == null) {
        Text(
            modifier = Modifier.fillMaxSize(),
            text = stringResource(R.string.general_loading_label)
        )
        return
    }
    val lifecycleOwner = rememberLifecycleOwner()
    val lifecycleState = lifecycleOwner.lifecycle.currentStateAsState()
    val lazyListState = rememberLazyListState()
    val loadingLabel = stringResource(R.string.general_loading_label)
    var displayCurrentMedia by remember { mutableStateOf(MediaPosition.None) }
    val currentItemPosition = remember(selectedPosition, items.itemSnapshotList) {
        items
            .itemSnapshotList
            .indexOfFirst { (it as? MediaItemData.Full)?.position == selectedPosition }
    }
    val currentItem = remember(currentItemPosition) {
        if (currentItemPosition >= 0) items.itemSnapshotList[currentItemPosition] as MediaItemData.Full else null
    }

    LaunchedEffect(lifecycleState.value == Lifecycle.State.RESUMED) {
        refreshShortcuts()
    }

    LaunchedEffect(selectedPosition) {
        snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo.map { it.index } }
            .distinctUntilChanged()
            .collect { itemData ->
                if (itemData.isEmpty()) {
                    return@collect
                }
                val selectedItem = items
                    .itemSnapshotList
                    .indexOfFirst { (it as? MediaItemData.Full)?.position == selectedPosition }
                displayCurrentMedia = if (itemData.min() >= selectedItem) {
                    MediaPosition.Top
                } else if (itemData.max() <= selectedItem) {
                    MediaPosition.Bottom
                } else {
                    MediaPosition.None
                }
            }
    }


    LaunchedEffect(lifecycleState.value == Lifecycle.State.RESUMED, currentItemPosition >= 0) {
        if (lifecycleState.value == Lifecycle.State.RESUMED && currentItemPosition >= 0) {
            lazyListState.scrollToItem(currentItemPosition)
        }
    }
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        ItemList(
            items = items,
            lazyListState = lazyListState,
            currentFullMedia = currentItem,
            shortcuts = shortcuts,
            selectedChapterTime = selectedChapterTime,
            onChapterItemClick = onChapterItemClick,
            onItemNavigation = onItemNavigation,
            onShortcut = onShortcut,
            onMediaItemClick = onMediaItemClick,
        )
        if (displayCurrentMedia != MediaPosition.None) {
            StickyItem(
                items = items,
                lazyListState = lazyListState,
                currentFullMedia = currentItem,
                shortcuts = shortcuts,
                currentMediaPosition = displayCurrentMedia,
                onItemNavigation = onItemNavigation,
                onShortcut = onShortcut
            )
        }
        ScrollBar(
            items = items,
            lazyListState = lazyListState,
            getSection = {
                when (val itemData = items.itemSnapshotList[lazyListState.firstVisibleItemIndex]) {
                    is MediaItemData.Full -> itemData.position.plus(1).toString()
                    is MediaItemData.PodcastChapter -> itemData.podcastPosition.plus(1).toString()
                    null -> loadingLabel
                }
            }
        )
    }
}

@Composable
private fun ItemList(
    items: LazyPagingItems<MediaItemData>,
    lazyListState: LazyListState,
    currentFullMedia: MediaItemData.Full?,
    shortcuts: PersistentList<MediaListViewModel.ShortcutData>,
    selectedChapterTime: Long,
    onChapterItemClick: (Int, Long) -> Unit,
    onItemNavigation: (MediaItemData.Full) -> Unit,
    onShortcut: (MediaListViewModel.ShortcutData, MediaItemData.Full) -> Unit,
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
            items[position]?.let { mediaItemData ->
                when (mediaItemData) {
                    is MediaItemData.PodcastChapter -> {
                        ChapterItem(
                            media = mediaItemData,
                            isSelected = mediaItemData.podcastPosition == currentFullMedia?.position && mediaItemData.time == selectedChapterTime,
                            onItemClick = onChapterItemClick
                        )
                    }

                    is MediaItemData.Full -> MediaItem(
                        media = mediaItemData,
                        shortcuts = shortcuts,
                        isSelected = mediaItemData == currentFullMedia,
                        onItemNavigation = onItemNavigation,
                        onShortcut = { onShortcut(it, mediaItemData) }
                    ) {
                        onMediaItemClick(mediaItemData.position)
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
    currentFullMedia: MediaItemData.Full?,
    shortcuts: PersistentList<MediaListViewModel.ShortcutData>,
    currentMediaPosition: MediaPosition,
    onShortcut: (MediaListViewModel.ShortcutData, MediaItemData.Full) -> Unit,
    onItemNavigation: (MediaItemData.Full) -> Unit
) {
    currentFullMedia?.let { media ->
        val coroutineScope = rememberCoroutineScope()
        val alignment = if (currentMediaPosition == MediaPosition.Top) {
            Alignment.TopCenter
        } else {
            Alignment.BottomCenter
        }
        val indexOfItemFull = items.itemSnapshotList.indexOf(currentFullMedia).takeIf { it >= 0 }
        MediaItem(
            media = media,
            shortcuts = shortcuts,
            isSelected = true,
            modifier = Modifier
                .align(alignment)
                .padding(end = handlerWidth),
            onItemNavigation = onItemNavigation,
            onShortcut = { onShortcut(it, media)}
        ) {
            coroutineScope.launch {
                indexOfItemFull?.let { index -> lazyListState.scrollToItem(index) }
            }
        }
    }
}

@Composable
fun MediaItem(
    media: MediaItemData.Full,
    shortcuts: PersistentList<MediaListViewModel.ShortcutData>,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onItemNavigation: (MediaItemData.Full) -> Unit,
    onShortcut: (MediaListViewModel.ShortcutData) -> Unit,
    onItemClick: () -> Unit
) {
    val background =
        if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    val textColor =
        if (isSelected) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface

    SlideRightToActionLeftToShortCut(
        modifier = modifier,
        isSingleShortcut = shortcuts.size == 1,
        actionBackground = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height = 116.dp)
                    .padding(8.dp)
                    .align(Alignment.CenterStart)
            ) {
                Image(
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.CenterVertically),
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = null
                )
            }
        },
        shortcuts = {
            for (shortcut in shortcuts.reversed()) {
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .size(64.dp)
                        .clickable {
                            onShortcut(shortcut)
                        }
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.medium
                        )
                ) {
                    Icon(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        painter = painterResource(shortcut.action.iconRes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Icon(
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.BottomEnd)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.medium
                            )
                            .padding(4.dp),
                        painter = painterResource(shortcut.field.iconRes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        },
        onSingleShortcut = {
            onShortcut(shortcuts[0])
        },
        onSlideComplete = {
            onItemNavigation(media)
        }) {
        Row(
            modifier = Modifier
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
                    id = 0L,
                    position = 0,
                    artUrl = "http://tutu.com",
                    title = "Song's title",
                    author = "Best band ever",
                    album = "Eponymous",
                    duration = "3:13"
                ),
                persistentListOf(),
                isSelected = false,
                onShortcut = {},
                onItemNavigation = {}
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
                    id = 0L,
                    position = 0,
                    artUrl = "http://tutu.com",
                    title = "Song's title",
                    author = "Best band ever",
                    duration = "3:13",
                    chapters = emptyList()
                ),
                shortcuts = persistentListOf(),
                isSelected = false,
                onShortcut = {},
                onItemNavigation = {}
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
