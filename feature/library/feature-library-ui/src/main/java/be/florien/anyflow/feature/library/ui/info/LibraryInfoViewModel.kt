package be.florien.anyflow.feature.library.ui.info

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.common.navigation.Navigator
import be.florien.anyflow.common.ui.domain.ImageConfig
import be.florien.anyflow.common.ui.domain.TagType
import be.florien.anyflow.common.ui.domain.TextConfig
import be.florien.anyflow.common.utils.TimeOperations
import be.florien.anyflow.feature.auth.domain.net.AuthenticationInterceptor
import be.florien.anyflow.feature.library.domain.LibraryInfoRepository
import be.florien.anyflow.feature.library.domain.model.IdText
import be.florien.anyflow.feature.library.domain.model.LibraryFieldType
import be.florien.anyflow.feature.library.domain.model.LibraryInfoRow
import be.florien.anyflow.feature.library.domain.model.LibraryRowType
import be.florien.anyflow.feature.library.domain.model.getKey
import be.florien.anyflow.feature.library.podcast.domain.LibraryInfoPodcastRepository
import be.florien.anyflow.feature.library.tags.domain.LibraryInfoTagsRepository
import be.florien.anyflow.feature.library.ui.LibraryViewModel
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
import javax.inject.Inject
import javax.inject.Provider
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class LibraryInfoViewModel @Inject constructor(
    private val libraryPodcastRepositoryProvider: Provider<LibraryInfoPodcastRepository>,
    private val libraryTagsRepositoryProvider: Provider<LibraryInfoTagsRepository>,
    override val filtersManager: FiltersManager,
    override val navigator: Navigator,
    val authenticationInterceptor: AuthenticationInterceptor
) : ViewModel(), LibraryViewModel {
    private val mutableLibraryInfoRows: MutableStateFlow<List<LibraryInfoRow>> =
        MutableStateFlow(listOf())
    val libraryInfoRows: StateFlow<List<LibraryInfoRow>> = mutableLibraryInfoRows

    private lateinit var libraryInfoRepository: LibraryInfoRepository

    override val areFiltersInEdition: LiveData<Boolean> = MutableLiveData(true)

    var filterNavigation: Filter? = null
        set(value) {
            if (value == field && value != null) {
                return
            }
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

    fun executeAction(rowPosition: Int, activity: FragmentActivity) {
        val row = libraryInfoRows.value[rowPosition]
        val action = row.rowType
        when (action) {
            LibraryRowType.MultiRow.SubFilter -> Unit

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

            LibraryRowType.Action.SeeInLibrary -> Unit
            LibraryRowType.Action.AddToFilter -> viewModelScope.launch { filterOn(row) }
            LibraryRowType.Action.AddToPlaylist -> {
                viewModelScope.launch {
                    val type = row.fieldType.toTagType() ?: return@launch
                    val idText = row.getIdText()
                    navigator.displayPlaylistSelection(
                        activity.supportFragmentManager,
                        idText.id,
                        type,
                        -1
                    )
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
        val key = "$fieldType${rowType.getKey()}"
        val title = rowType.titleRes ?: fieldType.titleRes
        return when (rowType) {
            is LibraryRowType.Action -> InfoRowDisplay.Action(
                key = key,
                title = title,
                actionDescription = TextConfig(
                    text = getIdText().text,
                    textRes = rowType.descriptionRes
                ),
                rowType = rowType,
                fieldType = fieldType
            )

            is LibraryRowType.MultiRow -> InfoRowDisplay.List(
                key = key,
                title = title,
                leftImage = fieldType.iconRes,
                countText = if (fieldType == LibraryFieldType.Tags.Duration) {
                    TextConfig(
                        mediaDuration = TimeOperations.toMediaDuration(
                            count.toDuration(DurationUnit.SECONDS)
                        )
                    )
                } else {
                    TextConfig(text = count.toString())
                }
            )

            is LibraryRowType.SingleRow -> {
                val idText = getIdText()
                InfoRowDisplay.Item(
                    key = key,
                    title = title,
                    id = idText.id,
                    leftImage = ImageConfig(
                        url = libraryInfoRepository.getArtUrl(fieldType.artType, idText.id),
                        resource = fieldType.iconRes
                    ),
                    info = TextConfig(text = idText.text, textRes = rowType.descriptionRes),
                    rowType = rowType,
                    fieldType = fieldType
                )
            }
        }
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