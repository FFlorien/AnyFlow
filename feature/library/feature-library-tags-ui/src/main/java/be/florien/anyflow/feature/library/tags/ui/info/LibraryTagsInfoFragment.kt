package be.florien.anyflow.feature.library.tags.ui.info

import androidx.lifecycle.ViewModelProvider
import be.florien.anyflow.architecture.di.viewModelFactory
import be.florien.anyflow.common.ui.data.info.InfoActions
import be.florien.anyflow.feature.library.tags.domain.LibraryTagsInfoActions
import be.florien.anyflow.feature.library.tags.ui.list.LibraryTagsListFragment
import be.florien.anyflow.feature.library.ui.R
import be.florien.anyflow.feature.library.ui.info.LibraryInfoFragment
import be.florien.anyflow.management.filters.model.Filter
import kotlin.random.Random

class LibraryTagsInfoFragment(parentFilter: Filter<*>? = null) : LibraryInfoFragment<LibraryTagsInfoActions>(parentFilter) {
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
            LibraryTagsInfoActions.LibraryPodcastActionType.SubFilter -> {
                val value = when (field) {
                    LibraryTagsInfoActions.LibraryTagsFieldType.Playlist -> LibraryTagsInfoViewModel.PLAYLIST_ID
                    LibraryTagsInfoActions.LibraryTagsFieldType.Album -> LibraryTagsInfoViewModel.ALBUM_ID
                    LibraryTagsInfoActions.LibraryTagsFieldType.AlbumArtist -> LibraryTagsInfoViewModel.ALBUM_ARTIST_ID
                    LibraryTagsInfoActions.LibraryTagsFieldType.Artist -> LibraryTagsInfoViewModel.ARTIST_ID
                    LibraryTagsInfoActions.LibraryTagsFieldType.Genre -> LibraryTagsInfoViewModel.GENRE_ID
                    LibraryTagsInfoActions.LibraryTagsFieldType.Song -> LibraryTagsInfoViewModel.SONG_ID
                    LibraryTagsInfoActions.LibraryTagsFieldType.Downloaded -> LibraryTagsInfoViewModel.DOWNLOAD_ID
                    else -> LibraryTagsInfoViewModel.GENRE_ID
                }
                viewModel.navigator.displayFragmentOnMain(
                    requireContext(),
                    LibraryTagsListFragment(value, viewModel.filterNavigation),
                    "TAGS",
                    LibraryTagsListFragment::class.java.simpleName
                )
            }

            else -> viewModel.executeAction(row)
        }
    }
}
