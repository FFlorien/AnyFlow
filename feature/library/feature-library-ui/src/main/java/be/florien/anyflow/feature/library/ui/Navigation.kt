package be.florien.anyflow.feature.library.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import be.florien.anyflow.common.di.AnyFlowViewModelFactory
import be.florien.anyflow.common.navigation.BottomNavDestination
import be.florien.anyflow.common.navigation.ComposeNavigator
import be.florien.anyflow.common.navigation.PodcastInfo
import be.florien.anyflow.common.navigation.PodcastList
import be.florien.anyflow.common.navigation.TagInfo
import be.florien.anyflow.common.navigation.TagList
import be.florien.anyflow.feature.library.domain.model.LibraryFieldType
import be.florien.anyflow.feature.library.domain.model.LibraryInfoRow
import be.florien.anyflow.feature.library.domain.model.LibraryRowType
import be.florien.anyflow.feature.library.ui.info.LibraryInfoScreen
import be.florien.anyflow.feature.library.ui.info.LibraryInfoViewModel
import be.florien.anyflow.feature.library.ui.info.LibraryInfoViewModel.Companion.ALBUM_ARTIST_ID
import be.florien.anyflow.feature.library.ui.info.LibraryInfoViewModel.Companion.ALBUM_ID
import be.florien.anyflow.feature.library.ui.info.LibraryInfoViewModel.Companion.ARTIST_ID
import be.florien.anyflow.feature.library.ui.info.LibraryInfoViewModel.Companion.DOWNLOAD_ID
import be.florien.anyflow.feature.library.ui.info.LibraryInfoViewModel.Companion.GENRE_ID
import be.florien.anyflow.feature.library.ui.info.LibraryInfoViewModel.Companion.PLAYLIST_ID
import be.florien.anyflow.feature.library.ui.info.LibraryInfoViewModel.Companion.PODCAST_EPISODE_ID
import be.florien.anyflow.feature.library.ui.info.LibraryInfoViewModel.Companion.PODCAST_ID
import be.florien.anyflow.feature.library.ui.info.LibraryInfoViewModel.Companion.PODCAST_TYPE
import be.florien.anyflow.feature.library.ui.info.LibraryInfoViewModel.Companion.SONG_ID
import be.florien.anyflow.feature.library.ui.info.LibraryInfoViewModel.Companion.TAGS_TYPE
import be.florien.anyflow.feature.library.ui.list.FilterDisplay
import be.florien.anyflow.feature.library.ui.list.LibraryListScreen
import be.florien.anyflow.feature.library.ui.list.LibraryListViewModel
import be.florien.anyflow.feature.library.ui.list.viewmodels.LibraryAlbumArtistListViewModel
import be.florien.anyflow.feature.library.ui.list.viewmodels.LibraryAlbumListViewModel
import be.florien.anyflow.feature.library.ui.list.viewmodels.LibraryArtistListViewModel
import be.florien.anyflow.feature.library.ui.list.viewmodels.LibraryDownloadedListViewModel
import be.florien.anyflow.feature.library.ui.list.viewmodels.LibraryGenreListViewModel
import be.florien.anyflow.feature.library.ui.list.viewmodels.LibraryPlaylistListViewModel
import be.florien.anyflow.feature.library.ui.list.viewmodels.LibraryPodcastEpisodeListViewModel
import be.florien.anyflow.feature.library.ui.list.viewmodels.LibraryPodcastListViewModel
import be.florien.anyflow.feature.library.ui.list.viewmodels.LibraryTagsListViewModel
import be.florien.anyflow.management.filters.domain.model.Filter
import be.florien.anyflow.management.filters.domain.model.FilterParam
import be.florien.anyflow.management.filters.domain.model.PodcastFilterType
import be.florien.anyflow.management.filters.domain.model.TagFilterType
import kotlinx.collections.immutable.persistentListOf

fun EntryProviderScope<NavKey>.libraryEntries(
    viewModelFactory: AnyFlowViewModelFactory,
    navigator: ComposeNavigator
) {
    entry<BottomNavDestination.TagLibrary> {
        val viewModel = viewModel<LibraryInfoViewModel>(factory = viewModelFactory)
        viewModel.setType(TAGS_TYPE)
        viewModel.filterNavigation = null
        val state =
            viewModel.state.collectAsStateWithLifecycle(persistentListOf())
        LibraryInfoScreen(
            list = state.value,
            executeAction = {
                val row = viewModel.libraryInfoRows.value[it]
                val filterParent = viewModel.filterNavigation
                if (!executeListNavigation(
                        row,
                        navigator,
                        filterParent
                    )
                ) { //todo navigate to info when SeeInLibrary
                    viewModel.executeAction(it)
                }
            }
        )
    }
    entry<TagInfo> { entry ->
        val viewModel = viewModel<LibraryInfoViewModel>(
            factory = viewModelFactory
        )
        viewModel.setType(TAGS_TYPE)
        viewModel.filterNavigation =
            Filter(
                FilterParam(
                    entry.type,
                    entry.id,
                    entry.title
                )
            )
        val state =
            viewModel.state.collectAsStateWithLifecycle(persistentListOf())
        LibraryInfoScreen(
            list = state.value,
            executeAction = {
                val row = viewModel.libraryInfoRows.value[it]
                val filterParent = viewModel.filterNavigation
                if (!executeListNavigation(row, navigator, filterParent)) {
                    viewModel.executeAction(it)
                }
            }
        )
    }
    entry<TagList> { entry ->
        val listType = entry.type
        val viewModel = getListViewModel(listType, viewModelFactory)
        viewModel.navigationFilter = entry.filterParent

        LibraryListScreen(
            itemsPager = viewModel.values,
            onClick = viewModel::toggleFilterSelection,
            onNavigation = {
                val filter = viewModel.getFilter(it.toItem())
                navigateToInfo(filter, it, navigator)
            }
        )
    }
    entry<BottomNavDestination.PodcastLibrary> {
        val viewModel =
            viewModel<LibraryInfoViewModel>(factory = viewModelFactory)
        viewModel.setType(PODCAST_TYPE)
        viewModel.filterNavigation = null
        val state =
            viewModel.state.collectAsStateWithLifecycle(persistentListOf())
        LibraryInfoScreen(
            list = state.value,
            executeAction = {
                val row = viewModel.libraryInfoRows.value[it]
                val filterParent = viewModel.filterNavigation
                if (!executeListNavigation(row, navigator, filterParent)) {
                    viewModel.executeAction(it)
                }
            }
        )
    }
    entry<PodcastInfo> { entry ->
        val viewModel = viewModel<LibraryInfoViewModel>(
            factory = viewModelFactory
        )
        viewModel.setType(PODCAST_TYPE)
        viewModel.filterNavigation =
            Filter(
                FilterParam(
                    entry.type,
                    entry.id,
                    entry.title
                )
            )
        val state =
            viewModel.state.collectAsStateWithLifecycle(persistentListOf())
        LibraryInfoScreen(
            list = state.value,
            executeAction = {
                val row = viewModel.libraryInfoRows.value[it]
                val filterParent = viewModel.filterNavigation
                if (!executeListNavigation(row, navigator, filterParent)) {
                    viewModel.executeAction(it)
                }
            }
        )
    }
    entry<PodcastList> { entry ->
        val listType = entry.type
        val viewModel = getListViewModel(listType, viewModelFactory)
        viewModel.navigationFilter = entry.filterParent

        LibraryListScreen(
            itemsPager = viewModel.values,
            onClick = viewModel::toggleFilterSelection,
            onNavigation = {
                val filter = viewModel.getFilter(it.toItem())
                navigateToInfo(filter, it, navigator)
            }
        )
    }
}

private fun navigateToInfo(
    filter: Filter,
    display: FilterDisplay,
    navigator: ComposeNavigator
) {
    val type = when (filter.mainParam.type) {
        is TagFilterType -> TagInfo(
            display.id,
            filter.mainParam.type,
            display.title
        )

        is PodcastFilterType -> PodcastInfo(
            display.id,
            filter.mainParam.type,
            display.title
        )
    }
    val parentType = when (filter.mainParam.type) {
        is TagFilterType -> BottomNavDestination.TagLibrary
        is PodcastFilterType -> BottomNavDestination.PodcastLibrary
    }
    navigator.navigate(type, parentType)
}

@Composable
private fun getListViewModel(
    listType: String,
    viewModelFactory: AnyFlowViewModelFactory
): LibraryListViewModel = when (listType) {
    PLAYLIST_ID -> viewModel<LibraryPlaylistListViewModel>(factory = viewModelFactory)
    ALBUM_ID -> viewModel<LibraryAlbumListViewModel>(factory = viewModelFactory)
    ALBUM_ARTIST_ID -> viewModel<LibraryAlbumArtistListViewModel>(
        factory = viewModelFactory
    )

    ARTIST_ID -> viewModel<LibraryArtistListViewModel>(factory = viewModelFactory)
    GENRE_ID -> viewModel<LibraryGenreListViewModel>(factory = viewModelFactory)
    SONG_ID -> viewModel<LibraryTagsListViewModel>(factory = viewModelFactory)
    DOWNLOAD_ID -> viewModel<LibraryDownloadedListViewModel>(factory = viewModelFactory)
    PODCAST_ID -> viewModel<LibraryPodcastListViewModel>(factory = viewModelFactory)
    PODCAST_EPISODE_ID -> viewModel<LibraryPodcastEpisodeListViewModel>(
        factory = viewModelFactory
    )

    else -> viewModel<LibraryGenreListViewModel>(factory = viewModelFactory)
}

private fun executeListNavigation(
    row: LibraryInfoRow,
    navigator: ComposeNavigator,
    filterParent: Filter?,
): Boolean =
    if (row.rowType == LibraryRowType.MultiRow.SubFilter || row.rowType == LibraryRowType.Action.SeeInLibrary) {
        val value = getTypeString(row)

        val route = if (row.fieldType is LibraryFieldType.Tags) {
            TagList(value, filterParent)
        } else {
            PodcastList(value, filterParent)
        }
        navigator.navigate(route)
        true
    } else {
        false
    }

private fun getTypeString(row: LibraryInfoRow): String = when (row.fieldType) {
    LibraryFieldType.Tags.Playlist -> PLAYLIST_ID
    LibraryFieldType.Tags.Album -> ALBUM_ID
    LibraryFieldType.Tags.AlbumArtist -> ALBUM_ARTIST_ID
    LibraryFieldType.Tags.Artist -> ARTIST_ID
    LibraryFieldType.Tags.Genre -> GENRE_ID
    LibraryFieldType.Tags.Song -> SONG_ID
    LibraryFieldType.Tags.Downloaded -> DOWNLOAD_ID
    LibraryFieldType.Podcast.Podcast -> PODCAST_ID
    LibraryFieldType.Podcast.PodcastEpisode -> PODCAST_EPISODE_ID
    LibraryFieldType.Tags.Duration -> GENRE_ID //Shouldn't happen
}