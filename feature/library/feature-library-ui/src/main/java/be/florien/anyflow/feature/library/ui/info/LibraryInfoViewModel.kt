package be.florien.anyflow.feature.library.ui.info

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.common.navigation.Navigator
import be.florien.anyflow.common.ui.domain.ImageConfig
import be.florien.anyflow.common.ui.domain.TextConfig
import be.florien.anyflow.feature.library.domain.LibraryInfoRepository
import be.florien.anyflow.feature.library.domain.model.IdText
import be.florien.anyflow.feature.library.domain.model.LibraryRowType
import be.florien.anyflow.feature.library.domain.model.LibraryFieldType
import be.florien.anyflow.feature.library.domain.model.LibraryInfoRow
import be.florien.anyflow.feature.library.podcast.domain.LibraryInfoPodcastRepository
import be.florien.anyflow.feature.library.tags.domain.LibraryInfoTagsRepository
import be.florien.anyflow.feature.library.ui.LibraryViewModel
import be.florien.anyflow.feature.library.ui.list.LibraryListFragment
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.management.filters.domain.model.Filter
import be.florien.anyflow.management.filters.domain.model.FilterType
import be.florien.anyflow.management.filters.domain.model.PodcastFilterType
import be.florien.anyflow.management.filters.domain.model.TagFilterType
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

class LibraryInfoViewModel @Inject constructor(
    private val libraryPodcastRepositoryProvider: Provider<LibraryInfoPodcastRepository>,
    private val libraryTagsRepositoryProvider: Provider<LibraryInfoTagsRepository>,
    override val filtersManager: FiltersManager,
    override val navigator: Navigator
) : ViewModel(), LibraryViewModel {
    private val mutableLibraryInfoRows: MutableStateFlow<List<LibraryInfoRow>> =
        MutableStateFlow(listOf())
    private val libraryInfoRows: StateFlow<List<LibraryInfoRow>> = mutableLibraryInfoRows

    private lateinit var libraryInfoRepository: LibraryInfoRepository

    override val areFiltersInEdition: LiveData<Boolean> = MutableLiveData(true)

    var filterNavigation: Filter? = null
        set(value) {
            field = value
            updateRows()
        }

    val state: Flow<PersistentList<InfoRowDisplay>> = libraryInfoRows.map { list ->
        list
            .map {
                it.toInfoRow()
            }
            .toPersistentList()
    }

    fun setType(type: String) {
        libraryInfoRepository = if (type == PODCAST_TYPE) {
            libraryPodcastRepositoryProvider.get()
        } else {
            libraryTagsRepositoryProvider.get()
        }
    }

    fun executeAction(context: Context, rowPosition: Int, backStackName: String) {
        val row = libraryInfoRows.value[rowPosition]
        val action = row.actionType
        when (action) {
            LibraryRowType.SubFilter -> {
                val value = when (row.fieldType) {
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

                navigator.displayFragmentOnMain(
                    context,
                    LibraryListFragment(value, filterNavigation),
                    backStackName,
                    LibraryListFragment::class.java.simpleName
                )
            }

            LibraryRowType.InfoTitle -> Unit
            LibraryRowType.ExpandableTitle -> {
                viewModelScope.launch(Dispatchers.IO) {
                    mutableLibraryInfoRows.value =
                        mutableLibraryInfoRows.value.toMutableList().apply {
                            val index = indexOf(row)
                            remove(row)
                            add(index, row.copy(actionType = LibraryRowType.ExpandedTitle))
                            addAll(index + 1, libraryInfoRepository.getActionList(row.fieldType))
                        }
                }
            }

            LibraryRowType.ExpandedTitle -> {
                mutableLibraryInfoRows.value = mutableLibraryInfoRows.value.toMutableList().apply {
                    val index = indexOf(row)
                    removeIf { it.fieldType == row.fieldType }
                    add(index, row.copy(actionType = LibraryRowType.ExpandableTitle))
                }
            }

            LibraryRowType.AddToFilter -> Unit//todo
            LibraryRowType.AddToPlaylist -> Unit//todo
            LibraryRowType.AddNext -> Unit//todo
            LibraryRowType.Search -> Unit//todo
            LibraryRowType.Download -> Unit//todo
        }
    }

    private fun updateRows() {
        viewModelScope.launch {
            mutableLibraryInfoRows.value = libraryInfoRepository.getInfoRowList(filterNavigation)
        }
    }

    private suspend fun LibraryInfoRow.toInfoRow(): InfoRowDisplay {
        val idText = if (actionType.isSingleDbRow) getIdText() else null
        val imageUrl = if (actionType.isUsingArt && idText != null) libraryInfoRepository.getArtUrl(fieldType.artType, idText.id) else null
        val leftImage = if (actionType.hasLeftIcon) ImageConfig(imageUrl, fieldType.iconRes) else null
        val title = actionType.titleRes ?: fieldType.titleRes
        return InfoRowDisplay(
            leftImage = leftImage,
            title = title,
            info = idText?.let { TextConfig(text = it.text, textRes = actionType.descriptionRes) } ?: infoText,
            actionIcon = actionType.iconRes,
            backgroundColor = null
        )
    }

    private suspend fun LibraryInfoRow.getIdText(): IdText {
        val filter = filterNavigation
        val filterType = getField(fieldType)
        val filterIfTypePresent = filter?.getFilterIfTypePresent(filterType)
        val filterData: IdText? = filterIfTypePresent?.takeIf { it.argument is Long }
            ?.let { IdText(it.argument as Long, it.displayText) }
        return filterData ?: libraryInfoRepository.getFilteredInfo(
            filterType,
            filter
        ) ?: IdText(0, "")
    }

    private fun getField(
        filterType: LibraryFieldType
    ): FilterType {
        return when (filterType) {
            LibraryFieldType.Podcast.Podcast -> PodcastFilterType.PODCAST_IS
            LibraryFieldType.Podcast.PodcastEpisode -> PodcastFilterType.PODCAST_EPISODE_IS
            LibraryFieldType.Tags.Song -> TagFilterType.SONG_IS
            LibraryFieldType.Tags.Artist -> TagFilterType.ARTIST_IS
            LibraryFieldType.Tags.AlbumArtist -> TagFilterType.ALBUM_ARTIST_IS
            LibraryFieldType.Tags.Album -> TagFilterType.ALBUM_IS
            LibraryFieldType.Tags.Playlist -> TagFilterType.PLAYLIST_IS
            LibraryFieldType.Tags.Downloaded -> TagFilterType.DOWNLOADED_STATUS_IS
            LibraryFieldType.Tags.Duration -> TagFilterType.SONG_IS
            LibraryFieldType.Tags.Genre -> TagFilterType.GENRE_IS
        }
    }

    companion object {

        const val PODCAST_TYPE = "podcast"
        const val TAGS_TYPE = "tags"

        const val PODCAST_ID = "Podcast"
        const val PODCAST_EPISODE_ID = "PodcastEpisode"
        const val GENRE_ID = "Genre"
        const val ALBUM_ARTIST_ID = "AlbumArtist"
        const val ARTIST_ID = "Artist"
        const val ALBUM_ID = "Album"
        const val SONG_ID = "Song"
        const val PLAYLIST_ID = "Playlist"
        const val DOWNLOAD_ID = "Download"
    }
}