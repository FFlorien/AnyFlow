package be.florien.anyflow.feature.library.tags.ui.list

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModelProvider
import be.florien.anyflow.common.di.ActivityScope
import be.florien.anyflow.common.di.ServerScope
import be.florien.anyflow.common.di.viewModelFactory
import be.florien.anyflow.common.ui.list.DetailViewHolderListener
import be.florien.anyflow.feature.library.domain.model.FilterItem
import be.florien.anyflow.feature.library.tags.ui.info.LibraryTagsInfoFragment
import be.florien.anyflow.feature.library.tags.ui.info.LibraryTagsInfoViewModel
import be.florien.anyflow.feature.library.tags.ui.list.viewmodels.LibraryAlbumArtistListViewModel
import be.florien.anyflow.feature.library.tags.ui.list.viewmodels.LibraryAlbumListViewModel
import be.florien.anyflow.feature.library.tags.ui.list.viewmodels.LibraryArtistListViewModel
import be.florien.anyflow.feature.library.tags.ui.list.viewmodels.LibraryDownloadedListViewModel
import be.florien.anyflow.feature.library.tags.ui.list.viewmodels.LibraryGenreListViewModel
import be.florien.anyflow.feature.library.tags.ui.list.viewmodels.LibraryPlaylistListViewModel
import be.florien.anyflow.feature.library.tags.ui.list.viewmodels.LibrarySongListViewModel
import be.florien.anyflow.feature.library.ui.R
import be.florien.anyflow.feature.library.ui.list.LibraryListFragment
import be.florien.anyflow.management.filters.model.Filter

@ActivityScope
@ServerScope
class LibraryTagsListFragment @SuppressLint("ValidFragment") //todo abstract this, for onattach, title, subtitle
constructor(
    filterType: String = LibraryTagsInfoViewModel.GENRE_ID,
    parentFilter: Filter<*>? = null
) : LibraryListFragment(filterType, parentFilter),
    DetailViewHolderListener<FilterItem> {

    override fun getViewModel(filterName: String) =
        ViewModelProvider(this, requireActivity().viewModelFactory)[
            when (filterName) {
                LibraryTagsInfoViewModel.ALBUM_ID -> LibraryAlbumListViewModel::class.java
                LibraryTagsInfoViewModel.ALBUM_ARTIST_ID -> LibraryAlbumArtistListViewModel::class.java
                LibraryTagsInfoViewModel.ARTIST_ID -> LibraryArtistListViewModel::class.java
                LibraryTagsInfoViewModel.GENRE_ID -> LibraryGenreListViewModel::class.java
                LibraryTagsInfoViewModel.SONG_ID -> LibrarySongListViewModel::class.java
                LibraryTagsInfoViewModel.DOWNLOAD_ID -> LibraryDownloadedListViewModel::class.java
                else -> LibraryPlaylistListViewModel::class.java
            }]

    override fun getTitle(): String = getString(R.string.library_title_main)

    override fun getSubtitle(): String? = when (filterType) {
        LibraryTagsInfoViewModel.ALBUM_ID -> getString(R.string.library_type_album)
        LibraryTagsInfoViewModel.ALBUM_ARTIST_ID -> getString(R.string.library_type_album_artist)
        LibraryTagsInfoViewModel.ARTIST_ID -> getString(R.string.library_type_artist)
        LibraryTagsInfoViewModel.GENRE_ID -> getString(R.string.library_type_genre)
        LibraryTagsInfoViewModel.SONG_ID -> getString(R.string.library_type_song)
        LibraryTagsInfoViewModel.PLAYLIST_ID -> getString(R.string.library_type_playlist)
        LibraryTagsInfoViewModel.DOWNLOAD_ID -> getString(R.string.library_type_download)
        else -> null
    }

    override fun onInfoDisplayAsked(item: FilterItem) {
        val filter = viewModel.getFilter(item)
        navigator.displayFragmentOnMain(
            requireContext(),
            LibraryTagsInfoFragment(filter),
            "PODCAST",
            LibraryTagsInfoFragment::class.java.simpleName
        )
    }
}
