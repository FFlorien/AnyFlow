package be.florien.anyflow.feature.library.ui.list

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.ViewModelProvider
import be.florien.anyflow.common.di.viewModelFactory
import be.florien.anyflow.common.resources.theming.AppTheme
import be.florien.anyflow.feature.library.ui.BaseFilteringFragment
import be.florien.anyflow.feature.library.ui.LibraryViewModel
import be.florien.anyflow.feature.library.ui.R
import be.florien.anyflow.feature.library.ui.info.LibraryInfoFragment
import be.florien.anyflow.feature.library.ui.info.LibraryInfoViewModel
import be.florien.anyflow.feature.library.ui.list.viewmodels.LibraryAlbumArtistListViewModel
import be.florien.anyflow.feature.library.ui.list.viewmodels.LibraryAlbumListViewModel
import be.florien.anyflow.feature.library.ui.list.viewmodels.LibraryArtistListViewModel
import be.florien.anyflow.feature.library.ui.list.viewmodels.LibraryDownloadedListViewModel
import be.florien.anyflow.feature.library.ui.list.viewmodels.LibraryGenreListViewModel
import be.florien.anyflow.feature.library.ui.list.viewmodels.LibraryPlaylistListViewModel
import be.florien.anyflow.feature.library.ui.list.viewmodels.LibraryPodcastEpisodeListViewModel
import be.florien.anyflow.feature.library.ui.list.viewmodels.LibraryPodcastListViewModel
import be.florien.anyflow.feature.library.ui.list.viewmodels.LibraryTagsListViewModel
import be.florien.anyflow.feature.library.ui.toItem
import be.florien.anyflow.management.filters.domain.model.Filter
import be.florien.anyflow.management.filters.domain.model.FilterParam
import be.florien.anyflow.management.filters.domain.model.TagFilterType

class LibraryListFragment @SuppressLint("ValidFragment") constructor(
    var filterType: String,
    private var parentFilterParam: Filter? = null
) : BaseFilteringFragment() {

    companion object {
        private const val FILTER_TYPE = "TYPE"
        private const val PARENT_FILTER = "PARENT_FILTER"
    }

    override val libraryViewModel: LibraryViewModel
        get() = viewModel
    override val navigator: be.florien.anyflow.common.navigation.Navigator
        get() = viewModel.navigator
    lateinit var viewModel: LibraryListViewModel

    override fun getTitle(): String = when (filterType) {
        LibraryInfoViewModel.ALBUM_ID ,
        LibraryInfoViewModel.ALBUM_ARTIST_ID ,
        LibraryInfoViewModel.ARTIST_ID ,
        LibraryInfoViewModel.GENRE_ID ,
        LibraryInfoViewModel.SONG_ID ,
        LibraryInfoViewModel.PLAYLIST_ID ,
        LibraryInfoViewModel.DOWNLOAD_ID -> getString(R.string.library_title_main)
        else -> getString(R.string.menu_podcast)
    }

    override fun getSubtitle(): String? = when (filterType) {
        LibraryInfoViewModel.ALBUM_ID -> getString(R.string.library_type_album)
        LibraryInfoViewModel.ALBUM_ARTIST_ID -> getString(R.string.library_type_album_artist)
        LibraryInfoViewModel.ARTIST_ID -> getString(R.string.library_type_artist)
        LibraryInfoViewModel.GENRE_ID -> getString(R.string.library_type_genre)
        LibraryInfoViewModel.SONG_ID -> getString(R.string.library_type_song)
        LibraryInfoViewModel.PLAYLIST_ID -> getString(R.string.library_type_playlist)
        LibraryInfoViewModel.DOWNLOAD_ID -> getString(R.string.library_type_download)
        LibraryInfoViewModel.PODCAST_ID -> getString(R.string.library_type_podcast)
        LibraryInfoViewModel.PODCAST_EPISODE_ID -> getString(R.string.library_type_podcast_episode)
        else -> null
    }

    init {
        arguments?.let {
            filterType = it.getString(FILTER_TYPE, "Error")
            parentFilterParam =
                Filter(*(it.getParcelableArray(PARENT_FILTER) as Array<FilterParam<*>>))
        }
        if (arguments == null) {
            arguments = Bundle().apply {
                putString(FILTER_TYPE, filterType)
                putParcelableArray(PARENT_FILTER, parentFilterParam?.toTypedArray())
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel = ViewModelProvider(this, requireActivity().viewModelFactory)[
            when (filterType) {
                LibraryInfoViewModel.PLAYLIST_ID -> LibraryPlaylistListViewModel::class.java
                LibraryInfoViewModel.ALBUM_ID -> LibraryAlbumListViewModel::class.java
                LibraryInfoViewModel.ALBUM_ARTIST_ID -> LibraryAlbumArtistListViewModel::class.java
                LibraryInfoViewModel.ARTIST_ID -> LibraryArtistListViewModel::class.java
                LibraryInfoViewModel.GENRE_ID -> LibraryGenreListViewModel::class.java
                LibraryInfoViewModel.SONG_ID -> LibraryTagsListViewModel::class.java
                LibraryInfoViewModel.DOWNLOAD_ID -> LibraryDownloadedListViewModel::class.java
                LibraryInfoViewModel.PODCAST_ID -> LibraryPodcastListViewModel::class.java
                LibraryInfoViewModel.PODCAST_EPISODE_ID -> LibraryPodcastEpisodeListViewModel::class.java
                else -> LibraryGenreListViewModel::class.java
            }
        ]
        viewModel.navigationFilter = parentFilterParam
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme {
                    LibraryListScreen(
                        itemsPager = viewModel.values,
                        onClick = viewModel::toggleFilterSelection,
                        onNavigation = ::onInfoDisplayAsked
                    )
                }
            }
        }
    }

    fun onInfoDisplayAsked(item: FilterDisplay) {
        val filter = viewModel.getFilter(item.toItem())
        val type = if (filter.mainParam.type is TagFilterType) {
            LibraryInfoViewModel.TAGS_TYPE
        } else {
            LibraryInfoViewModel.PODCAST_TYPE
        }
        navigator.displayFragmentOnMain(
            requireContext(),
            LibraryInfoFragment(type, filter),
            type,
            LibraryInfoFragment::class.java.simpleName
        )
    }
}