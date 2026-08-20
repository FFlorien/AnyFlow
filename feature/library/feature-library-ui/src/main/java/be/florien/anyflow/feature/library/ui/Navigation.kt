package be.florien.anyflow.feature.library.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.fragment.app.FragmentActivity
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
import be.florien.anyflow.common.navigation.findActivity
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
import be.florien.anyflow.management.filters.domain.model.FilterType
import be.florien.anyflow.management.filters.domain.model.PodcastFilterType
import be.florien.anyflow.management.filters.domain.model.TagFilterType
import kotlinx.collections.immutable.persistentListOf

fun EntryProviderScope<NavKey>.libraryEntries(
    viewModelFactory: AnyFlowViewModelFactory,
    navigator: ComposeNavigator
) {
    entry<BottomNavDestination.TagLibrary> {
        val activity = LocalContext.current.findActivity()
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
                if (!navigateToList(
                        row,
                        navigator,
                        filterParent
                    )
                ) { //todo navigate to info when SeeInLibrary and show playlist selection
                    viewModel.executeAction(it, activity as FragmentActivity)
                }
            }
        )
    }
    entry<TagInfo> { entry ->
        val activity = LocalContext.current.findActivity()
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
        val state = viewModel.state.collectAsStateWithLifecycle(persistentListOf())
        val resources = LocalResources.current
        LibraryInfoScreen(
            list = state.value,
            executeAction = { position ->
                val row = viewModel.libraryInfoRows.value[position]
                val type = row.fieldType.toFilterType()

                if (row.rowType == LibraryRowType.Action.SeeInLibrary && type != null) {
                    val parentRowDisplay = state
                        .value
                        .first { it.fieldType == row.fieldType }
                    val displayText = parentRowDisplay.info.getText(resources)
                    navigateToInfo(
                        Filter(
                            FilterParam(type, parentRowDisplay.id, displayText)
                        ),
                        navigator
                    )
                } else if (!navigateToList(row, navigator, viewModel.filterNavigation)) {
                    viewModel.executeAction(position, activity as FragmentActivity)
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
                navigateToInfo(filter, navigator)
            }
        )
    }
    entry<BottomNavDestination.PodcastLibrary> {
        val activity = LocalContext.current.findActivity()
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
                if (!navigateToList(row, navigator, filterParent)) {
                    viewModel.executeAction(it, activity as FragmentActivity)
                }
            }
        )
    }
    entry<PodcastInfo> { entry ->
        val activity = LocalContext.current.findActivity()
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
                if (!navigateToList(row, navigator, filterParent)) {
                    viewModel.executeAction(it, activity as FragmentActivity)
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
                navigateToInfo(filter, navigator)
            }
        )
    }
}

private fun navigateToInfo(
    filter: Filter,
    navigator: ComposeNavigator
) {
    val type = when (filter.mainParam.type) {
        is TagFilterType -> TagInfo(
            filter.mainParam.argument as Long,
            filter.mainParam.type,
            filter.mainParam.displayText
        )

        is PodcastFilterType -> PodcastInfo(
            filter.mainParam.argument as Long,
            filter.mainParam.type,
            filter.mainParam.displayText
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

private fun navigateToList(
    row: LibraryInfoRow,
    navigator: ComposeNavigator,
    filterParent: Filter?,
): Boolean =
    if (row.rowType == LibraryRowType.MultiRow.SubFilter || row.rowType == LibraryRowType.Action.SeeInLibrary) {
        val type = getTypeString(row)

        val route = if (row.fieldType is LibraryFieldType.Tags) {
            TagList(type, filterParent)
        } else {
            PodcastList(type, filterParent)
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

private fun LibraryFieldType.toFilterType(): FilterType? = when (this) {
    LibraryFieldType.Tags.Genre -> TagFilterType.GENRE_IS
    LibraryFieldType.Tags.AlbumArtist -> TagFilterType.ALBUM_ARTIST_IS
    LibraryFieldType.Tags.Album -> TagFilterType.ALBUM_IS
    LibraryFieldType.Tags.Artist -> TagFilterType.ARTIST_IS
    LibraryFieldType.Tags.Song -> TagFilterType.SONG_IS
    LibraryFieldType.Tags.Playlist -> TagFilterType.PLAYLIST_IS
    else -> null

}