package be.florien.anyflow.feature.mediaList.ui

import android.content.ComponentName
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.paging.compose.collectAsLazyPagingItems
import be.florien.anyflow.common.di.AnyFlowViewModelFactory
import be.florien.anyflow.common.navigation.BottomNavDestination
import be.florien.anyflow.common.navigation.ComposeNavigator
import be.florien.anyflow.common.navigation.PodcastInfo
import be.florien.anyflow.common.navigation.TagInfo
import be.florien.anyflow.common.navigation.findActivity
import be.florien.anyflow.feature.player.service.PlayerService
import be.florien.anyflow.feature.song.base.domain.model.SongActionType
import be.florien.anyflow.management.filters.domain.model.Filter
import be.florien.anyflow.management.filters.domain.model.FilterParam
import be.florien.anyflow.management.filters.domain.model.PodcastFilterType
import be.florien.anyflow.management.filters.domain.model.TagFilterType
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.Flow

fun EntryProviderScope<NavKey>.nowPlayingEntry(
    viewModelFactory: AnyFlowViewModelFactory,
    navigator: ComposeNavigator,
    isSearching: Flow<Boolean>
) {
    entry<BottomNavDestination.NowPlaying> {
        val context = LocalContext.current
        val activity = context.findActivity()

        val viewModel = viewModel<MediaListViewModel>(factory = viewModelFactory)
        var player: MediaController? by remember { mutableStateOf(null) }
        val searchState = rememberTextFieldState()
        val isSearchingValue = isSearching.collectAsState(false).value

        LaunchedEffect(viewModel) { // todo regard to lifecycle
            val sessionToken = SessionToken(
                context,
                ComponentName(context, PlayerService::class.java)
            )
            val mediaController =
                MediaController.Builder(context, sessionToken).buildAsync()
            mediaController.addListener(
                { player = mediaController.get() },
                MoreExecutors.directExecutor()
            )
        }
        LaunchedEffect(searchState) {
            snapshotFlow { searchState.text.toString() }.collect {
                viewModel.search(it)
            }
        }

        viewModel.player = player
        val state =
            viewModel.stateFlow.collectAsState(
                MediaListViewModel.State(
                    null,
                    0,
                    0,
                    emptyList(),
                    0,
                    0,
                    0
                )
            ).value
        val pagingList = state.mediaList?.collectAsLazyPagingItems()
        MediaListScreen(
            mediaAndChapterItems = pagingList,
            currentMediaPosition = state.mediaPosition,
            isSearching = isSearchingValue,
            searchPosition = state.searchPosition,
            searchTotal = state.searchTotal,
            searchedItemPosition = state.searchItemPosition,
            shortcuts = state.shortcuts.toPersistentList(),
            onMediaItemClick = viewModel::goToMedia,
            onItemNavigation = {
                val filter = when (it) {
                    is MediaItemData.Full.Song -> Filter(
                        FilterParam(
                            TagFilterType.SONG_IS,
                            it.id,
                            it.title
                        )
                    )

                    is MediaItemData.Full.PodcastEpisode -> Filter(
                        FilterParam(
                            PodcastFilterType.PODCAST_EPISODE_IS,
                            it.id,
                            it.title
                        )
                    )
                }
                val type = if (filter.mainParam.type is TagFilterType) {
                    TagInfo(it.id, filter.mainParam.type, it.title)
                } else {
                    PodcastInfo(it.id, filter.mainParam.type, it.title)
                }
                val parentType = if (filter.mainParam.type is TagFilterType) {
                    BottomNavDestination.TagLibrary
                } else {
                    BottomNavDestination.PodcastLibrary
                }
                navigator.navigate(type, parentType, true)
            },
            onSearchChange = viewModel::search,
            onSearchPositionChange = viewModel::setSearchPosition,
            onShortcut = { shortcut, media ->
                if (shortcut.action == SongActionType.AddToPlaylist) {
                    val type = shortcut.field.toTagType()
                    viewModel.navigator.displayPlaylistSelection(
                        (activity as FragmentActivity).supportFragmentManager,
                        media.id,
                        type,
                        -1
                    )
                } else {
                    viewModel.executeAction(media, shortcut)
                }
            },
            refreshShortcuts = viewModel::refreshShortcuts
        )
    }
}