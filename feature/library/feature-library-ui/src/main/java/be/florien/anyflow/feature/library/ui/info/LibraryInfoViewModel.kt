package be.florien.anyflow.feature.library.ui.info

import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.common.navigation.Navigator
import be.florien.anyflow.common.ui.domain.ImageConfig
import be.florien.anyflow.common.ui.domain.TagType
import be.florien.anyflow.common.ui.domain.TextConfig
import be.florien.anyflow.common.utils.TimeOperations
import be.florien.anyflow.feature.library.domain.LibraryInfoRepository
import be.florien.anyflow.feature.library.domain.model.IdText
import be.florien.anyflow.feature.library.domain.model.LibraryFieldType
import be.florien.anyflow.feature.library.domain.model.LibraryInfoRow
import be.florien.anyflow.feature.library.domain.model.LibraryRowType
import be.florien.anyflow.feature.library.domain.model.getKey
import be.florien.anyflow.feature.library.podcast.domain.LibraryInfoPodcastRepository
import be.florien.anyflow.feature.library.tags.domain.LibraryInfoTagsRepository
import be.florien.anyflow.feature.library.ui.LibraryViewModel
import be.florien.anyflow.feature.library.ui.list.LibraryListFragment
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.management.filters.domain.model.Filter
import be.florien.anyflow.management.filters.domain.model.FilterParam
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
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Provider
import kotlin.time.DurationUnit
import kotlin.time.toDuration

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

    fun executeAction(fragment: Fragment, rowPosition: Int, backStackName: String) {
        val row = libraryInfoRows.value[rowPosition]
        val action = row.rowType
        when (action) {
            LibraryRowType.MultiRow.SubFilter -> {
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
                    fragment.requireActivity(),
                    LibraryListFragment(value, filterNavigation),
                    backStackName,
                    LibraryListFragment::class.java.simpleName
                )
            }

            LibraryRowType.MultiRow.InfoTitle -> Unit
            LibraryRowType.SingleRow.ExpandableTitle -> {
                viewModelScope.launch(Dispatchers.IO) {
                    mutableLibraryInfoRows.value =
                        mutableLibraryInfoRows.value.toMutableList().apply {
                            val index = indexOf(row)
                            remove(row)
                            add(index, row.copy(rowType = LibraryRowType.SingleRow.ExpandedTitle))
                            addAll(index + 1, libraryInfoRepository.getActionList(row.fieldType))
                        }
                }
            }

            LibraryRowType.SingleRow.ExpandedTitle -> {
                mutableLibraryInfoRows.value = mutableLibraryInfoRows.value.toMutableList().apply {
                    val index = indexOf(row)
                    removeAll { it.fieldType == row.fieldType }
                    add(index, row.copy(rowType = LibraryRowType.SingleRow.ExpandableTitle))
                }
            }

            LibraryRowType.Action.SeeInLibrary -> {
                viewModelScope.launch {
                    val parentFilter = getFilterForRow(row)
                    withContext(Dispatchers.Main) {
                        val type = when (row.fieldType) {
                            is LibraryFieldType.Tags -> TAGS_TYPE
                            is LibraryFieldType.Podcast -> PODCAST_TYPE
                        }
                        navigator.displayFragmentOnMain(
                            fragment.requireContext(),
                            LibraryInfoFragment(
                                type = type,
                                parentFilter
                            ),
                            type,
                            LibraryInfoFragment::class.java.simpleName
                        )
                    }
                }
            }
            LibraryRowType.Action.AddToFilter -> viewModelScope.launch { filterOn(row) }
            LibraryRowType.Action.AddToPlaylist -> {
                viewModelScope.launch {
                    val type = row.fieldType.toTagType() ?: return@launch
                    val idText = row.getIdText()
                    navigator.displayPlaylistSelection(fragment.childFragmentManager, idText.id, type, -1)
                }
            }
            LibraryRowType.Action.AddNext -> Unit//todo
            LibraryRowType.Action.Search -> Unit//todo
            LibraryRowType.Action.Download -> Unit//todo
        }
    }

    private fun LibraryFieldType.toTagType() =
        when (this) {//todo copy paste from feature-songlist-ui, make it common
            LibraryFieldType.Podcast.Podcast -> TODO()
            LibraryFieldType.Podcast.PodcastEpisode -> TODO()
            LibraryFieldType.Tags.Duration -> TODO()
            LibraryFieldType.Tags.Genre -> TagType.Genre
            LibraryFieldType.Tags.AlbumArtist -> TagType.AlbumArtist
            LibraryFieldType.Tags.Album -> TagType.Album
            LibraryFieldType.Tags.Artist -> TagType.Artist
            LibraryFieldType.Tags.Song -> TagType.Title
            LibraryFieldType.Tags.Playlist -> TagType.Playlist
            else -> null
        }

    private fun updateRows() {
        viewModelScope.launch {
            mutableLibraryInfoRows.value = libraryInfoRepository.getInfoRowList(filterNavigation)
        }
    }

    private suspend fun LibraryInfoRow.toInfoRow(): InfoRowDisplay {
        val idText = if (rowType !is LibraryRowType.MultiRow) {
            getIdText()
        } else {
            IdText(0, text = count.toString())
        }
        val imageUrl = if (rowType is LibraryRowType.SingleRow) {
            libraryInfoRepository.getArtUrl(fieldType.artType, idText.id)
        } else {
            null
        }
        val leftImage = if (rowType !is LibraryRowType.Action) {
            ImageConfig(imageUrl, fieldType.iconRes)
        } else {
            null
        }
        val title = rowType.titleRes ?: fieldType.titleRes
        val info =
            if (rowType is LibraryRowType.MultiRow && fieldType == LibraryFieldType.Tags.Duration) {
                TextConfig(
                    mediaDuration = TimeOperations.toMediaDuration(
                        count.toDuration(DurationUnit.SECONDS)
                    )
                )
            } else {
                TextConfig(text = idText.text, textRes = rowType.descriptionRes)
            }

        return InfoRowDisplay(
            key = "$fieldType${rowType.getKey()}",
            leftImage = leftImage,
            title = title,
            info = info,
            actionIcon = rowType.iconRes,
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

    suspend fun filterOn(row: LibraryInfoRow) {
        val filter = getFilterForRow(row)
        filtersManager.clearFilters()
        filtersManager.addFilter(filter)
        filtersManager.commitChanges()
    }

    private suspend fun getFilterForRow(row: LibraryInfoRow): Filter {
        val idText = row.getIdText()
        val filter = Filter(
            FilterParam(
                getField(row.fieldType),
                idText.id,
                idText.text
            )
        )
        return filter
    }

    suspend fun getSearchTerms(row: LibraryInfoRow) = row.getIdText().text
    fun getRowAtPosition(position: Int): LibraryInfoRow {
        return libraryInfoRows.value[position]
    }

//    fun queueDownload(songInfo: SongInfo, fieldType: SongFieldType, index: Int?) {
//        val data = when (fieldType) {
//            SongFieldType.Title -> Triple(
//                songInfo.id,
//                TagFilterType.SONG_IS,
//                -1
//            )
//
//            SongFieldType.Artist -> Triple(
//                songInfo.artistId,
//                TagFilterType.ARTIST_IS,
//                -1
//            )
//
//            SongFieldType.Album -> Triple(
//                songInfo.albumId,
//                TagFilterType.ALBUM_IS,
//                -1
//            )
//
//            SongFieldType.Disk -> Triple(
//                songInfo.albumId,
//                TagFilterType.DISK_IS,
//                songInfo.disk
//            )
//
//            SongFieldType.AlbumArtist -> Triple(
//                songInfo.albumArtistId,
//                TagFilterType.ALBUM_ARTIST_IS,
//                -1
//            )
//
//            SongFieldType.Genre -> {
//                val trueIndex = index ?: return
//                Triple(
//                    songInfo.genreIds[trueIndex],
//                    TagFilterType.GENRE_IS,
//                    -1
//                )
//            }
//
//            SongFieldType.Playlist -> {
//                val trueIndex = index ?: return
//                Triple(
//                    songInfo.playlistIds[trueIndex],
//                    TagFilterType.PLAYLIST_IS,
//                    -1
//                )
//            }
//
//            else -> return
//        }
//        downloadManager.queueDownload(data.first, data.second, data.third)
//    }

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