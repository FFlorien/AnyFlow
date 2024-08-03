package be.florien.anyflow.feature.library.tags.ui.info

import androidx.lifecycle.ViewModelProvider
import be.florien.anyflow.architecture.di.viewModelFactory
import be.florien.anyflow.common.ui.data.info.InfoActions
import be.florien.anyflow.feature.library.domain.LibraryTagsInfoActions
import be.florien.anyflow.feature.library.tags.ui.list.LibraryTagsListFragment
import be.florien.anyflow.feature.library.ui.R
import be.florien.anyflow.feature.library.ui.info.LibraryInfoFragment
import be.florien.anyflow.management.filters.model.Filter
import kotlin.random.Random

class LibraryTagsInfoFragment(parentFilter: Filter<*>? = null) : LibraryInfoFragment(parentFilter) {
    override fun getTitle(): String = getString(R.string.library_title_main)
    override fun getSubtitle(): String? = parentFilter?.getFullDisplay()
    override fun getLibraryInfoViewModel() = ViewModelProvider(
        requireActivity(),
        requireActivity().viewModelFactory
    )[Random(23).toString(), LibraryTagsInfoViewModel::class.java]

    override fun executeAction(row: InfoActions.InfoRow) {
        val action = row.actionType
        val field = row.fieldType
        when (action) {
            LibraryTagsInfoActions.LibraryActionType.SubFilter -> {
                val value = when (field) {
                    LibraryTagsInfoActions.LibraryFieldType.Playlist -> LibraryTagsInfoViewModel.PLAYLIST_ID
                    LibraryTagsInfoActions.LibraryFieldType.Album -> LibraryTagsInfoViewModel.ALBUM_ID
                    LibraryTagsInfoActions.LibraryFieldType.AlbumArtist -> LibraryTagsInfoViewModel.ALBUM_ARTIST_ID
                    LibraryTagsInfoActions.LibraryFieldType.Artist -> LibraryTagsInfoViewModel.ARTIST_ID
                    LibraryTagsInfoActions.LibraryFieldType.Genre -> LibraryTagsInfoViewModel.GENRE_ID
                    LibraryTagsInfoActions.LibraryFieldType.Song -> LibraryTagsInfoViewModel.SONG_ID
                    LibraryTagsInfoActions.LibraryFieldType.Downloaded -> LibraryTagsInfoViewModel.DOWNLOAD_ID
                    LibraryTagsInfoActions.LibraryFieldType.PodcastEpisode -> LibraryTagsInfoViewModel.PODCAST_EPISODE_ID
                    else -> LibraryTagsInfoViewModel.GENRE_ID
                }
                viewModel.navigator.displayFragmentOnMain(
                    requireContext(),
                    LibraryTagsListFragment(value, viewModel.filterNavigation),
                    LibraryTagsListFragment::class.java.simpleName
                )
            }

            else -> viewModel.executeAction(row)
        }
    }
}
