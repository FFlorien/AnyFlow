package be.florien.anyflow.feature.filter.current.ui

import android.content.res.Resources
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import be.florien.anyflow.common.di.AnyFlowViewModelFactory
import be.florien.anyflow.common.navigation.BottomNavDestination
import be.florien.anyflow.common.navigation.ComposeNavigator
import be.florien.anyflow.management.filters.domain.model.FilterParam
import be.florien.anyflow.management.filters.domain.model.PodcastFilterType
import be.florien.anyflow.management.filters.domain.model.TagFilterType
import kotlinx.collections.immutable.toPersistentList


fun EntryProviderScope<NavKey>.currentFilterEntry(
    viewModelFactory: AnyFlowViewModelFactory,
    navigator: ComposeNavigator
) {
    entry<BottomNavDestination.Filters> {
        val viewModel = viewModel<CurrentFilterViewModel>(factory = viewModelFactory)
        val state = viewModel.stateFlow.collectAsStateWithLifecycle().value
        val resources = LocalResources.current
        val filters = state.map { filter ->
            val mainParam = filter.filter.mainParam
            val artType = mainParam.type.artType
            val argument = mainParam.argument
            FilterUiData(
                id = filter.id,
                imageUrl = if (artType != null && argument is Long) {
                    viewModel.getUrlForImage(artType, argument)
                } else {
                    null
                },
                displayText = filter.filter.joinToString(separator = "<br>") {
                    getFilterText(it, resources)
                },
                fallbackRes = getDefaultDrawable(filter.filter.mainParam)
            )
        }.toPersistentList()
        CurrentFiltersScreen(
            filters,
            { filterId -> viewModel.deleteFilter(state.first { it.id == filterId }.filter) },
            viewModel::clearFilters,
            {
                navigator.navigate(BottomNavDestination.TagLibrary)
            },
            {
                navigator.navigate(BottomNavDestination.PodcastLibrary)
            }
        )
    }
}

private fun getFilterText(filterParam: FilterParam<*>, resources: Resources) =
    when (filterParam.type) {
        TagFilterType.GENRE_IS -> resources.getString(
            R.string.filter_display_genre_is,
            filterParam.displayText
        )

        TagFilterType.SONG_IS -> resources.getString(
            R.string.filter_display_song_is,
            filterParam.displayText
        )

        TagFilterType.ARTIST_IS -> resources.getString(
            R.string.filter_display_artist_is,
            filterParam.displayText
        )

        TagFilterType.ALBUM_ARTIST_IS -> resources.getString(
            R.string.filter_display_album_artist_is,
            filterParam.displayText
        )

        TagFilterType.ALBUM_IS -> resources.getString(
            R.string.filter_display_album_is,
            filterParam.displayText
        )

        TagFilterType.DISK_IS -> resources.getString( //todo is not displayed correctly for now because it is a subfilter
            R.string.filter_display_disk_is,
            filterParam.displayText
        )

        TagFilterType.PLAYLIST_IS -> resources.getString(
            R.string.filter_display_playlist_is,
            filterParam.displayText
        )

        TagFilterType.DOWNLOADED_STATUS_IS -> resources.getString(
            if (filterParam.argument as Boolean) R.string.filter_display_is_downloaded
            else R.string.filter_display_is_not_downloaded
        )

        PodcastFilterType.PODCAST_EPISODE_IS -> resources.getString(
            R.string.filter_display_podcast_episode_is,
            filterParam.displayText
        )

        PodcastFilterType.PODCAST_IS -> resources.getString(
            R.string.filter_display_podcast_is,
            filterParam.displayText
        )

        PodcastFilterType.STATE_IS -> resources.getString(
            R.string.filter_display_state_is,
            filterParam.displayText
        )
    }


private fun getDefaultDrawable(filterParam: FilterParam<*>): Int? {
    return when (filterParam.type) {
        TagFilterType.ALBUM_ARTIST_IS,
        TagFilterType.ARTIST_IS -> R.drawable.ic_artist

        TagFilterType.GENRE_IS -> R.drawable.ic_genre
        TagFilterType.ALBUM_IS -> R.drawable.ic_album
        TagFilterType.DISK_IS -> R.drawable.ic_disk
        TagFilterType.PLAYLIST_IS -> R.drawable.ic_playlist
        TagFilterType.DOWNLOADED_STATUS_IS -> R.drawable.ic_download
        TagFilterType.SONG_IS -> R.drawable.ic_song
        PodcastFilterType.PODCAST_EPISODE_IS -> R.drawable.ic_podcast_episode
        PodcastFilterType.PODCAST_IS -> R.drawable.ic_podcast
        PodcastFilterType.STATE_IS -> if (filterParam.argument == true) R.drawable.ic_downloaded else R.drawable.ic_download
    }
}