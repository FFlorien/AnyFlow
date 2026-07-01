package be.florien.anyflow.feature.mediaList.ui

import android.content.ComponentName
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
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
import be.florien.anyflow.feature.player.service.PlayerService
import be.florien.anyflow.management.filters.domain.model.Filter
import be.florien.anyflow.management.filters.domain.model.FilterParam
import be.florien.anyflow.management.filters.domain.model.PodcastFilterType
import be.florien.anyflow.management.filters.domain.model.TagFilterType
import com.google.common.util.concurrent.MoreExecutors

fun EntryProviderScope<NavKey>.nowPlayingEntry(
    viewModelFactory: AnyFlowViewModelFactory,
    navigator: ComposeNavigator
) {
    entry<BottomNavDestination.NowPlaying> {
        val context = LocalContext.current

        val viewModel = viewModel<MediaListViewModel>(factory = viewModelFactory)
        var player: MediaController? by remember { mutableStateOf(null) }

        LaunchedEffect(viewModel) { // todo regard to lifecycle
            val sessionToken = SessionToken(
                context,
                ComponentName(context, PlayerService::class.java)
            )
            val mediaController =
                MediaController.Builder(context, sessionToken).buildAsync()
            mediaController.addListener({
                player = mediaController.get()
            }, MoreExecutors.directExecutor())
        }

        viewModel.player = player
        val state =
            viewModel.stateFlow.collectAsState(
                MediaListViewModel.State(
                    null,
                    0,
                    0
                )
            ).value
        val pagingList = state.mediaList?.collectAsLazyPagingItems()
        MediaListScreen(
            items = pagingList,
            selectedPosition = state.mediaPosition,
            selectedChapterTime = state.chapterTime,
            onMediaItemClick = viewModel::goToMedia,
            onChapterItemClick = viewModel::goToTime,
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
            })
    }
}