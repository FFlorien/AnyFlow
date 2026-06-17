package be.florien.anyflow.feature.filter.current.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import be.florien.anyflow.common.di.viewModelFactory
import be.florien.anyflow.common.navigation.Navigator
import be.florien.anyflow.common.resources.theming.AppTheme
import be.florien.anyflow.feature.library.ui.BaseFilteringFragment
import be.florien.anyflow.feature.library.ui.LibraryViewModel
import be.florien.anyflow.management.filters.domain.model.FilterParam
import be.florien.anyflow.management.filters.domain.model.PodcastFilterType
import be.florien.anyflow.management.filters.domain.model.TagFilterType
import kotlinx.collections.immutable.toPersistentList

class CurrentFilterFragment : BaseFilteringFragment() {
    override fun getTitle(): String = getString(R.string.menu_filters)

    override val libraryViewModel: LibraryViewModel
        get() = viewModel
    override val navigator: Navigator
        get() = viewModel.navigator
    lateinit var viewModel: CurrentFilterViewModel
    //todo save filter group menu
    //todo currentFilterVM implement ViewModel directly ???????

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel = ViewModelProvider(
            this,
            requireActivity().viewModelFactory
        )[CurrentFilterViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireActivity()).apply {
            setContent {
                val state = viewModel.stateFlow.collectAsStateWithLifecycle().value
                val filters = state.map {
                    val mainParam = it.filter.mainParam
                    val artType = mainParam.type.artType
                    val argument = mainParam.argument
                    Filter(
                        id = it.id,
                        imageUrl = if (artType != null && argument is Long) {
                            viewModel.getUrlForImage(artType, argument)
                        } else {
                            null
                        },
                        displayText = it.filter.joinToString(separator = "<br>") { getFilterText(it) },
                        fallbackRes = getDefaultDrawable(it.filter.mainParam)
                    )
                }.toPersistentList()
                AppTheme(authenticationInterceptor = viewModel.authenticationInterceptor) {
                    CurrentFiltersScreen(
                        filters,
                        { filterId -> viewModel.deleteFilter(state.first { it.id == filterId }.filter) },
                        viewModel::clearFilters,
                        { navigator.navigateToLibrary(requireActivity()) },
                        { navigator.navigateToPodcast(requireActivity()) }
                    )
                }
            }
        }
    }


    private fun getFilterText(filterParam: FilterParam<*>) =
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
}